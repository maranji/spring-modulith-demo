package com.example.analytics.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import com.example.analytics.AverageQuery;
import com.example.messaging.MessageBus;

/**
 * Auto-configurazione dello starter "analytics" (implementazione
 * in-process): richiede solo un {@link MessageBus} nel contesto (fornito
 * da {@code messaging-spring-boot-starter}) — non richiede più, a
 * compile-time, che il modulo Market Data sia quello locale: chiunque
 * gestisca {@code FindPricesQuery} andrà bene.
 *
 * <p>{@code @EnableAsync} è necessaria perché {@code @ApplicationModuleListener}
 * (usato da {@link MarketDataChangeListener}) è meta-annotata con
 * {@code @Async}: l'host non deve doverci pensare, lo starter la abilita da sé.
 */
@AutoConfiguration
@EnableAsync
@ConditionalOnClass({AverageQuery.class, MessageBus.class})
public class AnalyticsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AnalyticsCache analyticsCache() {
        return new AnalyticsCache();
    }

    @Bean
    @ConditionalOnMissingBean
    AnalyticsCalculationService analyticsCalculationService(MessageBus messageBus, AnalyticsCache cache) {
        return new AnalyticsCalculationService(messageBus, cache);
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
    MarketDataChangeListener marketDataChangeListener(AnalyticsCache cache) {
        return new MarketDataChangeListener(cache);
    }
}
