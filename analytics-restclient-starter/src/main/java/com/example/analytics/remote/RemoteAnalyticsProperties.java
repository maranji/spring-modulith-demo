package com.example.analytics.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.remote")
class RemoteAnalyticsProperties {

    private String baseUrl = "http://localhost:8080";

    String getBaseUrl() {
        return baseUrl;
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
