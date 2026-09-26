package com.example.analytics.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import com.example.analytics.AverageQuery;
import com.example.contractbus.ContractBus;

/**
 * Must not be combined with {@code analytics-spring-boot-starter}: both register
 * handlers for the same contracts and the bus would fail at startup.
 */
@AutoConfiguration
@ConditionalOnClass({AverageQuery.class, ContractBus.class})
@EnableConfigurationProperties(RemoteAnalyticsProperties.class)
public class AnalyticsRestClientAutoConfiguration {

    @Bean
    RemoteAnalyticsClient remoteAnalyticsClient(RemoteAnalyticsProperties properties,
                                                 RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        return new RemoteAnalyticsClient(restClient);
    }

    @Bean
    RemoteAverageQueryHandler remoteAverageQueryHandler(RemoteAnalyticsClient client) {
        return new RemoteAverageQueryHandler(client);
    }

    @Bean
    RemoteStandardDeviationQueryHandler remoteStandardDeviationQueryHandler(RemoteAnalyticsClient client) {
        return new RemoteStandardDeviationQueryHandler(client);
    }
}
