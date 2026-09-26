package com.example.analytics.internal;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.example.analytics.PriceStatistics;
import com.example.analytics.StatisticType;

/**
 * A separate bean, not a field of {@link AnalyticsCalculationService}, so the
 * listener can share it without depending on the package-private service.
 */
class AnalyticsCache {

    private final ConcurrentMap<CacheKey, PriceStatistics> entries = new ConcurrentHashMap<>();

    PriceStatistics computeIfAbsent(String asset, LocalDate from, LocalDate to, StatisticType type,
                                     Function<CacheKey, PriceStatistics> compute) {
        return entries.computeIfAbsent(new CacheKey(asset, from, to, type), compute);
    }

    void clear() {
        entries.clear();
    }

    record CacheKey(String asset, LocalDate from, LocalDate to, StatisticType type) {
    }
}
