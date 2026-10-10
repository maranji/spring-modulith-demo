# Action Router POC — Market Data & Analytics

POC of a **Gradle multi-module** modular monolith (Java 21, Spring Boot 3.4.1) in which the **only** communication channel between modules is a hand-written **action router**. The goal is to push this mechanism as far as possible, to fully understand its limits.

Principles:

1. every domain module is packaged as a **Spring Boot starter** with auto-configuration;
2. each module's **API** (actions, responses, infos) is separate from its implementation;
3. **`app` imports no service interface from the domain modules**: it sends actions, which are pure data, to the router. A module can therefore be removed as a dependency and replaced by an adapter to an external service, without touching `app`;
4. one-to-many notifications go through the router too: no `ApplicationEventPublisher` or any other Spring mechanism to make modules talk to each other.

The document [`docs/modular-monolith-architecture-spring-modulith.md`](docs/modular-monolith-architecture-spring-modulith.md) is the general architectural reference the POC started from (it used Spring Modulith, later removed).

## 1. Structure

```
action-router-demo/
├── action-core/                        ← Action<R> and Info markers (zero dependencies)
├── action-router/                      ← transport API
├── action-router-spring-boot-starter/  ← in-process action router
├── market-data-action/                 ← Market Data API: actions, infos, data
├── market-data-spring-boot-starter/    ← in-process implementation (JSON files on disk)
├── analytics-action/                   ← Analytics API: actions + data
├── analytics-spring-boot-starter/      ← in-process implementation (calculation + cache)
├── analytics-restclient-starter/       ← ALTERNATIVE implementation: HTTP client to an external service
├── app/                                ← always-on Spring Boot application (REST)
└── docs/                               ← architectural reference document
```

| Module | Contains | Depends on |
|---|---|---|
| **action-core** | `Action<R>`, `Info` | **nothing** |
| **action-router** | `ActionRouter`, `ActionHandler<C,R>`, `InfoHandler<I>` | `action-core` |
| **action-router-spring-boot-starter** | `SimpleActionRouter`: collects every handler, checks at startup that none is missing or duplicated, and dispatches by type | `action-router` |
| **market-data-action** | `AssetPrices`, `AvailableAssets`, `RefreshMarketData`, `MarketDataRefreshed` (info), `PricePoint`, `AssetNotFoundException` | `action-core` |
| **market-data-spring-boot-starter** | `MarketDataStore` + 3 `ActionHandler`s (one per action) | `action-router`, `market-data-action` |
| **analytics-action** | `AveragePrice`, `StandardDeviationQuery`, `PriceStatistics`, `StatisticType`, `InsufficientDataException` | `action-core` (not even `market-data-action`) |
| **analytics-spring-boot-starter** | `AnalyticsCalculationService`, `AnalyticsCache` + 2 `ActionHandler`s + 1 `InfoHandler` | `action-router`, `analytics-action`, `market-data-action` (only for the `AssetPrices`/`PricePoint`/`MarketDataRefreshed` types) |
| **analytics-restclient-starter** | `RemoteAnalyticsClient` (calls an external Analytics service over HTTP) + 2 alternative `ActionHandler`s | `action-router`, `analytics-action`, `market-data-action` (only for the exception) |
| **app** | REST controllers, error handling | `action-router`, `market-data-action`, `analytics-action`, `action-router-spring-boot-starter`, `market-data-spring-boot-starter` **+** one of the two Analytics implementations |

The starters declare `action-router` directly instead of receiving it transitively from their action module: domain actions depend only on `action-core`, never on the router.

## 2. The action router

### Two semantics, two types

```java
// action-core
public interface Action<R> {}   // request/response: exactly 1 handler
public interface Info {}          // notification: 0 to N handlers, no response

// action-router
public interface ActionRouter {
    <R> R route(Action<R> action);
    void broadcast(Info info);
}
public interface ActionHandler<C extends Action<R>, R> { R handle(C action); }
public interface InfoHandler<I extends Info> { void handle(I info); }
```

| | `route(Action<R>)` | `broadcast(Info)` |
|---|---|---|
| Registered handlers | exactly one (a duplicate makes startup fail) | zero, one or many |
| No handler | startup fails if the action is under `action-router.base-packages`; otherwise `IllegalStateException` on `route` | no effect |
| Response | `R`, declared by the action | none |
| Handler exception | propagates to the caller | only logged: it doesn't reach the caller and the following handlers are still called |

Every action declares its own response type (`Action<Void>` for commands, such as `RefreshMarketData`); a handler whose response type doesn't match the action's is a compile error. Commands and infos are deliberately kept distinct: a command must have its single handler, and the router must be able to report when it's missing; an info may interest nobody. The name `Info` (rather than "event") is deliberate: the POC doesn't want to suggest an event-driven architecture, just a one-to-many notification on the same router.

### How `SimpleActionRouter` works

At startup `SimpleActionRouter` receives **all** the `ActionHandler` and `InfoHandler` beans in the Spring context, whichever starter registered them, and indexes them by the type they declare to handle, read via reflection from the generic:

```java
ResolvableType.forClass(handler.getClass()).as(ActionHandler.class).getGeneric(0).resolve()
```

This is the only "magic" spot in the project: a handler must implement `ActionHandler<SomeAction, SomeResponse>` or `InfoHandler<SomeInfo>` **directly**; an AOP proxy that hides the generic information would break dispatch.

- `route(action)` looks up the handler for `action.getClass()` and invokes it; if there is none, the error names the action that no module handles.
- `broadcast(info)` invokes every handler registered for `info.getClass()`. If a handler throws, the router logs the exception and moves on to the next one. In `SimpleActionRouter` the handlers run sequentially in the caller's thread: this is a choice of this implementation, not of the `ActionRouter` interface, and another implementation could run them asynchronously without changing the callers.

At startup (`afterSingletonsInstantiated`) the router also checks that the index is complete and unambiguous; otherwise the application doesn't start:

- if two handlers declare the same action, the error lists every contested action with its handlers (typically two alternative implementation modules on the classpath);
- if a concrete `Action` found on the classpath under `action-router.base-packages` has no handler, the error lists the unhandled actions (typically a forgotten implementation starter). Actions listed in `action-router.ignored-actions` are excluded.

The router routes by **type**, not by service: there is no "AnalyticsService" or "MarketDataService" interface anywhere.

### `app` sends only data

```java
// PriceStatisticsController — no service interface imported
private final ActionRouter actionRouter;

@GetMapping("/average")
PriceStatistics average(@PathVariable String symbol,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    validateRange(from, to);   // 400 if from > to
    return actionRouter.route(new AverageQuery(symbol, from, to));
}
```

`AveragePrice` and `PriceStatistics` are the only information the caller and the implementation need to share.

### Only `build.gradle` decides who handles each action

```gradle
// app/build.gradle
if (analyticsMode == 'remote') {
    implementation project(':analytics-restclient-starter')   // handlers call an external service over HTTP
} else {
    implementation project(':analytics-spring-boot-starter')  // handlers compute in-process
}
```

```bash
./gradlew :app:bootRun                          # Analytics in-process (default)
./gradlew :app:bootRun -PanalyticsMode=remote   # Analytics as an external service
./gradlew :app:printAnalyticsMode               # show which implementation is wired in
```

**Zero lines of `org.maranji.app` change** when switching from local to remote: the only thing that changes is which module on the classpath registered the handler for that action. The two implementations are mutually exclusive: having both on the classpath would register two handlers for the same action, and startup would fail with an error listing them.

`analytics-restclient-starter` calls the same REST contract exposed by `PriceStatisticsController` (`GET /api/assets/{symbol}/statistics/average|standard-deviation`) at `analytics.remote.base-url` and translates HTTP 404 and 422 errors into the same domain exceptions (`AssetNotFoundException`, `InsufficientDataException`). In remote mode nobody handles `MarketDataRefreshed`, and for an info that's fine.

### Communication between domain modules

Analytics gets prices from Market Data with a request on the router:

```java
List<PricePoint> pricePoints = actionRouter.route(new FindPricesQuery(asset, from, to));
```

The data refresh combines both semantics. The refresh **request** is a command, so it goes through `route` and is handled by one and only one module; the **notification** that the data changed is an info, broadcast to whoever is interested:

```
POST /api/assets/refresh
  └─ AssetsController ──route(RefreshMarketDataCommand)──▶ RefreshMarketDataCommandHandler   (exactly 1 handler: market-data)
                                                             └─ MarketDataStore.refresh()
                                                                  └─ broadcast(MarketDataRefreshed) ──▶ 0..N InfoHandlers
                                                                                                        (today: MarketDataRefreshedHandler)
```

The scheduled refresh of `MarketDataStore` broadcasts `MarketDataRefreshed` too. `MarketDataRefreshedHandler` (in `analytics-spring-boot-starter`) clears the statistics cache; Market Data doesn't know who, if anyone, receives the info.

## 3. Known limits

This section collects the action router's limits as they come up. It is the real object of study of the POC.

1. **Broadcasting an info during bean initialization creates a cycle.** The first call to the router builds the handler index, so it instantiates all of them. If the initial load of `MarketDataStore` (`@PostConstruct`) broadcast `MarketDataRefreshed`, `FindPricesQueryHandler` would be instantiated too, and it depends on `MarketDataStore` itself, still being created: startup fails with `BeanCurrentlyInCreationException` (verified). That's why the initial load broadcasts nothing: at startup there is no cache to invalidate.
2. **Whoever calls `broadcast` doesn't know whether an info was handled.** Handler errors are only logged: if the Analytics cache invalidation failed, Market Data wouldn't notice and statistics would stay computed on stale data until the next successful broadcast.
3. **No delivery guarantee.** Infos live only in memory: if the process ends during the broadcast, or a handler fails, the notification is lost. There is no persistence or retry.
4. **No transaction integration.** `broadcast` invokes the handlers immediately: if it were called inside a transaction that is later rolled back, the handlers would already have reacted to something that never happened.
5. **Dispatch by exact class.** A handler registered for a supertype (or an interface) doesn't receive subtypes, for both actions and infos.
6. **Order of an info's handlers.** It's the bean order (`ObjectProvider.orderedStream()`): defined only if the handlers use `@Order`/`Ordered`.
7. **Handlers can't be proxied.** The handled type is read from the generic of the concrete class: a proxy that hides it breaks dispatch.
8. **No automatic verification of module boundaries.** Without Spring Modulith (`ApplicationModules.verify()`) the only protection is Java visibility: implementation classes are package-private (except the `*AutoConfiguration` classes, which are `public`), under `internal/` in the in-process starters and under `remote/` in `analytics-restclient-starter`.
9. **A missing handler surfaces at startup, not at compile time.** `app` compiles even if the starter implementing an action it uses is missing. To avoid discovering it on the first `route`, at startup the router looks up every concrete `Action` on the classpath under `action-router.base-packages` and, if one has no handler, the application doesn't start. An action module on the classpath therefore requires its implementation, unless the action is listed in `action-router.ignored-actions`. The check doesn't see actions outside those packages, and it needs a test that starts the context for the error to surface during the `build`.

## 4. REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/assets` | List of available assets |
| `POST` | `/api/assets/refresh` | Manually reload the data from disk (202 Accepted) |
| `GET` | `/api/assets/{symbol}/statistics/average?from=YYYY-MM-DD&to=YYYY-MM-DD` | Average price over the period |
| `GET` | `/api/assets/{symbol}/statistics/standard-deviation?from=YYYY-MM-DD&to=YYYY-MM-DD` | Sample standard deviation of prices over the period |

Errors as [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807):
`404` unknown asset, `422` no data in the period, `400` `from` after `to`.

Sample data: `AAPL` and `MSFT`, 11 fictitious quotes each (January 2–16, 2026), in `app/src/main/resources/data/*.json`.

## 5. Configuration

The main properties, in `app/src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `market-data.directory` | `classpath:data/` | Directory of the JSON files (Spring `Resource` syntax, e.g. `file:/absolute/path/`) |
| `market-data.refresh-interval` | `PT5M` | Interval of the automatic data reload |
| `market-data.refresh-enabled` | `true` | Enables the automatic reload (the manual one via `POST /api/assets/refresh` is always available) |
| `action-router.base-packages` | `org.maranji` | Packages where the router looks, at startup, for the `Action`s that must have a handler |
| `action-router.ignored-actions` | (empty) | Actions on the classpath that are allowed to have no handler |
| `analytics.remote.base-url` | `http://localhost:8081` in `application.yml` (`http://localhost:8080` if the property is missing) | URL of the external Analytics service; used only with `-PanalyticsMode=remote` |

## 6. How to run the project

Requires JDK 21; the Gradle Wrapper (Gradle 8.5) is included.

```bash
# Full build + tests (Analytics in-process, default)
./gradlew build

# Tests of a single module or a single class
./gradlew :analytics-spring-boot-starter:test
./gradlew test --tests '*SimpleActionRouterTest'

# Run
./gradlew :app:bootRun
# or, for the remote Analytics implementation (needs a service listening
# on analytics.remote.base-url that exposes the same REST contract):
./gradlew :app:bootRun -PanalyticsMode=remote
```

Quick check (default, local mode):

```bash
curl "http://localhost:8080/api/assets"
curl "http://localhost:8080/api/assets/AAPL/statistics/average?from=2026-01-01&to=2026-01-31"
curl "http://localhost:8080/api/assets/AAPL/statistics/standard-deviation?from=2026-01-01&to=2026-01-31"
curl -X POST "http://localhost:8080/api/assets/refresh"
```

`PriceStatisticsControllerIT` assumes local mode (it checks values computed on the real sample data).

## 7. Included tests

- `SimpleActionRouterTest` (action-router-spring-boot-starter) — the heart of the mechanism: `route` dispatch by type, error when the handler is missing or when there are two, startup failure when a required action has no handler, `broadcast` to many handlers, `broadcast` with no handlers, a handler exception that doesn't reach the caller and doesn't stop the following handlers.
- `ActionRouterAutoConfigurationTest` (action-router-spring-boot-starter) — the auto-configuration finds the actions on the classpath and prevents startup if one has no handler, unless it's in `action-router.ignored-actions`.
- `PricePointTest` (market-data-action) — value type validation.
- `MarketDataStoreTest`, `MarketDataQueryHandlersTest` — market-data logic, info broadcast only on refresh, and delegation of the three handlers.
- `PriceStatisticsCalculatorTest`, `AnalyticsCalculationServiceTest`, `AnalyticsQueryHandlersTest`, `MarketDataRefreshedHandlerTest` — analytics logic (based on a mocked `ActionRouter`), delegation of the two handlers and cache invalidation.
- `RemoteAnalyticsClientTest` (analytics-restclient-starter) — HTTP call and error translation with `MockRestServiceServer`.
- `PriceStatisticsControllerIT` — end-to-end via MockMvc (local mode).

## 8. Directions to explore

- An asynchronous `broadcast` (on an `Executor`): what is gained and what is lost (ordering, deterministic tests, error visibility).
- Delivery guarantee: a persistent registry of infos with retry, built on top of the router.
- A `ActionRouter` implementation that forwards actions and infos over HTTP or a queue (Kafka/RabbitMQ), without changing the callers.
- `market-data-restclient-starter`: extract Market Data too and see what happens to infos when the broadcaster lives in another process.
- An ArchUnit test to replace the boundary verification lost with Spring Modulith.
