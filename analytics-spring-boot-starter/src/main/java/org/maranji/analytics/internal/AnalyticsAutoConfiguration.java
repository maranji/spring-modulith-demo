package org.maranji.analytics.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import org.maranji.analytics.actions.AverageQuery;
import org.maranji.actionrouter.ActionRouter;

@AutoConfiguration
@ConditionalOnClass({AverageQuery.class, ActionRouter.class})
public class AnalyticsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AnalyticsCache analyticsCache() {
        return new AnalyticsCache();
    }

    @Bean
    @ConditionalOnMissingBean
    AnalyticsCalculationService analyticsCalculationService(ActionRouter actionRouter, AnalyticsCache cache) {
        return new AnalyticsCalculationService(actionRouter, cache);
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
