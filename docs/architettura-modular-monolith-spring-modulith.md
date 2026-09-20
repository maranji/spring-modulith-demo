# Architettura Modular Monolith con Spring Modulith

Analisi su come costruire un'applicazione monolitica basata su moduli service, progettata fin dall'inizio per poter essere estratta in servizi indipendenti quando necessario.

---

## 1. Comunicazione asincrona tra moduli in Java: opzioni disponibili

La scelta dipende dal fatto che i moduli siano nella stessa JVM/processo oppure processi/servizi separati.

### Stessa applicazione (stesso processo)

Non serve un broker vero e proprio:

- **`java.util.concurrent.BlockingQueue`** (`LinkedBlockingQueue`, `ArrayBlockingQueue`) — soluzione semplice e nativa, zero dipendenze esterne.
- **Event Bus in-memory**: Guava `EventBus`, oppure con Spring `ApplicationEventPublisher` + `@EventListener` (eventualmente `@Async`).
- **LMAX Disruptor** — per throughput/latenza estremi, ma più complesso da usare bene.
- **Vert.x Event Bus** — se l'architettura è già basata su Vert.x; funziona anche in cluster.
- **Akka / Apache Pekko** — modello ad attori, adatto quando la logica dei moduli è complessa e stateful.

Per la maggior parte dei casi, una `BlockingQueue` + `ExecutorService`, oppure un event bus (Guava/Spring), sono sufficienti e più semplici da mantenere.

### Processi/servizi separati

Qui ha senso un vero message broker:

- **RabbitMQ** — molto usato, client AMQP maturo, buon compromesso semplicità/robustezza.
- **Apache Kafka** — throughput alto, persistenza log-based, event streaming.
- **ActiveMQ Artemis** — supporto JMS nativo (`javax.jms`/`jakarta.jms`).
- **NATS** — leggero e veloce, buon client Java.

### E ZeroMQ?

ZeroMQ **non è un broker**, è una libreria di messaging "brokerless" (comunicazione diretta socket-to-socket). In Java non esiste binding nativo: si usa **JeroMQ** (porting puro Java) o **jzmq** (binding JNI).

- Ha senso per comunicazione a bassissima latenza tra processi/macchine diverse, senza overhead di un broker, gestendo tu stesso routing/retry/persistenza (ZeroMQ non offre queste garanzie di default).
- **Non ha senso** dentro la stessa JVM: introduce complessità (gestione socket, thread I/O, serializzazione) per risolvere un problema che BlockingQueue/event bus risolvono con molto meno codice.

---

## 2. Architettura "modular monolith with extraction path"

Pattern: costruire monolitico ma con confini già pronti per diventare distribuiti.

### Principio guida

Ogni modulo comunica con gli altri **solo tramite messaggi/eventi ben definiti**, mai tramite chiamate dirette a metodi di altri moduli o accesso diretto al loro stato/DB. Rispettando questo vincolo, l'estrazione futura diventa un cambio di trasporto, non un redesign.

### Componenti dell'architettura

1. **Confini con contratti espliciti**: ogni modulo espone Comandi (richiesta/risposta) ed Eventi (fire-and-forget), con DTO/POJO serializzabili invece di oggetti di dominio condivisi.

2. **Trasporto astratto dietro un'interfaccia**:
```java
public interface MessageBus {
    void publish(String topic, Message msg);
    void subscribe(String topic, MessageHandler handler);
    <T> CompletableFuture<T> request(String destination, Message msg);
}
```
   - Fase monolitica: implementazione in-memory.
   - Dopo estrazione: RabbitMQ/Kafka/NATS, codice applicativo invariato.

3. **Moduli come "quasi-microservizi"**: package/moduli separati (Maven/Gradle multi-project, eventualmente JPMS), schema DB logico separato per modulo, niente JOIN cross-modulo, configurazione/logging/health check indipendenti.

4. **Tecnologie che funzionano sia in-process che distribuite**:
   - **Vert.x**: Event Bus identico che i verticle siano nella stessa JVM o clusterizzati (Hazelcast/Infinispan/Zookeeper); passare da monolite a distribuito è un cambio di configurazione.
   - **Axon Framework**: command bus/event bus astratti dal trasporto, pensato per CQRS/Event Sourcing.
   - **Spring Modulith**: modular monolith → microservizi con supporto per eventi applicativi che possono diventare eventi Kafka/RabbitMQ con cambio di configurazione minimo.

5. **Dati**: ogni modulo scrive solo nelle proprie tabelle; se serve accedere a dati di un altro modulo, si usano eventi (event-carried state transfer) o richieste esplicite, mai query dirette.

---

## 3. Spring Modulith

Progetto ufficialmente supportato dal team Spring, pensato per costruire un monolite ben strutturato in moduli con path di estrazione verso microservizi.

### Concetto base: moduli = package Java

```
com.miaazienda.app
├── Application.java          (@SpringBootApplication)
├── order/                    ← modulo "order"
│   ├── OrderService.java
│   ├── OrderCreated.java     (evento pubblico del modulo)
│   └── internal/             ← package interno, non visibile fuori
│       ├── OrderRepository.java
│       └── OrderEntity.java
├── inventory/
│   └── ...
└── shipping/
    └── ...
```

Regole automatiche:
- Ogni package di primo livello sotto la root è un modulo.
- Le classi in `internal/` sono incapsulate: violazioni segnalate a compile-time/test-time.
- Le classi fuori da `internal/` sono l'API pubblica del modulo.

### Verifica automatica dell'architettura

```java
class ModularityTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.verify(); // fallisce su violazioni di incapsulamento o dipendenze cicliche
    }

    @Test
    void createDocumentation() {
        new Documenter(ApplicationModules.of(Application.class))
            .writeDocumentation(); // genera diagrammi PlantUML/C4
    }
}
```

Test eseguibile in CI, blocca merge con accoppiamenti non voluti; genera documentazione architetturale sempre aggiornata.

### Comunicazione tra moduli: eventi applicativi

```java
// modulo "order"
@Service
public class OrderService {
    private final ApplicationEventPublisher events;

    public void placeOrder(Order order) {
        events.publishEvent(new OrderPlaced(order.getId(), order.getItems()));
    }
}
```

```java
// modulo "inventory"
@Component
class InventoryEventListener {

    @ApplicationModuleListener   // annotazione chiave di Modulith
    void on(OrderPlaced event) {
        // decrementa lo stock
    }
}
```

`@ApplicationModuleListener` offre, rispetto al semplice `@EventListener`:
1. Esecuzione asincrona di default (thread pool separato).
2. Esecuzione transazionale-safe: consegna dopo il commit (`AFTER_COMMIT`).
3. Persistenza dell'evento (Event Publication Registry).

### Event Publication Registry

Ogni evento pubblicato viene salvato in una tabella (`EVENT_PUBLICATION`) prima della consegna. Se il listener fallisce, l'evento resta "non completato" e viene ri-consegnato al riavvio o tramite retry. Garanzia "at-least-once delivery" senza message broker, con `spring-modulith-events-jdbc` o `-mongodb`.

```sql
CREATE TABLE event_publication (
    id UUID,
    listener_id VARCHAR,
    event_type VARCHAR,
    serialized_event VARCHAR,
    publication_date TIMESTAMP,
    completion_date TIMESTAMP  -- NULL finché non processato con successo
);
```

### Percorso verso l'estrazione

- **Opzione A — Sostituzione del trasporto**: aggiungendo `spring-modulith-events-kafka` (o AMQP), gli stessi eventi `@ApplicationModuleListener` vengono serializzati e pubblicati su Kafka automaticamente, senza modificare il codice applicativo.
- **Opzione B — Named Interfaces**: API esplicite tra moduli per chiamate sincrone dirette, utile se serve anche comunicazione sincrona interna oltre agli eventi.

### Testing dei moduli in isolamento

```java
@ApplicationModuleTest
class OrderModuleTest {
    // avvia solo il modulo "order", non l'intera applicazione
}
```

### Tabella riepilogativa

| Esigenza | Cosa dà Spring Modulith |
|---|---|
| Confini modulari rispettati | Package `internal/` + `verify()` in CI |
| Comunicazione asincrona tra moduli | `@ApplicationModuleListener` (async, transazionale) |
| Garanzia di consegna senza broker | Event Publication Registry su DB |
| Path verso l'estrazione | Cambio driver evento (in-memory → Kafka/RabbitMQ) senza toccare il dominio |
| Documentazione architetturale | Generazione automatica diagrammi moduli |
| Test isolati per modulo | `@ApplicationModuleTest` |

---

## 4. Implementazioni diverse per country nello stesso codice sorgente

L'API pubblica del modulo (es. `OrderService`) resta unica; le implementazioni diverse restano dentro `internal/`.

```
order/
├── OrderService.java              (interfaccia pubblica)
├── OrderPlaced.java               (evento pubblico)
└── internal/
    ├── OrderServiceItImpl.java    (implementazione Italia)
    ├── OrderServiceDeImpl.java    (implementazione Germania)
    └── OrderServiceConfig.java
```

### Livello 1 — Strategy pattern + Spring Profiles

Adatto quando ogni deploy dell'app serve un solo paese.

```java
public interface OrderService {
    Order placeOrder(OrderRequest request);
}

@Service
@Profile("country-it")
class OrderServiceItImpl implements OrderService { /* IVA italiana, ecc. */ }

@Service
@Profile("country-de")
class OrderServiceDeImpl implements OrderService { /* USt tedesca, ecc. */ }
```

```properties
# application-it.properties
spring.profiles.active=country-it
```

### Livello 2 — Strategy dinamica a runtime

Per quando la stessa istanza deve gestire più country contemporaneamente (multi-tenant):

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
    private final List<OrderStrategy> strategies; // Spring inietta tutte le implementazioni

    public Order placeOrder(OrderRequest request) {
        return strategies.stream()
            .filter(s -> s.supports(request.getCountry()))
            .findFirst()
            .orElseThrow(() -> new UnsupportedCountryException(request.getCountry()))
            .placeOrder(request);
    }
}
```

### Livello 3 — Configurazione per parametri/regole minime

Se cambiano solo valori (aliquota IVA, formato indirizzo, valuta), non serve una classe diversa:

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

### Test di modulo con profili

```java
@ApplicationModuleTest
@ActiveProfiles("country-it")
class OrderModuleItTest {
    // testa solo il comportamento IT
}
```

### Riepilogo scelta

| Scenario | Approccio |
|---|---|
| Un deploy per country, differenze sostanziali di logica | Spring Profiles + implementazioni separate |
| Un deploy multi-country, differenze sostanziali di logica | Strategy pattern a runtime |
| Stessa logica, cambiano solo parametri/regole | `@ConfigurationProperties` per country |
| Differenze così grandi da meritare deploy indipendenti | Estrazione come modulo/servizio separato |

---

## 5. Gestione dei dati country-specific

### Livello 0 — Da evitare: tabella unica con colonne per country

Tabella con colonne prefissate per country (`it_codice_fiscale`, `de_ust_id`, ecc.): funziona con pochi country, diventa illeggibile e accoppiata con 5+.

### Livello 1 — Core comune + estensione per country (composizione)

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

L'API pubblica del modulo espone solo il dominio object (`Order`), mai le entity JPA specifiche. Ogni country aggiunge la propria tabella senza toccare le altre; se un country viene estratto, la sua tabella va con lui.

### Livello 2 — Gerarchia di dominio tipizzata (JPA Inheritance)

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

`JOINED` genera una tabella per la classe base e una per sottoclasse; `SINGLE_TABLE` è più leggero (una tabella, colonne nullable) ma torna verso il problema del Livello 0.

### Livello 3 — Colonna JSON per dati country-specific

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

Nessuna migration per aggiungere campi/country, ma si perdono vincoli DB e le query sui campi JSON sono meno efficienti.

### Collegamento con Strategy pattern

```java
public interface OrderStrategy {
    boolean supports(Country country);
    Order placeOrder(OrderRequest request);
    OrderPersistenceHandler persistenceHandler();
}
```

Aggiungere un country (nuova strategy + nuova tabella/estensione) è solo un'aggiunta, mai una modifica (Open/Closed Principle).

### Regola ferrea

Le entity JPA specifiche per country restano in `internal/`, mai esposte fuori dal modulo. Chi consuma il modulo vede solo il dominio object pubblico. Cruciale per l'estrazione: la tabella/estensione migra col country senza che altri moduli abbiano mai fatto query dirette su di essa.

### Riepilogo scelta

| Scenario | Approccio dati |
|---|---|
| 2-3 country, pochi campi extra, stabili | Core + tabelle di estensione 1:1 |
| Type-safety forte e query polimorfiche | JPA Inheritance JOINED |
| Country/campi che cambiano spesso | Colonna JSONB con mapping tipizzato |
| Reportistica SQL pesante sui campi extra | Evitare JSON, preferire tabelle relazionali |

Consiglio pratico: migration del core e delle estensioni country in changeset Flyway/Liquibase separati per country.

---

## 6. Modulo Spring Modulith vs modulo Java (JPMS)

Sono due concetti indipendenti:

- **Spring Modulith**: convenzione di package, verifica a runtime/test-time via analisi bytecode (ArchUnit), non richiede JPMS.
- **JPMS**: meccanismo di linguaggio/JVM (`module-info.java`), controllo a compile-time e runtime dal class loader.

### Perché combinarli è raro in pratica

1. Spring si basa su reflection e classpath scanning; JPMS con la sua strong encapsulation richiede configurazioni aggiuntive (`opens`, `exports to`) — fonte storica di attrito.
2. Spring Modulith lavora a livello di package dentro lo stesso modulo JPMS/artefatto; separare ogni modulo applicativo in JPMS distinti richiederebbe anche artefatti Maven/Gradle distinti — salto di complessità notevole in fase monolitica.

### Approccio pragmatico consigliato

- **Confini logici** → Spring Modulith (package convention + `verify()` in CI)
- **Confini fisici/di build** → moduli Maven/Gradle separati (multi-module project): compilazione separata, dipendenze esplicite, possibilità di pubblicare come artefatto versionato
- **JPMS** → generalmente evitato salvo esigenze specifiche (jlink, compliance stringente sull'incapsulamento)

```
mia-app/
├── app-order/
│   ├── pom.xml
│   └── src/main/java/com/app/order/...
├── app-inventory/
├── app-shipping/
└── app-bootstrap/        ← ha la @SpringBootApplication
    └── pom.xml
```

Dentro ogni modulo Maven si applica comunque la convenzione `internal/` di Spring Modulith; il confine "duro" lo dà Maven tramite dipendenze dichiarate.

### Tabella riepilogativa

| Livello di confine | Meccanismo | Quando usarlo |
|---|---|---|
| Logico, stesso deployable | Spring Modulith (`internal/`) | Sempre, caso base |
| Fisico, di build | Moduli Maven/Gradle separati | Consigliato appena i moduli si stabilizzano, propedeutico all'estrazione |
| JPMS | `module-info.java` | Solo con esigenze specifiche (jlink, compliance) |

---

## 7. Modulo Spring Modulith come Spring Boot Starter riusabile

Due obiettivi diversi da non confondere:
- **Estrazione**: un modulo che oggi vive nel monolite, domani diventa un servizio a parte.
- **Riuso**: un modulo condiviso, così com'è, tra più applicazioni diverse.

### Meccanica

Uno starter è un artefatto Maven/Gradle con auto-configurazione (`AutoConfiguration.imports`) che registra bean nel contesto Spring ospitante — ortogonale a Spring Modulith.

### Dichiarazione esplicita del modulo

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Audit Module"
)
package com.miaazienda.auditstarter;

import org.springframework.modulith.ApplicationModule;
```

### Struttura tipica

```
audit-spring-boot-starter/
├── pom.xml
└── src/main/java/com/miaazienda/auditstarter/
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

### Comunicazione via eventi

Una volta incluso lo starter, tutto gira nello stesso contesto Spring: i suoi `@ApplicationModuleListener` ricevono eventi pubblicati dai moduli dell'app ospitante e viceversa, senza configurazione di trasporto — stesso `ApplicationEventPublisher` condiviso. Utile per moduli cross-cutting (audit, notifiche, multi-tenancy, feature flag, i18n).

```java
@ApplicationModuleListener
void on(OrderPlaced event) {
    auditService.record("Order placed: " + event.orderId());
}
```

### Caveat sulla verify()

Il comportamento esatto di `ApplicationModules.verify()` per moduli in package esterni al root dell'app dipende dalla versione di Spring Modulith (area in evoluzione). Molti team trattano lo starter come "modulo aperto" (allowed dependency verso tutti) piuttosto che sottoporlo alla stessa verifica stretta dei moduli di dominio.

### Non confondere i due obiettivi

| Tipo di modulo | Pattern consigliato |
|---|---|
| Modulo di dominio (order, inventory) — candidato a estrazione futura | Package interno + Spring Modulith `internal/`, moduli Maven separati |
| Modulo trasversale/tecnico riusabile tra prodotti diversi | Spring Boot starter versionato a parte, eventualmente con `@ApplicationModule` |

Se un modulo deve essere sia estraibile che riusabile: **starter per la logica core condivisa** + **modulo Modulith locale in ogni app che la estende/configura** (Strategy pattern applicato a livello di differenze di prodotto).

---

## 8. Shared Kernel: condividere classi come `Order` tra app e starter

Problema noto in DDD: lo **Shared Kernel** introduce accoppiamento tra bounded context. Risolverlo bene ora prepara all'estrazione futura in microservizi (stesso problema si presenterebbe con servizi separati).

### Principio chiave: condividere il contratto, non l'aggregate

Non mettere nella libreria condivisa l'intera classe `Order` (con JPA, logica di business, invarianti). Condividere solo la porzione minima necessaria — tipicamente l'evento, non l'aggregate.

```
order-contracts/                    ← libreria condivisa, MINIMALE
└── src/main/java/com/app/order/contracts/
    ├── OrderPlaced.java            ← record immutabile
    ├── OrderCancelled.java
    └── OrderId.java                ← value object tipizzato
```

```java
// order-contracts — niente JPA, niente logica, solo dati
public record OrderPlaced(
    OrderId orderId,
    String countryCode,
    BigDecimal amount,
    Instant placedAt
) {}
```

Il modulo `order` dell'app mantiene le sue entity JPA completamente interne:

```
app/order/
├── internal/
│   ├── OrderEntity.java           ← JPA, resta SOLO qui
│   ├── OrderEntityIt.java
│   └── OrderRepository.java
└── OrderService.java              ← usa order-contracts per gli eventi
```

Grafo delle dipendenze:
```
audit-starter    → dipende da → order-contracts
app (order)      → dipende da → order-contracts
order-contracts  → non dipende da nessuno dei due
```

Nodo a parte nel grafo, evita cicli e accoppiamento a due vie.

### Perché riduce la complessità

La libreria contratti è:
- **Piccola**: solo record/DTO immutabili, zero logica, dipendenze minime (giusto Jackson se serve serializzazione).
- **Stabile**: cambia raramente, rappresenta un contratto pubblico.
- **Versionata semanticamente**: breaking change = major bump, come un'API REST o schema Avro/Protobuf.

### Interface Segregation: eventi mirati

Evitare un unico DTO "fatto per tutti" con decine di campi. Preferire eventi specifici per caso d'uso:

```java
// per audit
public record OrderPlaced(OrderId orderId, String countryCode, Instant placedAt) {}

// per loyalty
public record OrderCompleted(OrderId orderId, BigDecimal amount, CustomerId customerId) {}
```

Minimizza il "blast radius" dei cambiamenti.

### Alternativa più leggera: notifiche generiche

Se lo starter deve solo sapere che "qualcosa è successo" (es. invalidare una cache), si possono passare solo identificatori primitivi:

```java
public record DomainEventOccurred(String eventType, String aggregateId, Instant occurredAt) {}
```

Contratto più generico e stabile, a costo di type-safety e possibile chiamata di richiamo per i dettagli. Utile solo se lo starter è genuinamente agnostico rispetto al dominio.

### Collegamento con l'estrazione futura

Quando `order` viene estratto come servizio, `order-contracts` diventa lo schema degli eventi pubblicati su Kafka/RabbitMQ (eventualmente generato da Avro/JSON Schema). Il lavoro di separare contratto e implementazione, fatto ora per lo starter, è lo stesso prerequisito richiesto dall'estrazione.

### Riepilogo

| Cosa NON condividere | Cosa condividere |
|---|---|
| Entity JPA (`OrderEntity`) | Eventi/DTO immutabili (`OrderPlaced`) |
| Logica di business (`OrderService` impl) | Value object minimi (`OrderId`) |
| Repository, query | Interfacce di contratto, se serve invocazione sincrona |

| Principio | Perché |
|---|---|
| Libreria contratti separata, senza dipendenze da app/starter | Evita cicli, isola l'evoluzione |
| Contratti piccoli e specifici per consumer | Riduce blast radius dei cambiamenti |
| Versionamento semantico rigoroso | Breaking change gestiti come API pubblica |
| Zero JPA/Spring nella libreria contratti | Resta riusabile anche fuori da questo contesto Spring |

Nota pratica: se il numero di moduli/starter cresce, valutare uno schema registry anche per il caso in-process (es. generare classi Java da JSON Schema con plugin Maven/Gradle), utile soprattutto se l'estrazione futura dovrà interagire con servizi non-Java. Per iniziare, scrivere i record a mano in `order-contracts` è sufficiente.
