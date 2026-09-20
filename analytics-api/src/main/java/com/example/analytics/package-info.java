/**
 * Contratto pubblico del modulo "Analytics": i messaggi (richiesta/risposta)
 * per calcolare statistiche sul prezzo di un asset — {@link AverageQuery},
 * {@link StandardDeviationQuery} — più {@link PriceStatistics} e le
 * eccezioni di dominio.
 *
 * <p>Come {@code com.example.marketdata} (vedi il relativo modulo
 * {@code market-data-api}), questo modulo Gradle contiene solo il
 * contratto (l'unica dipendenza è {@code messaging-api}, altrettanto
 * neutrale): chi invia questi messaggi (es. {@code app}, attraverso il
 * {@code MessageBus}) non sa, e non deve sapere, se sono gestiti
 * in-process ({@code analytics-spring-boot-starter}) o da un servizio HTTP
 * esterno ({@code analytics-restclient-starter}) — nessuna interfaccia di
 * servizio è parte di questo contratto.
 */
package com.example.analytics;
