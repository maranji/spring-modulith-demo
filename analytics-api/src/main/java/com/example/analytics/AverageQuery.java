package com.example.analytics;

import java.time.LocalDate;

import com.example.messaging.Message;

/**
 * Richiede la media del prezzo di un asset nel periodo indicato. Gestita
 * dal modulo Analytics tramite il {@code MessageBus}, in-process o da un
 * servizio esterno a seconda del deployment (vedi README).
 *
 * @throws com.example.marketdata.AssetNotFoundException (sollevata dall'handler) se l'asset non esiste
 * @throws InsufficientDataException (sollevata dall'handler) se non ci sono quotazioni nel periodo indicato
 */
public record AverageQuery(String asset, LocalDate from, LocalDate to) implements Message<PriceStatistics> {
}
