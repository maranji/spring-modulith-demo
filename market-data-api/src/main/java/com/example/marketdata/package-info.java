/**
 * Contratto pubblico del modulo "Market Data": i messaggi (richiesta/
 * risposta) per interrogarlo — {@link FindPricesQuery},
 * {@link AvailableAssetsQuery}, {@link RefreshMarketDataCommand} — più i
 * record di dati ({@link PricePoint}), l'evento {@link MarketDataRefreshed}
 * e le eccezioni di dominio.
 *
 * <p>Questo modulo Gradle contiene <b>solo il contratto</b>, senza alcuna
 * dipendenza da Spring o da qualunque altro framework (l'unica dipendenza
 * è {@code messaging-api}, altrettanto neutrale). Nessuna interfaccia di
 * servizio: chi vuole i dati di un asset invia uno di questi messaggi al
 * {@code MessageBus}, senza sapere né dover sapere se a rispondere è
 * un'implementazione in-process (vedi {@code market-data-spring-boot-starter}),
 * un database o un eventuale servizio esterno.
 */
package com.example.marketdata;
