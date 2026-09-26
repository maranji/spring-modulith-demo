# Spring Modulith POC — Market Data & Analytics

POC che dimostra un modular monolith con **Spring Modulith**, gestito con
**Gradle multi-modulo**, in cui:

1. ogni modulo di dominio è impacchettato come **Spring Boot starter** con auto-configuration;
2. il **contratto** di ogni modulo (i contratti di richiesta/risposta) è separato dalla sua implementazione;
3. **`app` non importa alcuna interfaccia di servizio dei moduli di dominio** — comunica con loro solo attraverso un **contract bus** in-process, inviando contratti che sono pura data. Questo rende un modulo davvero rimuovibile come dipendenza e sostituibile con un adapter verso un servizio esterno, senza toccare `app`.

## 1. Struttura

```
spring-modulith-demo/
├── base-contract/                      ← marcatore Contract<R> (zero dipendenze)
├── contract-bus/                       ← CONTRATTO del trasporto
├── contract-bus-spring-boot-starter/   ← contract bus in-process
├── market-data-contract/               ← CONTRATTO Market Data: contratti + dati
├── market-data-spring-boot-starter/    ← implementazione in-process (file JSON su disco)
├── analytics-contract/                 ← CONTRATTO Analytics: contratti + dati
├── analytics-spring-boot-starter/      ← implementazione in-process (calcolo + cache)
├── analytics-restclient-starter/       ← implementazione ALTERNATIVA: client HTTP verso un servizio esterno
└── app/                                ← applicazione Spring Boot sempre attiva (REST)
```

| Modulo | Contiene | Dipende da |
|---|---|---|
| **base-contract** | `Contract<R>` | **nessuno** |
| **contract-bus** | `ContractHandler<C,R>`, `ContractBus` | `base-contract` |
| **contract-bus-spring-boot-starter** | `SimpleContractBus`: raccoglie tutti i `ContractHandler` e smista per tipo | `contract-bus` |
| **market-data-contract** | `FindPricesQuery`, `AvailableAssetsQuery`, `RefreshMarketDataCommand`, `PricePoint`, `MarketDataRefreshed`, `AssetNotFoundException` | `base-contract` |
| **market-data-spring-boot-starter** | `MarketDataStore` + 3 `ContractHandler` (uno per contratto) | `market-data-contract` |
| **analytics-contract** | `AverageQuery`, `StandardDeviationQuery`, `PriceStatistics`, `StatisticType`, `InsufficientDataException` | `base-contract` (nemmeno `market-data-contract`) |
| **analytics-spring-boot-starter** | `AnalyticsCalculationService` + 2 `ContractHandler` | `analytics-contract`, `market-data-contract` (solo per i tipi `FindPricesQuery`/`PricePoint`) |
| **analytics-restclient-starter** | Chiama un servizio Analytics esterno via HTTP + 2 `ContractHandler` alternativi | `analytics-contract`, `market-data-contract` (solo per l'eccezione) |
| **app** | Controller REST, gestione errori | `contract-bus`, `market-data-contract`, `analytics-contract` **+** un'implementazione a scelta per ciascun modulo |

## 2. Il punto centrale: niente interfacce di servizio in `app`

Prima di questa iterazione, `app` chiamava `AnalyticsService.average(...)` — un'interfaccia Java, cioè un contratto di **servizio** (un "verbo"). Anche se l'implementazione era ben incapsulata in `internal`, `app` doveva comunque importare `com.example.analytics.AnalyticsService`: un accoppiamento diretto al *modo* in cui la funzionalità viene invocata, non solo ai dati scambiati.

Ora `app` invia **contratti** (dati) a un **bus generico**:

```java
// PriceStatisticsController — non importa com.example.analytics.AnalyticsService
private final ContractBus contractBus;

@GetMapping("/average")
PriceStatistics average(@PathVariable String symbol, @RequestParam LocalDate from, @RequestParam LocalDate to) {
    return contractBus.send(new AverageQuery(symbol, from, to));
}
```

`AverageQuery` e `PriceStatistics` sono **le classi che compongono richiesta e risposta** — esattamente "l'unica informazione che serve condividere" di cui parlavi. `ContractBus` è generico, non sa nulla di Analytics: è definito in `contract-bus`, un modulo neutro che non appartiene a nessun dominio.

### Come funziona il bus (`contract-bus-spring-boot-starter`)

```java
public interface Contract<R> {}   // in base-contract
public interface ContractHandler<C extends Contract<R>, R> { R handle(C contract); }
public interface ContractBus { <R> R send(Contract<R> contract); }

public record AverageQuery(String asset, LocalDate from, LocalDate to) implements Contract<PriceStatistics> {}
```

`SimpleContractBus`, all'avvio, riceve **tutti** i bean `ContractHandler` presenti nel contesto Spring (qualunque modulo/starter li abbia registrati) e li indicizza per il tipo di contratto che dichiarano di gestire (letto via reflection dal generico, con `ResolvableType` di Spring). `send(contract)` cerca l'handler per `contract.getClass()` e lo invoca. Non c'è alcun riferimento, da nessuna parte in questo meccanismo, a un'interfaccia "AnalyticsService" o "MarketDataService": il bus instrada per **tipo di contratto**, non per servizio.

Questa è l'astrazione di trasporto descritta nel documento di riferimento (§2): oggi è in-process e sincrona; l'interfaccia `ContractBus` è la stessa se domani la si volesse implementare inoltrando il contratto su HTTP o su una coda, senza cambiare i chiamanti.

### Chi gestisce ogni contratto, lo decide solo il `build.gradle`

```gradle
// app/build.gradle
if (analyticsMode == 'remote') {
    implementation project(':analytics-restclient-starter')   // gli handler chiamano un servizio esterno via HTTP
} else {
    implementation project(':analytics-spring-boot-starter')  // gli handler calcolano in-process
}
```

```bash
./gradlew :app:bootRun                          # Analytics in-process (default)
./gradlew :app:bootRun -PanalyticsMode=remote   # Analytics come servizio esterno
```

**Zero righe di `com.example.app` cambiano** passando da locale a remoto: i controller inviano sempre lo stesso `AverageQuery`/`StandardDeviationQuery` allo stesso `ContractBus`; cambia solo quale modulo, tra quelli sul classpath, ha registrato l'handler per quel contratto.

`analytics-restclient-starter` chiama lo stesso contratto REST già esposto da `PriceStatisticsController` (`GET /api/assets/{symbol}/statistics/average|standard-deviation`) e traduce gli errori HTTP (404, 422) nelle stesse eccezioni di dominio, così il comportamento è indistinguibile per chi chiama. Il suo test (`RemoteAnalyticsClientTest`) simula quel servizio esterno con `MockRestServiceServer`, senza doverlo avviare davvero.

### E la comunicazione interna Analytics → Market Data?

Anche `AnalyticsCalculationService` (dentro `analytics-spring-boot-starter`) non chiama più un'interfaccia `MarketDataService`: ottiene i prezzi inviando un `FindPricesQuery` allo stesso `ContractBus`:

```java
List<BigDecimal> prices = contractBus.send(new FindPricesQuery(asset, from, to))
        .stream().map(PricePoint::price).toList();
```

Coerente con il principio guida del documento (§2): *"Ogni modulo comunica con gli altri solo tramite messaggi/eventi ben definiti, mai tramite chiamate dirette a metodi di altri moduli."* Analytics dipende da `market-data-contract` solo per i **tipi** `FindPricesQuery`/`PricePoint` (dati), non da un'interfaccia di servizio — che infatti non esiste più: è stata rimossa insieme ad `AnalyticsService`.

### Perché `market-data` resta locale in questa demo

Lo stesso schema (un `market-data-restclient-starter`) si applicherebbe simmetricamente a Market Data; non l'ho aggiunto solo per tenere la demo focalizzata — il contratto (`market-data-contract`) è già pronto per questo, dipendendo solo da `base-contract` e senza interfacce di servizio.

### L'evento `MarketDataRefreshed` resta un evento, non un contratto del bus

Quando market-data ricarica i dati pubblica `MarketDataRefreshed` tramite l'`ApplicationEventPublisher`/`@ApplicationModuleListener` di Spring Modulith — un meccanismo di pub/sub **diverso e complementare** al contract bus sincrono: è "fire and forget" (uno a molti, nessuno risponde), mentre il bus è "richiesta/risposta" (uno a uno, uno risponde). Ha senso tenerli distinti: mescolare i due comprometterebbe entrambi.

## 3. Nota sui confini di Spring Modulith

Come prima di questa iterazione, `base-contract`/`market-data-contract`/`analytics-contract`/`contract-bus` **non** portano l'annotazione `@org.springframework.modulith.ApplicationModule`: sono pensati per essere framework-agnostic. `ApplicationModules.verify()` funziona comunque per convenzione di package/`internal`.

`MarketDataModuleTests`/`AnalyticsModuleTests` continuano a girare nei rispettivi `-spring-boot-starter` (il loro classpath di test unisce contratto + `internal`); `ModularityTests` in `app` verifica solo il modulo "web". Le dipendenze cross-artefatto Gradle non sono coperte da nessuno dei due `verify()`; la protezione reale resta la visibilità Java (tutto sotto `internal/` è package-private).

## 4. API REST (invariata)

| Metodo | Path | Descrizione |
|---|---|---|
| `GET` | `/api/assets` | Elenco degli asset disponibili |
| `POST` | `/api/assets/refresh` | Ricarica manualmente i dati dal disco (202 Accepted) |
| `GET` | `/api/assets/{symbol}/statistics/average?from=YYYY-MM-DD&to=YYYY-MM-DD` | Media dei prezzi nel periodo |
| `GET` | `/api/assets/{symbol}/statistics/standard-deviation?from=YYYY-MM-DD&to=YYYY-MM-DD` | Deviazione standard campionaria dei prezzi nel periodo |

Errori come [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807):
`404` asset sconosciuto, `422` nessun dato nel periodo, `400` `from` successivo a `to`.

Dati di esempio: `AAPL` e `MSFT`, 11 quotazioni fittizie ciascuno (2–16 gennaio 2026), in `app/src/main/resources/data/*.json`.

## 5. Come eseguire il progetto

Il progetto è stato scritto e revisionato **staticamente** in questo
ambiente: **le policy di rete di questa sandbox bloccano sia Maven Central
sia il Gradle Plugin Portal**, quindi non è stato possibile eseguire
`gradle build`/`gradle test` né generare il Gradle Wrapper qui. Il codice
segue fedelmente le API di Spring Boot 3.4 / Spring Modulith 1.3 / Spring
Framework 6.1 (`RestClient`, `ResolvableType`), ma va compilato e testato
sulla tua macchina prima di considerarlo definitivo.

```bash
# 1. Genera il wrapper (o usa una tua installazione locale di Gradle 8.x + JDK 21)
gradle wrapper --gradle-version 8.14.3

# 2. Build completa + test (Analytics in-process, default)
./gradlew build

# 3. Avvio
./gradlew :app:bootRun
# oppure, per l'implementazione remota di Analytics (serve un servizio in
# ascolto su analytics.remote.base-url che esponga lo stesso contratto REST):
./gradlew :app:bootRun -PanalyticsMode=remote
```

Prova rapida (modalità default, locale):

```bash
curl "http://localhost:8080/api/assets"
curl "http://localhost:8080/api/assets/AAPL/statistics/average?from=2026-01-01&to=2026-01-31"
curl "http://localhost:8080/api/assets/AAPL/statistics/standard-deviation?from=2026-01-01&to=2026-01-31"
curl -X POST "http://localhost:8080/api/assets/refresh"
```

`PriceStatisticsControllerIT` assume la modalità locale (verifica i valori calcolati sui dati di esempio reali).

## 6. Test inclusi

- `SimpleContractBusTest` (contract-bus-spring-boot-starter) — il cuore del nuovo meccanismo: dispatch corretto per tipo di contratto, errore chiaro se manca l'handler.
- `MarketDataStoreTest`, `MarketDataQueryHandlersTest` — logica di market-data e delega dei tre handler.
- `PriceStatisticsCalculatorTest`, `AnalyticsCalculationServiceTest`, `AnalyticsQueryHandlersTest`, `MarketDataChangeListenerTest` — logica di analytics (ora basata su `ContractBus` mockato) e delega dei due handler.
- `RemoteAnalyticsClientTest` (analytics-restclient-starter) — chiamata HTTP e traduzione errori con `MockRestServiceServer`.
- `MarketDataModuleTests` / `AnalyticsModuleTests` / `ModularityTests` — `ApplicationModules.verify()` + documentazione architetturale.
- `PriceStatisticsControllerIT` — end-to-end via MockMvc (modalità locale).

## 7. Cose da verificare una volta compilato

1. **Versioni**: Spring Boot `3.4.1`, Spring Modulith `1.3.1`.
2. **`ResolvableType.forClass(handler.getClass()).as(ContractHandler.class).getGeneric(0).resolve()`**: è l'unico punto "magico" del progetto (risoluzione del generico a runtime). Se una futura implementazione di `ContractHandler` viene proxata da un altro aspetto AOP oltre a quelli già usati qui, verificare che `handler.getClass()` resti la classe concreta e non un proxy che perde l'informazione generica.
3. **`MockRestServiceServer.bindTo(RestClient.Builder)`**: disponibile da Spring Framework 6.1 (incluso in Boot 3.4.1).
4. **`spring.modulith.events.jdbc.schema-initialization.enabled`**: come già segnalato, se la chiave esatta differisce nella tua versione viene semplicemente ignorata — controlla nei log la creazione della tabella `EVENT_PUBLICATION`.

## 8. Estensioni naturali

- Applicare lo stesso schema a Market Data: `market-data-restclient-starter` con due `ContractHandler` che chiamano un servizio esterno, per estrarre anche quel modulo.
- Sostituire il file JSON con un vero provider di mercato: un nuovo `ContractHandler` per `FindPricesQuery` in un nuovo modulo, zero impatti su `analytics-contract`/`app`.
- Un'implementazione di `ContractBus` che inoltra su Kafka/RabbitMQ invece che in memoria — stesso principio, applicato alla comunicazione asincrona multi-processo anziché in-process.
- Se il numero di contratti cresce, un piccolo test ArchUnit in `app` che vieti `import com.example.*.internal..*` (già impossibile, essendo package-private) o, più utilmente, che vieti `import` di qualunque interfaccia con suffisso `Service` dal modulo `app`, per far fallire la build se qualcuno reintroduce l'accoppiamento che questa iterazione ha rimosso.
