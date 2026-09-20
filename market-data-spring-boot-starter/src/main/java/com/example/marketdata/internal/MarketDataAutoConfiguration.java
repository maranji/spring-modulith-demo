package com.example.marketdata.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.marketdata.FindPricesQuery;
import com.example.messaging.MessageBus;

/**
 * Auto-configurazione dello starter "market-data". Basta aggiungere la
 * dipendenza dal modulo al classpath di un'applicazione Spring Boot
 * (insieme a {@code messaging-spring-boot-starter}, per avere un
 * {@link MessageBus}) per ottenere i tre handler pienamente funzionanti,
 * senza ulteriore configurazione Java (solo, opzionalmente, le proprietà
 * {@code market-data.*} in application.yml).
 *
 * <p>Non serve dichiarare un bean {@link ResourcePatternResolver}: ogni
 * {@code ApplicationContext} di Spring implementa già questa interfaccia ed
 * è iniettabile direttamente.
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
                                     ApplicationEventPublisher events) {
        return new MarketDataStore(resourceResolver, objectMapper, properties, events);
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
