package com.example.analytics.internal;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.example.analytics.PriceStatistics;
import com.example.analytics.StatisticType;

/**
 * Cache in memoria dei risultati di calcolo già effettuati, svuotata da
 * {@link MarketDataChangeListener} quando il modulo Market Data pubblica un
 * evento {@code MarketDataRefreshed}.
 *
 * <p>Tenuta come bean a sé stante (anziché come campo privato di
 * {@link AnalyticsCalculationService}) in modo che sia condivisibile con il
 * listener senza dipendere dal tipo concreto di {@code AnalyticsCalculationService},
 * che è package-private e non implementa alcuna interfaccia pubblica.
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
