package com.example.analytics.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione dell'adapter HTTP verso un servizio Analytics esterno.
 *
 * <pre>{@code
 * analytics:
 *   remote:
 *     base-url: http://analytics-service:8080
 * }</pre>
 */
@ConfigurationProperties(prefix = "analytics.remote")
class RemoteAnalyticsProperties {

    /** URL base del servizio Analytics quando gira come processo indipendente. */
    private String baseUrl = "http://localhost:8080";

    String getBaseUrl() {
        return baseUrl;
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
