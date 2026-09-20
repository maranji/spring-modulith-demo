package com.example.analytics;

import java.time.LocalDate;

/**
 * Risultato del calcolo di una statistica sul prezzo di un asset in un
 * periodo di tempo.
 *
 * @param asset      il simbolo dell'asset, normalizzato in maiuscolo
 * @param from       inizio del periodo considerato (incluso)
 * @param to         fine del periodo considerato (incluso)
 * @param type       la funzione di calcolo applicata
 * @param value      il risultato del calcolo
 * @param sampleSize il numero di quotazioni usate per il calcolo
 */
public record PriceStatistics(String asset, LocalDate from, LocalDate to, StatisticType type,
                               double value, int sampleSize) {
}
