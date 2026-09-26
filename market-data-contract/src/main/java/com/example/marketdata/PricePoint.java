package com.example.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Closing price of an asset on a given date. */
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
