# Modular Monolith Architecture with Spring Modulith

An analysis of how to build a monolithic application based on service modules, designed from the start so that modules can be extracted into independent services when needed.

---

## 1. Asynchronous communication between modules in Java: available options

The choice depends on whether the modules live in the same JVM/process or in separate processes/services.

### Same application (same process)

No real broker is needed:

- **`java.util.concurrent.BlockingQueue`** (`LinkedBlockingQueue`, `ArrayBlockingQueue`) — a simple, native solution with zero external dependencies.
- **In-memory event bus**: Guava `EventBus`, or Spring `ApplicationEventPublisher` + `@EventListener` (optionally `@Async`).
- **LMAX Disruptor** — for extreme throughput/latency, but harder to use well.
- **Vert.x Event Bus** — if the architecture is already based on Vert.x; it also works in a cluster.
- **Akka / Apache Pekko** — actor model, suitable when module logic is complex and stateful.

In most cases a `BlockingQueue` + `ExecutorService`, or an event bus (Guava/Spring), is enough and simpler to maintain.

### Separate processes/services

Here a real message broker makes sense:

- **RabbitMQ** — widely used, mature AMQP client, a good balance of simplicity and robustness.
- **Apache Kafka** — high throughput, log-based persistence, event streaming.
- **ActiveMQ Artemis** — native JMS support (`javax.jms`/`jakarta.jms`).
- **NATS** — lightweight and fast, good Java client.

### What about ZeroMQ?

ZeroMQ **is not a broker**, it is a "brokerless" messaging library (direct socket-to-socket communication). There is no native Java binding: you use **JeroMQ** (pure Java port) or **jzmq** (JNI binding).

- It makes sense for very low-latency communication between different processes/machines, without broker overhead, handling routing/retry/persistence yourself (ZeroMQ doesn't offer these guarantees by default).
- It **makes no sense** inside the same JVM: it adds complexity (socket management, I/O threads, serialization) to solve a problem that a BlockingQueue/event bus solves with much less code.

---

## 2. "Modular monolith with extraction path" architecture

Pattern: build a monolith, but with boundaries already ready to become distributed.

### Guiding principle

Each module communicates with the others **only through well-defined messages/events**, never through direct calls to other modules' methods or direct access to their state/DB. If this constraint holds, a future extraction becomes a change of transport, not a redesign.

### Architecture components

1. **Boundaries with explicit contracts**: each module exposes Commands (request/response) and Events (fire-and-forget), with serializable DTOs/POJOs instead of shared domain objects.

2. **Transport abstracted behind an interface**:
```java
public interface MessageBus {
    void publish(String topic, Message msg);
    void subscribe(String topic, MessageHandler handler);
    <T> CompletableFuture<T> request(String destination, Message msg);
}
```
   - Monolithic phase: in-memory implementation.
   - After extraction: RabbitMQ/Kafka/NATS, application code unchanged.

3. **Modules as "quasi-microservices"**: separate packages/modules (Maven/Gradle multi-project, possibly JPMS), a separate logical DB schema per module, no cross-module JOINs, independent configuration/logging/health checks.

4. **Technologies that work both in-process and distributed**:
   - **Vert.x**: the same Event Bus whether verticles are in the same JVM or clustered (Hazelcast/Infinispan/Zookeeper); moving from monolith to distributed is a configuration change.
   - **Axon Framework**: command bus/event bus abstracted from the transport, designed for CQRS/Event Sourcing.
   - **Spring Modulith**: modular monolith → microservices, with support for application events that can become Kafka/RabbitMQ events with minimal configuration changes.

5. **Data**: each module writes only to its own tables; if it needs another module's data, it uses events (event-carried state transfer) or explicit requests, never direct queries.

---

## 3. Spring Modulith

A project officially supported by the Spring team, designed to build a monolith well structured into modules, with an extraction path towards microservices.

### Basic concept: modules = Java packages

```
com.mycompany.app
├── Application.java          (@SpringBootApplication)
├── order/                    ← "order" module
│   ├── OrderService.java
│   ├── OrderCreated.java     (public event of the module)
│   └── internal/             ← internal package, not visible outside
│       ├── OrderRepository.java
│       └── OrderEntity.java
├── inventory/
│   └── ...
└── shipping/
    └── ...
```

Automatic rules:
- Every top-level package under the root is a module.
- Classes in `internal/` are encapsulated: violations are reported at compile/test time.
- Classes outside `internal/` are the module's public API.

### Automatic architecture verification

```java
class ModularityTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify(); // fails on encapsulation violations or cyclic dependencies
    }

    @Test
    void createDocumentation() {
        new Documenter(ApplicationModules.of(Application.class))
            .writeDocumentation(); // generates PlantUML/C4 diagrams
    }
}
```

A test that runs in CI, blocks merges with unwanted coupling, and generates always up-to-date architectural documentation.

### Communication between modules: application events

```java
// "order" module
@Service
public class OrderService {
    private final ApplicationEventPublisher events;

    public void placeOrder(Order order) {
        events.publishEvent(new OrderPlaced(order.getId(), order.getItems()));
    }
}
```

```java
// "inventory" module
@Component
class InventoryEventListener {

    @ApplicationModuleListener   // Modulith's key annotation
    void on(OrderPlaced event) {
        // decrement stock
    }
}
```

Compared to a plain `@EventListener`, `@ApplicationModuleListener` offers:
1. Asynchronous execution by default (separate thread pool).
2. Transaction-safe execution: delivery after commit (`AFTER_COMMIT`).
3. Event persistence (Event Publication Registry).

### Event Publication Registry

Every published event is saved to a table (`EVENT_PUBLICATION`) before delivery. If the listener fails, the event stays "incomplete" and is redelivered on restart or via retry. An "at-least-once delivery" guarantee without a message broker, with `spring-modulith-events-jdbc` or `-mongodb`.

```sql
CREATE TABLE event_publication (
    id UUID,
    listener_id VARCHAR,
    event_type VARCHAR,
    serialized_event VARCHAR,
    publication_date TIMESTAMP,
    completion_date TIMESTAMP  -- NULL until successfully processed
);
```

### Extraction path

- **Option A — Transport replacement**: by adding `spring-modulith-events-kafka` (or AMQP), the same `@ApplicationModuleListener` events are serialized and published to Kafka automatically, without changing application code.
- **Option B — Named Interfaces**: explicit APIs between modules for direct synchronous calls, useful if internal synchronous communication is needed besides events.

### Testing modules in isolation

```java
@ApplicationModuleTest
class OrderModuleTest {
    // starts only the "order" module, not the whole application
}
```

### Summary table

| Need | What Spring Modulith provides |
|---|---|
| Module boundaries respected | `internal/` packages + `verify()` in CI |
| Asynchronous communication between modules | `@ApplicationModuleListener` (async, transactional) |
| Delivery guarantee without a broker | Event Publication Registry on DB |
| Extraction path | Change of event driver (in-memory → Kafka/RabbitMQ) without touching the domain |
| Architectural documentation | Automatic generation of module diagrams |
| Isolated tests per module | `@ApplicationModuleTest` |

---

## 4. Different implementations per country in the same codebase

The module's public API (e.g. `OrderService`) stays unique; the different implementations stay inside `internal/`.

```
order/
├── OrderService.java              (public interface)
├── OrderPlaced.java               (public event)
└── internal/
    ├── OrderServiceItImpl.java    (Italy implementation)
    ├── OrderServiceDeImpl.java    (Germany implementation)
    └── OrderServiceConfig.java
```

### Level 1 — Strategy pattern + Spring Profiles

Suitable when each deployment of the app serves a single country.

```java
public interface OrderService {
    Order placeOrder(OrderRequest request);
}

@Service
@Profile("country-it")
class OrderServiceItImpl implements OrderService { /* Italian VAT, etc. */ }

@Service
@Profile("country-de")
class OrderServiceDeImpl implements OrderService { /* German VAT, etc. */ }
```

```properties
# application-it.properties
spring.profiles.active=country-it
```

### Level 2 — Dynamic strategy at runtime

For when the same instance must handle several countries at once (multi-tenant):

```java
public interface OrderStrategy {
    boolean supports(Country country);
    Order placeOrder(OrderRequest request);
}

@Component
class OrderServiceItStrategy implements OrderStrategy {
    public boolean supports(Country country) { return country == Country.IT; }
    public Order placeOrder(OrderRequest request) { /* ... */ }
}
```

```java
@Service
class OrderService {
    private final List<OrderStrategy> strategies; // Spring injects every implementation

    public Order placeOrder(OrderRequest request) {
        return strategies.stream()
            .filter(s -> s.supports(request.getCountry()))
            .findFirst()
            .orElseThrow(() -> new UnsupportedCountryException(request.getCountry()))
            .placeOrder(request);
    }
}
```

### Level 3 — Configuration for minor parameters/rules

If only values change (VAT rate, address format, currency), a different class isn't needed:

```java
@ConfigurationProperties(prefix = "order.country")
public class CountryOrderProperties {
    private BigDecimal vatRate;
    private String addressFormat;
}
```

```yaml
order:
  country:
    vat-rate: 0.22
```

### Module tests with profiles

```java
@ApplicationModuleTest
@ActiveProfiles("country-it")
class OrderModuleItTest {
    // tests only the IT behavior
}
```

### Choosing an approach

| Scenario | Approach |
|---|---|
| One deployment per country, substantial logic differences | Spring Profiles + separate implementations |
| One multi-country deployment, substantial logic differences | Runtime strategy pattern |
| Same logic, only parameters/rules change | `@ConfigurationProperties` per country |
| Differences so large they deserve independent deployments | Extraction as a separate module/service |

---

## 5. Handling country-specific data

### Level 0 — To avoid: a single table with per-country columns

A table with country-prefixed columns (`it_codice_fiscale`, `de_ust_id`, etc.): it works with a few countries, but becomes unreadable and coupled with 5+.

### Level 1 — Common core + per-country extension (composition)

```java
@Entity
@Table(name = "order_entity")
class OrderEntity {
    @Id Long id;
    BigDecimal amount;
    String currency;
    String countryCode;
}

@Entity
@Table(name = "order_extension_it")
class OrderExtensionIt {
    @Id Long orderId;
    String codiceFiscale;
    String lotteryCode;
}
```

The module's public API exposes only the domain object (`Order`), never the specific JPA entities. Each country adds its own table without touching the others; if a country is extracted, its table goes with it.

### Level 2 — Typed domain hierarchy (JPA Inheritance)

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "country_code")
abstract class OrderEntity {
    @Id Long id;
    BigDecimal amount;
    String currency;
}

@Entity
@DiscriminatorValue("IT")
class OrderEntityIt extends OrderEntity {
    String codiceFiscale;
    String lotteryCode;
}
```

`JOINED` generates one table for the base class and one per subclass; `SINGLE_TABLE` is lighter (one table, nullable columns) but drifts back towards the Level 0 problem.

### Level 3 — JSON column for country-specific data

```java
@Entity
@Table(name = "order_entity")
class OrderEntity {
    @Id Long id;
    BigDecimal amount;
    String countryCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    Map<String, Object> countryData;
}
```

```java
CountrySpecificDataIt data = objectMapper.convertValue(
    entity.getCountryData(), CountrySpecificDataIt.class);
```

No migration is needed to add fields/countries, but DB constraints are lost and queries on JSON fields are less efficient.

### Connection with the Strategy pattern

```java
public interface OrderStrategy {
    boolean supports(Country country);
    Order placeOrder(OrderRequest request);
    OrderPersistenceHandler persistenceHandler();
}
```

Adding a country (new strategy + new table/extension) is only an addition, never a modification (Open/Closed Principle).

### Iron rule

Country-specific JPA entities stay in `internal/`, never exposed outside the module. Consumers of the module see only the public domain object. This is crucial for extraction: the table/extension migrates with the country without other modules ever having queried it directly.

### Choosing an approach

| Scenario | Data approach |
|---|---|
| 2-3 countries, few extra fields, stable | Core + 1:1 extension tables |
| Strong type-safety and polymorphic queries | JPA Inheritance JOINED |
| Countries/fields that change often | JSONB column with typed mapping |
| Heavy SQL reporting on the extra fields | Avoid JSON, prefer relational tables |

Practical advice: keep core and per-country extension migrations in separate Flyway/Liquibase changesets per country.

---

## 6. Spring Modulith module vs Java module (JPMS)

These are two independent concepts:

- **Spring Modulith**: a package convention, verified at runtime/test time through bytecode analysis (ArchUnit); it doesn't require JPMS.
- **JPMS**: a language/JVM mechanism (`module-info.java`), checked at compile time and at runtime by the class loader.

### Why combining them is rare in practice

1. Spring relies on reflection and classpath scanning; JPMS, with its strong encapsulation, requires additional configuration (`opens`, `exports to`) — a historical source of friction.
2. Spring Modulith works at the package level inside the same JPMS module/artifact; splitting each application module into distinct JPMS modules would also require distinct Maven/Gradle artifacts — a significant jump in complexity during the monolithic phase.

### Recommended pragmatic approach

- **Logical boundaries** → Spring Modulith (package convention + `verify()` in CI)
- **Physical/build boundaries** → separate Maven/Gradle modules (multi-module project): separate compilation, explicit dependencies, the option to publish as a versioned artifact
- **JPMS** → generally avoided except for specific needs (jlink, strict encapsulation compliance)

```
my-app/
├── app-order/
│   ├── pom.xml
│   └── src/main/java/com/app/order/...
├── app-inventory/
├── app-shipping/
└── app-bootstrap/        ← holds the @SpringBootApplication
    └── pom.xml
```

Spring Modulith's `internal/` convention still applies inside each Maven module; Maven provides the "hard" boundary through declared dependencies.

### Summary table

| Boundary level | Mechanism | When to use it |
|---|---|---|
| Logical, same deployable | Spring Modulith (`internal/`) | Always, base case |
| Physical, build-level | Separate Maven/Gradle modules | Recommended as soon as modules stabilize, a prerequisite for extraction |
| JPMS | `module-info.java` | Only for specific needs (jlink, compliance) |

---

## 7. A Spring Modulith module as a reusable Spring Boot Starter

Two different goals not to be confused:
- **Extraction**: a module that lives in the monolith today becomes a separate service tomorrow.
- **Reuse**: a module shared, as is, among several different applications.

### Mechanics

A starter is a Maven/Gradle artifact with auto-configuration (`AutoConfiguration.imports`) that registers beans in the host Spring context — orthogonal to Spring Modulith.

### Explicit module declaration

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Audit Module"
)
package com.mycompany.auditstarter;

import org.springframework.modulith.ApplicationModule;
```

### Typical structure

```
audit-spring-boot-starter/
├── pom.xml
└── src/main/java/com/mycompany/auditstarter/
    ├── AuditEvent.java
    ├── AuditService.java
    ├── AuditAutoConfiguration.java
    └── internal/
        ├── AuditServiceImpl.java
        └── AuditRepository.java
```

```java
@AutoConfiguration
@ConditionalOnClass(AuditService.class)
@EnableConfigurationProperties(AuditProperties.class)
class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditService auditService(AuditRepository repo) {
        return new AuditServiceImpl(repo);
    }
}
```

### Communication via events

Once the starter is included, everything runs in the same Spring context: its `@ApplicationModuleListener`s receive events published by the host app's modules and vice versa, with no transport configuration — the same shared `ApplicationEventPublisher`. Useful for cross-cutting modules (audit, notifications, multi-tenancy, feature flags, i18n).

```java
@ApplicationModuleListener
void on(OrderPlaced event) {
    auditService.record("Order placed: " + event.orderId());
}
```

### Caveat on verify()

The exact behavior of `ApplicationModules.verify()` for modules in packages outside the app's root depends on the Spring Modulith version (an evolving area). Many teams treat the starter as an "open module" (allowed dependency towards everything) rather than subjecting it to the same strict verification as domain modules.

### Don't confuse the two goals

| Module type | Recommended pattern |
|---|---|
| Domain module (order, inventory) — candidate for future extraction | Internal package + Spring Modulith `internal/`, separate Maven modules |
| Cross-cutting/technical module reused across different products | Separately versioned Spring Boot starter, possibly with `@ApplicationModule` |

If a module must be both extractable and reusable: **a starter for the shared core logic** + **a local Modulith module in each app that extends/configures it** (Strategy pattern applied to product differences).

---

## 8. Shared Kernel: sharing classes like `Order` between app and starter

A well-known DDD problem: the **Shared Kernel** introduces coupling between bounded contexts. Solving it well now prepares for a future extraction into microservices (the same problem would come up with separate services).

### Key principle: share the contract, not the aggregate

Don't put the whole `Order` class (with JPA, business logic, invariants) in the shared library. Share only the minimal portion needed — typically the event, not the aggregate.

```
order-contracts/                    ← shared library, MINIMAL
└── src/main/java/com/app/order/contracts/
    ├── OrderPlaced.java            ← immutable record
    ├── OrderCancelled.java
    └── OrderId.java                ← typed value object
```

```java
// order-contracts — no JPA, no logic, just data
public record OrderPlaced(
    OrderId orderId,
    String countryCode,
    BigDecimal amount,
    Instant placedAt
) {}
```

The app's `order` module keeps its JPA entities completely internal:

```
app/order/
├── internal/
│   ├── OrderEntity.java           ← JPA, stays ONLY here
│   ├── OrderEntityIt.java
│   └── OrderRepository.java
└── OrderService.java              ← uses order-contracts for events
```

Dependency graph:
```
audit-starter    → depends on → order-contracts
app (order)      → depends on → order-contracts
order-contracts  → depends on neither
```

A separate node in the graph, avoiding cycles and two-way coupling.

### Why it reduces complexity

The contracts library is:
- **Small**: only immutable records/DTOs, zero logic, minimal dependencies (just Jackson if serialization is needed).
- **Stable**: it rarely changes, it represents a public contract.
- **Semantically versioned**: breaking change = major bump, like a REST API or an Avro/Protobuf schema.

### Interface Segregation: targeted events

Avoid a single "one-size-fits-all" DTO with dozens of fields. Prefer use-case-specific events:

```java
// for audit
public record OrderPlaced(OrderId orderId, String countryCode, Instant placedAt) {}

// for loyalty
public record OrderCompleted(OrderId orderId, BigDecimal amount, CustomerId customerId) {}
```

This minimizes the "blast radius" of changes.

### Lighter alternative: generic notifications

If the starter only needs to know that "something happened" (e.g. to invalidate a cache), you can pass only primitive identifiers:

```java
public record DomainEventOccurred(String eventType, String aggregateId, Instant occurredAt) {}
```

A more generic and stable contract, at the cost of type-safety and a possible callback to fetch the details. Useful only if the starter is genuinely domain-agnostic.

### Connection with future extraction

When `order` is extracted as a service, `order-contracts` becomes the schema of the events published on Kafka/RabbitMQ (possibly generated from Avro/JSON Schema). The work of separating contract and implementation, done now for the starter, is the same prerequisite that extraction requires.

### Summary

| What NOT to share | What to share |
|---|---|
| JPA entities (`OrderEntity`) | Immutable events/DTOs (`OrderPlaced`) |
| Business logic (`OrderService` impl) | Minimal value objects (`OrderId`) |
| Repositories, queries | Contract interfaces, if synchronous invocation is needed |

| Principle | Why |
|---|---|
| Separate contracts library, with no dependency on app/starter | Avoids cycles, isolates evolution |
| Small, consumer-specific contracts | Reduces the blast radius of changes |
| Strict semantic versioning | Breaking changes handled like a public API |
| Zero JPA/Spring in the contracts library | Stays reusable outside this Spring context too |

Practical note: if the number of modules/starters grows, consider a schema registry even for the in-process case (e.g. generating Java classes from JSON Schema with a Maven/Gradle plugin), useful above all if a future extraction will need to interact with non-Java services. To start with, writing the records by hand in `order-contracts` is enough.
