package com.example.marketdata.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.contractbus.ContractBus;
import com.example.marketdata.FindPricesQuery;

/**
 * No {@link ResourcePatternResolver} bean is needed: every
 * {@code ApplicationContext} already implements it.
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(MarketDataProperties.class)
@ConditionalOnClass(FindPricesQuery.class)
public class MarketDataAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MarketDataStore marketDataStore(ResourcePatternResolver resourceResolver,
                                     ObjectMapper objectMapper,
                                     MarketDataProperties properties,
                                     ContractBus contractBus) {
        return new MarketDataStore(resourceResolver, objectMapper, properties, contractBus);
    }

    @Bean
    FindPricesQueryHandler findPricesQueryHandler(MarketDataStore store) {
        return new FindPricesQueryHandler(store);
    }

    @Bean
    AvailableAssetsQueryHandler availableAssetsQueryHandler(MarketDataStore store) {
        return new AvailableAssetsQueryHandler(store);
    }

    @Bean
    RefreshMarketDataCommandHandler refreshMarketDataCommandHandler(MarketDataStore store) {
        return new RefreshMarketDataCommandHandler(store);
    }
}
