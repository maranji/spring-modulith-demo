package com.example.analytics;

import java.time.LocalDate;

public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String asset, LocalDate from, LocalDate to, StatisticType type) {
        super("Not enough price data for asset '%s' between %s and %s to compute %s"
                .formatted(asset, from, to, type));
    }
}
