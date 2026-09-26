package com.example.marketdata.internal;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
class MarketDataProperties {

    /** Spring Resource syntax: {@code classpath:data/} for the demo, {@code file:/absolute/path/} in production. */
    private String directory = "classpath:data/";

    private Duration refreshInterval = Duration.ofMinutes(5);

    private boolean refreshEnabled = true;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }
}
