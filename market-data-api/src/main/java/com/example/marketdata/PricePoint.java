package com.example.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Il prezzo di chiusura di un asset in una data specifica.
 *
 * @param date  la data di quotazione
 * @param price il prezzo di chiusura in quella data
 */
public record PricePoint(LocalDate date, BigDecimal price) {

    public PricePoint {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (price == null) {
            throw new IllegalArgumentException("price must not be null");
        }
    }
}
