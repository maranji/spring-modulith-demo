package com.example.analytics;

import java.time.LocalDate;

/**
 * @param asset normalized to uppercase
 * @param from  inclusive
 * @param to    inclusive
 */
public record PriceStatistics(String asset, LocalDate from, LocalDate to, StatisticType type,
                              double value, int sampleSize) {
}
