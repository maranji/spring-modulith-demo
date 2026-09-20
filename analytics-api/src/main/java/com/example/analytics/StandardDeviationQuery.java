package com.example.analytics;

import java.time.LocalDate;

import com.example.messaging.Message;

/**
 * Richiede la deviazione standard campionaria del prezzo di un asset nel
 * periodo indicato.
 *
 * @throws com.example.marketdata.AssetNotFoundException (sollevata dall'handler) se l'asset non esiste
 * @throws InsufficientDataException (sollevata dall'handler) se ci sono meno di due quotazioni nel periodo indicato
 */
public record StandardDeviationQuery(String asset, LocalDate from, LocalDate to) implements Message<PriceStatistics> {
}
