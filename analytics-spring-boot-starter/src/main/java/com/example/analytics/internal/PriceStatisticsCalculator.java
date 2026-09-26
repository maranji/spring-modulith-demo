package com.example.analytics.internal;

import java.math.BigDecimal;
import java.util.List;

final class PriceStatisticsCalculator {

    private PriceStatisticsCalculator() {
    }

    static double average(List<BigDecimal> prices) {
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute an average with zero prices");
        }
        return prices.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElseThrow();
    }

    /** Sample standard deviation (n - 1 divisor): observed prices are a sample, not the whole population. */
    static double standardDeviation(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            throw new IllegalArgumentException("Cannot compute a standard deviation with fewer than two prices");
        }
        double mean = average(prices);
        double sumOfSquaredDiffs = prices.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .map(price -> (price - mean) * (price - mean))
                .sum();
        return Math.sqrt(sumOfSquaredDiffs / (prices.size() - 1));
    }
}
