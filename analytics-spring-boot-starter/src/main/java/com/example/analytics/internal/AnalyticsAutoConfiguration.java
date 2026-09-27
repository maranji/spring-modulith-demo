package com.example.analytics.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.example.analytics.AverageQuery;
import com.example.contractbus.ContractBus;

@AutoConfiguration
@ConditionalOnClass({AverageQuery.class, ContractBus.class})
public class AnalyticsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AnalyticsCache analyticsCache() {
        return new AnalyticsCache();
    }

    @Bean
    @ConditionalOnMissingBean
    AnalyticsCalculationService analyticsCalculationService(ContractBus contractBus, AnalyticsCache cache) {
        return new AnalyticsCalculationService(contractBus, cache);
    }

    @Bean
    AverageQueryHandler averageQueryHandler(AnalyticsCalculationService service) {
        return new AverageQueryHandler(service);
    }

    @Bean
    StandardDeviationQueryHandler standardDeviationQueryHandler(AnalyticsCalculationService service) {
        return new StandardDeviationQueryHandler(service);
    }

    @Bean
    MarketDataRefreshedHandler marketDataRefreshedHandler(AnalyticsCache cache) {
        return new MarketDataRefreshedHandler(cache);
    }
}
