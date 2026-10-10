# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A POC of a **modular monolith** with a **Gradle multi-module** layout (Java 21, Spring Boot 3.4.1, Gradle wrapper 8.5) whose modules communicate **only** through a hand-written `ActionRouter`. The goal is to push the router as far as possible to understand its limits; Spring Modulith was deliberately removed. Domain: market data + price analytics. The README is the authoritative architecture reference; this file summarizes the parts that matter for working in the code.

## Build & Run

```bash
./gradlew build                                  # compile + all tests (analytics in-process, default)
./gradlew :app:bootRun                           # run the app on :8080 (analytics in-process)
./gradlew :app:bootRun -PanalyticsMode=remote    # run with analytics as an external HTTP service
./gradlew :<module>:test                         # run one module's tests, e.g. :analytics-spring-boot-starter:test
./gradlew test --tests '*SimpleActionRouterTest'  # run a single test class by name
./gradlew :app:printAnalyticsMode                # print which analytics implementation is wired in
```

## Core architectural principle — read this before changing module wiring

**`app` never imports a domain service interface.** It talks to domain modules only by sending **actions (pure data)** through a generic `ActionRouter`, and domain modules talk to each other the same way. This is the central design point of the whole POC; preserve it. Don't reintroduce Spring's `ApplicationEventPublisher`/`@EventListener` or Spring Modulith for inter-module communication.

- A **request/response** call is `actionRouter.route(new SomeQuery(...))`; the return type is the `R` the action declares. There is no `AnalyticsService`/`MarketDataService` interface anywhere — the router routes **by action type**, not by service.
- Every request record implements the marker `Action<R>` (module `action-core`, package `org.maranji.actioncore`), declaring its response type: e.g. `AverageQuery implements Action<PriceStatistics>`, `RefreshMarketDataCommand implements Action<Void>`. `ActionRouter.route(Action<R>)` returns `R`, and `ActionHandler<C extends Action<R>, R>` must agree with it — a mismatch is a compile error. `Action` lives in its own module so domain actions depend only on it, **never on the router**.
- **One-to-many notifications** implement the marker `Info` (also in `action-core`) and go through `actionRouter.broadcast(info)`, handled by `InfoHandler<I extends Info>`. `route` requires exactly one handler; `broadcast` accepts zero or more. Keep commands (`Action<Void>`) and infos distinct. The name is deliberate: avoid "event" naming, which would suggest an event-driven architecture.
- `SimpleActionRouter` (in `action-router-spring-boot-starter`) collects every `ActionHandler` and `InfoHandler` bean at startup and indexes them by the type they handle, resolved via reflection: `ResolvableType.forClass(handler.getClass()).as(ActionHandler.class).getGeneric(0).resolve()`. This is the one "magic" line — a handler must implement `ActionHandler<SomeAction, SomeResponse>`/`InfoHandler<SomeInfo>` **directly** (an AOP proxy that erases the generic would break dispatch). `broadcast` is currently synchronous, in the caller's thread; a failing `InfoHandler` is logged and never reaches the caller, and the following handlers still run. At startup (`afterSingletonsInstantiated`) the router also scans the classpath for every concrete `Action` under `action-router.base-packages` (default `org.maranji`) and fails if one has no handler, so a missing implementation starter surfaces at boot, not on the first `route`; `action-router.ignored-actions` lists the exceptions.
- **Which module handles an action is decided only by `app/build.gradle`**, not by `app`'s Java code. `-PanalyticsMode=remote` swaps `analytics-spring-boot-starter` for `analytics-restclient-starter` on the classpath — no `org.maranji.app` source changes. The two are **mutually exclusive** Gradle deps; wiring both would register two handlers for the same action and fail.
- Analytics → Market Data communication also goes through the router (`actionRouter.route(new FindPricesQuery(...))`), never a direct call. Analytics depends on `market-data-action` only for the data **types**.
- `MarketDataRefreshed` is an `Info`: `MarketDataStore` broadcasts it on refresh (manual or scheduled) and `MarketDataRefreshedHandler` in analytics clears the cache. The initial `@PostConstruct` load deliberately does **not** broadcast it: the first router call indexes every handler, including `FindPricesQueryHandler`, which depends on the `MarketDataStore` still being created (`BeanCurrentlyInCreationException`).
- The README section "Known limits" tracks the router's known limits; update it when an experiment uncovers a new one.

## Module layout

Each domain follows an **action / implementation** split:

- **`*-action`** modules (`market-data-action`, `analytics-action`) — pure domain types: request/response records, infos and shared value types. Their **only** dependency is `api project(':action-core')` — no Spring, no router, no other module; no service interfaces of any kind.
- **`*-spring-boot-starter`** modules — the implementation, exposed as a Spring Boot starter with auto-configuration (registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`). All implementation classes live under an **`internal/`** package (`remote/` in `analytics-restclient-starter`) and are **package-private** (except the `*AutoConfiguration` classes, which are `public`) — that Java visibility is the only module boundary enforcement. Each domain starter declares `implementation project(':action-router')` directly (not transitively via the action module); `action-router-spring-boot-starter` uses `api`, since it exposes the `ActionRouter` bean.
- `analytics-restclient-starter` — an *alternative* analytics implementation that calls an external HTTP service exposing the same REST contract, translating HTTP 404/422 into the same domain exceptions.

## REST API (served by `app`)

| Method | Path |
|---|---|
| `GET` | `/api/assets` |
| `POST` | `/api/assets/refresh` (202) |
| `GET` | `/api/assets/{symbol}/statistics/average?from=YYYY-MM-DD&to=YYYY-MM-DD` |
| `GET` | `/api/assets/{symbol}/statistics/standard-deviation?from=YYYY-MM-DD&to=YYYY-MM-DD` |

Errors are RFC 7807 Problem Details: `404` unknown asset, `422` no data in range, `400` `from` after `to`. Sample data (`AAPL`, `MSFT`, Jan 2026) lives in `app/src/main/resources/data/*.json`; market-data reads from `market-data.directory` (default `classpath:data/`, configurable in `application.yml`).

## Testing notes

- `SimpleActionRouterTest` covers the dispatch mechanism: `route` routing by type, missing/duplicate handler errors, startup failure when a required action has no handler, `broadcast` to many/zero handlers, and a failing info handler neither reaching the caller nor stopping the following ones.
- `ActionRouterAutoConfigurationTest` uses `ApplicationContextRunner` against fixture actions in `actionrouter.internal.fixture` (scan that package only, or `SimpleActionRouterTest`'s nested actions get picked up too).
- `PriceStatisticsControllerIT` is an end-to-end MockMvc test that **assumes local (in-process) mode** — it asserts values computed from the real sample data.
- `RemoteAnalyticsClientTest` uses `MockRestServiceServer` to simulate the external analytics service without starting it.
