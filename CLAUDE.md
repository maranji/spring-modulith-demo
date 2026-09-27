# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A POC of a **modular monolith** with a **Gradle multi-module** layout (Java 21, Spring Boot 3.4.1, Gradle wrapper 8.5) whose modules communicate **only** through a hand-written `ContractBus`. The goal is to push the bus as far as possible to understand its limits; Spring Modulith was deliberately removed. Domain: market data + price analytics. The README (in Italian) is the authoritative architecture reference; this file summarizes the parts that matter for working in the code.

## Build & Run

```bash
./gradlew build                                  # compile + all tests (analytics in-process, default)
./gradlew :app:bootRun                           # run the app on :8080 (analytics in-process)
./gradlew :app:bootRun -PanalyticsMode=remote    # run with analytics as an external HTTP service
./gradlew :<module>:test                         # run one module's tests, e.g. :analytics-spring-boot-starter:test
./gradlew test --tests '*SimpleContractBusTest'   # run a single test class by name
./gradlew :app:printAnalyticsMode                # print which analytics implementation is wired in
```

## Core architectural principle — read this before changing module wiring

**`app` never imports a domain service interface.** It talks to domain modules only by sending **contracts (pure data)** through a generic `ContractBus`, and domain modules talk to each other the same way. This is the central design point of the whole POC; preserve it. Don't reintroduce Spring's `ApplicationEventPublisher`/`@EventListener` or Spring Modulith for inter-module communication.

- A **request/response** call is `contractBus.send(new SomeQuery(...))`; the return type is the `R` the contract declares. There is no `AnalyticsService`/`MarketDataService` interface anywhere — the bus routes **by contract type**, not by service.
- Every request record implements the marker `Contract<R>` (module `base-contract`, package `com.example.basecontract`), declaring its response type: e.g. `AverageQuery implements Contract<PriceStatistics>`, `RefreshMarketDataCommand implements Contract<Void>`. `ContractBus.send(Contract<R>)` returns `R`, and `ContractHandler<C extends Contract<R>, R>` must agree with it — a mismatch is a compile error. `Contract` lives in its own module so domain contracts depend only on it, **never on the bus**.
- **One-to-many notifications** implement the marker `Info` (also in `base-contract`) and go through `contractBus.broadcast(info)`, handled by `InfoHandler<I extends Info>`. `send` requires exactly one handler; `broadcast` accepts zero or more. Keep commands (`Contract<Void>`) and infos distinct. The name is deliberate: avoid "event" naming, which would suggest an event-driven architecture.
- `SimpleContractBus` (in `contract-bus-spring-boot-starter`) collects every `ContractHandler` and `InfoHandler` bean at startup and indexes them by the type they handle, resolved via reflection: `ResolvableType.forClass(handler.getClass()).as(ContractHandler.class).getGeneric(0).resolve()`. This is the one "magic" line — a handler must implement `ContractHandler<SomeContract, SomeResponse>`/`InfoHandler<SomeInfo>` **directly** (an AOP proxy that erases the generic would break dispatch). `broadcast` is currently synchronous, in the caller's thread; a failing `InfoHandler` is logged and never reaches the caller, and the following handlers still run.
- **Which module handles a contract is decided only by `app/build.gradle`**, not by `app`'s Java code. `-PanalyticsMode=remote` swaps `analytics-spring-boot-starter` for `analytics-restclient-starter` on the classpath — no `com.example.app` source changes. The two are **mutually exclusive** Gradle deps; wiring both would register two handlers for the same contract and fail.
- Analytics → Market Data communication also goes through the bus (`contractBus.send(new FindPricesQuery(...))`), never a direct call. Analytics depends on `market-data-contract` only for the data **types**.
- `MarketDataRefreshed` is an `Info`: `MarketDataStore` broadcasts it on refresh (manual or scheduled) and `MarketDataRefreshedHandler` in analytics clears the cache. The initial `@PostConstruct` load deliberately does **not** broadcast it: the first bus call indexes every handler, including `FindPricesQueryHandler`, which depends on the `MarketDataStore` still being created (`BeanCurrentlyInCreationException`).
- The README section "Limiti noti" tracks the bus's known limits; update it when an experiment uncovers a new one.

## Module layout

Each domain follows a **contract / implementation** split:

- **`*-contract`** modules (`market-data-contract`, `analytics-contract`) — pure domain types: request/response records, infos and shared value types. Their **only** dependency is `api project(':base-contract')` — no Spring, no bus, no other module; no service interfaces of any kind.
- **`*-spring-boot-starter`** modules — the implementation, exposed as a Spring Boot starter with auto-configuration (registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`). All implementation classes live under an **`internal/`** package (`remote/` in `analytics-restclient-starter`) and are **package-private** — that Java visibility is the only module boundary enforcement. Each starter declares `implementation project(':contract-bus')` directly (not transitively via the contract module).
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

- `SimpleContractBusTest` covers the dispatch mechanism: `send` routing by type, missing/duplicate handler errors, `broadcast` to many/zero handlers, and a failing info handler neither reaching the caller nor stopping the following ones.
- `PriceStatisticsControllerIT` is an end-to-end MockMvc test that **assumes local (in-process) mode** — it asserts values computed from the real sample data.
- `RemoteAnalyticsClientTest` uses `MockRestServiceServer` to simulate the external analytics service without starting it.
