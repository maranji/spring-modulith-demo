package com.example.analytics.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.example.analytics.InsufficientDataException;
import com.example.analytics.PriceStatistics;
import com.example.analytics.StatisticType;
import com.example.marketdata.FindPricesQuery;
import com.example.marketdata.PricePoint;
import com.example.messaging.MessageBus;

/**
 * Calcolo delle statistiche sul prezzo di un asset. Ottiene i prezzi
 * inviando un {@link FindPricesQuery} al {@link MessageBus}, non chiamando
 * un'interfaccia di servizio del modulo Market Data: non sa, e non deve
 * sapere, se quel messaggio è gestito da un'implementazione locale (file
 * su disco), da un database o da un servizio esterno.
 *
 * <p>I risultati sono mantenuti in {@link AnalyticsCache}, svuotata da
 * {@link MarketDataChangeListener} quando il modulo Market Data pubblica un
 * evento {@code MarketDataRefreshed}.
 *
 * <p>Invocata solo dagli handler di questo package ({@link AverageQueryHandler},
 * {@link StandardDeviationQueryHandler}): non implementa alcuna interfaccia
 * pubblica, per lo stesso motivo per cui Market Data non ne implementa una.
 */
class AnalyticsCalculationService {

    private final MessageBus messageBus;
    private final AnalyticsCache cache;

    AnalyticsCalculationService(MessageBus messageBus, AnalyticsCache cache) {
        this.messageBus = messageBus;
        this.cache = cache;
    }

    PriceStatistics average(String asset, LocalDate from, LocalDate to) {
        return compute(asset, from, to, StatisticType.AVERAGE);
    }

    PriceStatistics standardDeviation(String asset, LocalDate from, LocalDate to) {
        return compute(asset, from, to, StatisticType.STANDARD_DEVIATION);
    }

    private PriceStatistics compute(String asset, LocalDate from, LocalDate to, StatisticType type) {
        String normalizedAsset = asset.trim().toUpperCase(Locale.ROOT);
        return cache.computeIfAbsent(normalizedAsset, from, to, type,
                key -> doCompute(key.asset(), key.from(), key.to(), key.type()));
    }

    private PriceStatistics doCompute(String asset, LocalDate from, LocalDate to, StatisticType type) {
        List<BigDecimal> prices = messageBus.send(new FindPricesQuery(asset, from, to)).stream()
                .map(PricePoint::price)
                .toList();

        int minimumSampleSize = type == StatisticType.STANDARD_DEVIATION ? 2 : 1;
        if (prices.size() < minimumSampleSize) {
            throw new InsufficientDataException(asset, from, to, type);
        }

        double value = switch (type) {
            case AVERAGE -> PriceStatisticsCalculator.average(prices);
            case STANDARD_DEVIATION -> PriceStatisticsCalculator.standardDeviation(prices);
        };

        return new PriceStatistics(asset, from, to, type, value, prices.size());
    }
}
