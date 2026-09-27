# Contract Bus POC — Market Data & Analytics

POC di un modular monolith **Gradle multi-modulo** (Java 21, Spring Boot 3.4.1) in cui l'**unico** canale di comunicazione tra moduli è un **contract bus** scritto a mano. L'obiettivo è spingere questo meccanismo il più lontano possibile, per capirne a fondo i limiti.

Principi:

1. ogni modulo di dominio è impacchettato come **Spring Boot starter** con auto-configuration;
2. il **contratto** di ogni modulo (richieste, risposte, info) è separato dalla sua implementazione;
3. **`app` non importa alcuna interfaccia di servizio dei moduli di dominio**: invia contratti che sono pura data al bus. Un modulo è quindi rimuovibile come dipendenza e sostituibile con un adapter verso un servizio esterno, senza toccare `app`;
4. anche le notifiche uno-a-molti passano dal bus: niente `ApplicationEventPublisher` né altri meccanismi di Spring per far parlare i moduli.

Il documento [`docs/architettura-modular-monolith-spring-modulith.md`](docs/architettura-modular-monolith-spring-modulith.md) è il riferimento architetturale generale da cui la POC è partita (usava Spring Modulith, poi rimosso).

## 1. Struttura

```
contract-bus-demo/
├── base-contract/                      ← marcatori Contract<R> e Info (zero dipendenze)
├── contract-bus/                       ← CONTRATTO del trasporto
├── contract-bus-spring-boot-starter/   ← contract bus in-process
├── market-data-contract/               ← CONTRATTO Market Data: contratti, info, dati
├── market-data-spring-boot-starter/    ← implementazione in-process (file JSON su disco)
├── analytics-contract/                 ← CONTRATTO Analytics: contratti + dati
├── analytics-spring-boot-starter/      ← implementazione in-process (calcolo + cache)
├── analytics-restclient-starter/       ← implementazione ALTERNATIVA: client HTTP verso un servizio esterno
├── app/                                ← applicazione Spring Boot sempre attiva (REST)
└── docs/                               ← documento di riferimento architetturale
```

| Modulo | Contiene | Dipende da |
|---|---|---|
| **base-contract** | `Contract<R>`, `Info` | **nessuno** |
| **contract-bus** | `ContractBus`, `ContractHandler<C,R>`, `InfoHandler<I>` | `base-contract` |
| **contract-bus-spring-boot-starter** | `SimpleContractBus`: raccoglie tutti gli handler e smista per tipo | `contract-bus` |
| **market-data-contract** | `FindPricesQuery`, `AvailableAssetsQuery`, `RefreshMarketDataCommand`, `MarketDataRefreshed` (info), `PricePoint`, `AssetNotFoundException` | `base-contract` |
| **market-data-spring-boot-starter** | `MarketDataStore` + 3 `ContractHandler` (uno per contratto) | `contract-bus`, `market-data-contract` |
| **analytics-contract** | `AverageQuery`, `StandardDeviationQuery`, `PriceStatistics`, `StatisticType`, `InsufficientDataException` | `base-contract` (nemmeno `market-data-contract`) |
| **analytics-spring-boot-starter** | `AnalyticsCalculationService`, `AnalyticsCache` + 2 `ContractHandler` + 1 `InfoHandler` | `contract-bus`, `analytics-contract`, `market-data-contract` (solo per i tipi `FindPricesQuery`/`PricePoint`/`MarketDataRefreshed`) |
| **analytics-restclient-starter** | `RemoteAnalyticsClient` (chiama un servizio Analytics esterno via HTTP) + 2 `ContractHandler` alternativi | `contract-bus`, `analytics-contract`, `market-data-contract` (solo per l'eccezione) |
| **app** | Controller REST, gestione errori | `contract-bus`, `market-data-contract`, `analytics-contract`, `contract-bus-spring-boot-starter`, `market-data-spring-boot-starter` **+** una delle due implementazioni di Analytics |

Gli starter dichiarano `contract-bus` direttamente, non lo ricevono in modo transitivo dal proprio modulo contratto: i contratti di dominio dipendono solo da `base-contract`, mai dal bus.

## 2. Il contract bus

### Due semantiche, due tipi

```java
// base-contract
public interface Contract<R> {}   // richiesta/risposta: esattamente 1 handler
public interface Info {}          // notifica: da 0 a N handler, nessuna risposta

// contract-bus
public interface ContractBus {
    <R> R send(Contract<R> contract);
    void broadcast(Info info);
}
public interface ContractHandler<C extends Contract<R>, R> { R handle(C contract); }
public interface InfoHandler<I extends Info> { void handle(I info); }
```

| | `send(Contract<R>)` | `broadcast(Info)` |
|---|---|---|
| Handler registrati | esattamente uno (un duplicato fa fallire l'avvio) | zero, uno o molti |
| Nessun handler | `IllegalStateException` | nessun effetto |
| Risposta | `R`, dichiarato dal contratto | nessuna |
| Eccezione di un handler | risale al chiamante | solo loggata: non raggiunge il chiamante e gli handler successivi vengono chiamati comunque |

Ogni contratto dichiara il proprio tipo di risposta (`Contract<Void>` per i comandi, come `RefreshMarketDataCommand`); un handler il cui tipo di risposta non coincide con quello del contratto è un errore di compilazione. Comandi e info restano distinti di proposito: un comando deve avere il suo unico handler, e il bus deve poter segnalare quando manca; un'info può non interessare a nessuno. Il nome `Info` (e non "evento") è voluto: la POC non vuole evocare un'architettura a eventi, ma solo una notifica uno-a-molti sullo stesso bus.

### Come funziona `SimpleContractBus`

All'avvio `SimpleContractBus` riceve **tutti** i bean `ContractHandler` e `InfoHandler` presenti nel contesto Spring, qualunque starter li abbia registrati, e li indicizza per il tipo che dichiarano di gestire, letto via reflection dal generico:

```java
ResolvableType.forClass(handler.getClass()).as(ContractHandler.class).getGeneric(0).resolve()
```

È l'unico punto "magico" del progetto: un handler deve implementare `ContractHandler<SomeContract, SomeResponse>` o `InfoHandler<SomeInfo>` **direttamente**; un proxy AOP che nasconde l'informazione generica romperebbe il dispatch.

- `send(contract)` cerca l'handler per `contract.getClass()` e lo invoca; se non c'è, l'errore indica quale contratto non ha un modulo che lo gestisce.
- `broadcast(info)` invoca tutti gli handler registrati per `info.getClass()`. Se un handler lancia un'eccezione, il bus la logga e passa al successivo. In `SimpleContractBus` gli handler girano in sequenza nel thread del chiamante: è una scelta di questa implementazione, non del contratto `ContractBus`, e un'altra implementazione potrà eseguirli in modo asincrono senza cambiare i chiamanti.

Il bus instrada per **tipo**, non per servizio: non esiste da nessuna parte un'interfaccia "AnalyticsService" o "MarketDataService".

### `app` invia solo dati

```java
// PriceStatisticsController — nessuna interfaccia di servizio importata
private final ContractBus contractBus;

@GetMapping("/average")
PriceStatistics average(@PathVariable String symbol,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    validateRange(from, to);   // 400 se from > to
    return contractBus.send(new AverageQuery(symbol, from, to));
}
```

`AverageQuery` e `PriceStatistics` sono l'unica informazione che chiamante e implementazione devono condividere.

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
./gradlew :app:printAnalyticsMode               # mostra quale implementazione è configurata
```

**Zero righe di `com.example.app` cambiano** passando da locale a remoto: cambia solo quale modulo, tra quelli sul classpath, ha registrato l'handler per quel contratto. Le due implementazioni sono mutuamente esclusive: averle entrambe sul classpath registrerebbe due handler per lo stesso contratto.

`analytics-restclient-starter` chiama lo stesso contratto REST esposto da `PriceStatisticsController` (`GET /api/assets/{symbol}/statistics/average|standard-deviation`) su `analytics.remote.base-url` e traduce gli errori HTTP 404 e 422 nelle stesse eccezioni di dominio (`AssetNotFoundException`, `InsufficientDataException`). In modalità remota nessuno gestisce `MarketDataRefreshed`, e per un'info va bene così.

### Comunicazione tra moduli di dominio

Analytics ottiene i prezzi da Market Data con una richiesta sul bus:

```java
List<PricePoint> pricePoints = contractBus.send(new FindPricesQuery(asset, from, to));
```

Il refresh dei dati combina le due semantiche. La **richiesta** di refresh è un comando, quindi passa da `send` ed è gestita da uno e un solo modulo; la **notifica** che i dati sono cambiati è un'info, trasmessa a chiunque sia interessato:

```
POST /api/assets/refresh
  └─ AssetsController ──send(RefreshMarketDataCommand)──▶ RefreshMarketDataCommandHandler   (esattamente 1 handler: market-data)
                                                            └─ MarketDataStore.refresh()
                                                                 └─ broadcast(MarketDataRefreshed) ──▶ 0..N InfoHandler
                                                                                                       (oggi: MarketDataRefreshedHandler)
```

Anche il refresh programmato di `MarketDataStore` trasmette `MarketDataRefreshed`. `MarketDataRefreshedHandler` (in `analytics-spring-boot-starter`) svuota la cache delle statistiche; Market Data non sa chi, e se qualcuno, riceve l'info.

## 3. Limiti noti

Questa sezione raccoglie i limiti del contract bus man mano che emergono. È il vero oggetto di studio della POC.

1. **Trasmettere un'info durante l'inizializzazione dei bean crea un ciclo.** La prima chiamata al bus costruisce l'indice degli handler, quindi li istanzia tutti. Se il caricamento iniziale di `MarketDataStore` (`@PostConstruct`) trasmettesse `MarketDataRefreshed`, verrebbe istanziato anche `FindPricesQueryHandler`, che dipende proprio da `MarketDataStore`, ancora in creazione: l'avvio fallisce con `BeanCurrentlyInCreationException` (verificato). Per questo il caricamento iniziale non trasmette nulla: all'avvio non c'è nessuna cache da invalidare.
2. **Chi chiama `broadcast` non sa se un'info è stata gestita.** Gli errori degli handler vengono solo loggati: se l'invalidazione della cache di Analytics fallisse, Market Data non se ne accorgerebbe e le statistiche resterebbero calcolate sui dati vecchi fino al broadcast successivo andato a buon fine.
3. **Nessuna garanzia di consegna.** Le info vivono solo in memoria: se il processo termina durante il broadcast, o un handler fallisce, la notifica è persa. Non c'è persistenza né retry.
4. **Nessuna integrazione con le transazioni.** `broadcast` invoca subito gli handler: se venisse chiamato dentro una transazione poi annullata, gli handler avrebbero già reagito a qualcosa che non è mai avvenuto.
5. **Dispatch per classe esatta.** Un handler registrato per un supertipo (o un'interfaccia) non riceve i sottotipi, sia per i contratti sia per le info.
6. **Ordine degli handler di un'info.** È quello dei bean (`ObjectProvider.orderedStream()`): definito solo se gli handler usano `@Order`/`Ordered`.
7. **Handler non proxabili.** Il tipo gestito si ricava dal generico della classe concreta: un proxy che lo nasconde rompe il dispatch.
8. **Nessuna verifica automatica dei confini tra moduli.** Senza Spring Modulith (`ApplicationModules.verify()`) l'unica protezione è la visibilità Java: le classi di implementazione sono package-private, sotto `internal/` negli starter in-process e sotto `remote/` in `analytics-restclient-starter`.

## 4. API REST

| Metodo | Path | Descrizione |
|---|---|---|
| `GET` | `/api/assets` | Elenco degli asset disponibili |
| `POST` | `/api/assets/refresh` | Ricarica manualmente i dati dal disco (202 Accepted) |
| `GET` | `/api/assets/{symbol}/statistics/average?from=YYYY-MM-DD&to=YYYY-MM-DD` | Media dei prezzi nel periodo |
| `GET` | `/api/assets/{symbol}/statistics/standard-deviation?from=YYYY-MM-DD&to=YYYY-MM-DD` | Deviazione standard campionaria dei prezzi nel periodo |

Errori come [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807):
`404` asset sconosciuto, `422` nessun dato nel periodo, `400` `from` successivo a `to`.

Dati di esempio: `AAPL` e `MSFT`, 11 quotazioni fittizie ciascuno (2–16 gennaio 2026), in `app/src/main/resources/data/*.json`.

## 5. Configurazione

Le proprietà principali, in `app/src/main/resources/application.yml`:

| Proprietà | Default | Descrizione |
|---|---|---|
| `market-data.directory` | `classpath:data/` | Cartella dei file JSON (sintassi `Resource` di Spring, es. `file:/percorso/assoluto/`) |
| `market-data.refresh-interval` | `PT5M` | Intervallo del ricaricamento automatico dei dati |
| `market-data.refresh-enabled` | `true` | Abilita il ricaricamento automatico (quello manuale via `POST /api/assets/refresh` resta sempre disponibile) |
| `analytics.remote.base-url` | `http://localhost:8081` in `application.yml` (`http://localhost:8080` se la proprietà manca) | URL del servizio Analytics esterno; usata solo con `-PanalyticsMode=remote` |

## 6. Come eseguire il progetto

Serve JDK 21; il Gradle Wrapper (Gradle 8.5) è incluso.

```bash
# Build completa + test (Analytics in-process, default)
./gradlew build

# Test di un solo modulo o di una sola classe
./gradlew :analytics-spring-boot-starter:test
./gradlew test --tests '*SimpleContractBusTest'

# Avvio
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

## 7. Test inclusi

- `SimpleContractBusTest` (contract-bus-spring-boot-starter) — il cuore del meccanismo: dispatch di `send` per tipo, errore se manca l'handler o se ce ne sono due, `broadcast` verso più handler, `broadcast` senza handler, eccezione di un handler che non raggiunge il chiamante e non ferma gli handler successivi.
- `PricePointTest` (market-data-contract) — validazione del value type.
- `MarketDataStoreTest`, `MarketDataQueryHandlersTest` — logica di market-data, broadcast dell'info solo al refresh e delega dei tre handler.
- `PriceStatisticsCalculatorTest`, `AnalyticsCalculationServiceTest`, `AnalyticsQueryHandlersTest`, `MarketDataRefreshedHandlerTest` — logica di analytics (basata su `ContractBus` mockato), delega dei due handler e invalidazione della cache.
- `RemoteAnalyticsClientTest` (analytics-restclient-starter) — chiamata HTTP e traduzione errori con `MockRestServiceServer`.
- `PriceStatisticsControllerIT` — end-to-end via MockMvc (modalità locale).

## 8. Direzioni da esplorare

- Un `broadcast` asincrono (su un `Executor`): cosa si guadagna e cosa si perde (ordine, test deterministici, visibilità degli errori).
- Garanzia di consegna: un registro persistente delle info con retry, scritto sopra il bus.
- Un'implementazione di `ContractBus` che inoltra contratti e info su HTTP o su una coda (Kafka/RabbitMQ), senza cambiare i chiamanti.
- `market-data-restclient-starter`: estrarre anche Market Data e vedere cosa succede alle info quando chi le trasmette sta in un altro processo.
- Un test ArchUnit che sostituisca la verifica dei confini persa con Spring Modulith.
