# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A POC of a **modular monolith** built with **Spring Modulith** and a **Gradle multi-module** layout (Java 21, Spring Boot 3.4.1, Spring Modulith 1.3.1, Gradle wrapper 8.5). Domain: market data + price analytics. The README (in Italian) is the authoritative architecture reference; this file summarizes the parts that matter for working in the code.

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

**`app` never imports a domain service interface.** It talks to domain modules only by sending **contracts (pure data)** through a generic `ContractBus`. This is the central design point of the whole POC; preserve it.

- A **request/response** call is `contractBus.send(new SomeQuery(...))`; the return type is the `R` the contract declares. There is no `AnalyticsService`/`MarketDataService` interface anywhere — the bus routes **by contract type**, not by service.
- Every request record implements the marker `Contract<R>` (module `base-contract`, package `com.example.basecontract`), declaring its response type: e.g. `AverageQuery implements Contract<PriceStatistics>`, `RefreshMarketDataCommand implements Contract<Void>`. `ContractBus.send(Contract<R>)` returns `R`, and `ContractHandler<C extends Contract<R>, R>` must agree with it — a mismatch is a compile error. `Contract` lives in its own module so domain contracts depend only on it, **never on the bus**.
- `SimpleContractBus` (in `contract-bus-spring-boot-starter`) collects every `ContractHandler` bean at startup and indexes it by the contract type it handles, resolved via reflection: `ResolvableType.forClass(handler.getClass()).as(ContractHandler.class).getGeneric(0).resolve()`. This is the one "magic" line — a handler must implement `ContractHandler<SomeContract, SomeResponse>` **directly** (an AOP proxy that erases the generic would break dispatch).
- **Which module handles a contract is decided only by `app/build.gradle`**, not by `app`'s Java code. `-PanalyticsMode=remote` swaps `analytics-spring-boot-starter` for `analytics-restclient-starter` on the classpath — no `com.example.app` source changes. The two are **mutually exclusive** Gradle deps; wiring both would register two handlers for the same contract and fail.
- Analytics → Market Data communication also goes through the bus (`contractBus.send(new FindPricesQuery(...))`), never a direct call. Analytics depends on `market-data-contract` only for the data **types**.
- **`MarketDataRefreshed` is an event, NOT a bus contract.** It is published via Spring's `ApplicationEventPublisher` / `@ApplicationModuleListener` (fire-and-forget pub/sub, one-to-many). Keep this separate from the synchronous request/response bus — don't merge the two mechanisms.

## Module layout

Each domain follows a **contract / implementation** split:

- **`*-contract`** modules (`market-data-contract`, `analytics-contract`) — pure domain types: request/response records and shared value types. Their **only** dependency is `api project(':base-contract')` — no Spring, no bus, no other module; no service interfaces of any kind.
- **`*-spring-boot-starter`** modules — the implementation, exposed as a Spring Boot starter with auto-configuration (registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`). All implementation classes live under an **`internal/`** package and are **package-private** — that Java visibility is the real module boundary enforcement. Each starter declares `implementation project(':contract-bus')` directly (not transitively via the contract module).
- `analytics-restclient-starter` — an *alternative* analytics implementation that calls an external HTTP service exposing the same REST contract, translating HTTP 404/422 into the same domain exceptions.

`base-contract` and the `*-contract` modules deliberately carry **no** `@ApplicationModule` annotation (they are framework-agnostic); `ApplicationModules.verify()` still works by package convention.

## REST API (served by `app`)

| Method | Path |
|---|---|
| `GET` | `/api/assets` |
| `POST` | `/api/assets/refresh` (202) |
| `GET` | `/api/assets/{symbol}/statistics/average?from=YYYY-MM-DD&to=YYYY-MM-DD` |
| `GET` | `/api/assets/{symbol}/statistics/standard-deviation?from=YYYY-MM-DD&to=YYYY-MM-DD` |

Errors are RFC 7807 Problem Details: `404` unknown asset, `422` no data in range, `400` `from` after `to`. Sample data (`AAPL`, `MSFT`, Jan 2026) lives in `app/src/main/resources/data/*.json`; market-data reads from `market-data.directory` (default `classpath:data/`, configurable in `application.yml`).

## Testing notes

- `SimpleContractBusTest` covers the dispatch mechanism (correct routing by type + clear error when no handler is registered).
- Module tests (`MarketDataModuleTests`, `AnalyticsModuleTests`) run inside each starter, where the test classpath unites contract + `internal`. `ModularityTests` in `app` verifies only the web module. Gradle cross-artifact dependencies are not covered by `verify()`.
- `PriceStatisticsControllerIT` is an end-to-end MockMvc test that **assumes local (in-process) mode** — it asserts values computed from the real sample data.
- `RemoteAnalyticsClientTest` uses `MockRestServiceServer` to simulate the external analytics service without starting it.

## Persistence

H2 in-memory (`application.yml`) backs the Spring Modulith Event Publication Registry (`spring-modulith-starter-jdbc`), which guarantees event delivery without an external broker. Relevant only when market-data runs in-process (it publishes `MarketDataRefreshed`).
