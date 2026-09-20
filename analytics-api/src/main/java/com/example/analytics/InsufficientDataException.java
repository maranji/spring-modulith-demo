package com.example.analytics;

import java.time.LocalDate;

/**
 * Sollevata quando non ci sono abbastanza quotazioni nel periodo richiesto
 * per calcolare la statistica desiderata (nessun prezzo per la media, meno
 * di due prezzi per la deviazione standard).
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String asset, LocalDate from, LocalDate to, StatisticType type) {
        super("Not enough price data for asset '%s' between %s and %s to compute %s"
                .formatted(asset, from, to, type));
    }
}
