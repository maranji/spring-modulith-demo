package org.maranji.analytics.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.maranji.analytics.contracts.InsufficientDataException;
import org.maranji.analytics.contracts.PriceStatistics;
import org.maranji.analytics.contracts.StatisticType;
import org.maranji.contractbus.ContractBus;
import org.maranji.marketdata.contracts.FindPricesQuery;
import org.maranji.marketdata.contracts.PricePoint;

class AnalyticsCalculationService {

    private final ContractBus contractBus;
    private final AnalyticsCache cache;

    AnalyticsCalculationService(ContractBus contractBus, AnalyticsCache cache) {
        this.contractBus = contractBus;
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
        List<PricePoint> pricePoints = contractBus.send(new FindPricesQuery(asset, from, to));
        List<BigDecimal> prices = pricePoints.stream().map(PricePoint::price).toList();

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
