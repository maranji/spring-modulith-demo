package com.example.analytics.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import com.example.analytics.AverageQuery;
import com.example.messaging.MessageBus;

/**
 * Auto-configurazione dell'adapter HTTP per il modulo Analytics: registra
 * gli handler di {@code AverageQuery}/{@code StandardDeviationQuery} che
 * delegano a un servizio esterno via HTTP, invece che calcolare in-process.
 *
 * <p><b>Non</b> va usato insieme a {@code analytics-spring-boot-starter}
 * nello stesso deployment: sono due implementazioni alternative dello
 * stesso contratto (locale vs. remota). Vedi il README per come scegliere
 * quale attivare in {@code app/build.gradle}.
 */
@AutoConfiguration
@ConditionalOnClass({AverageQuery.class, MessageBus.class})
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
