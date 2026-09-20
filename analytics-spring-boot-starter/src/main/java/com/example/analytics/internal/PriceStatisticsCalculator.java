package com.example.analytics.internal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Funzioni di calcolo pure sulle serie di prezzo. Nessuna dipendenza da
 * Spring o dal modulo Market Data: facilmente testabile in isolamento e,
 * in prospettiva, facilmente estraibile o riusabile altrove.
 */
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

    /**
     * Deviazione standard campionaria (divisore {@code n - 1}), la stima
     * corretta quando i prezzi osservati sono un campione e non l'intera
     * popolazione dei prezzi possibili.
     */
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
