package com.example.analytics.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.contractbus.InfoHandler;
import com.example.marketdata.MarketDataRefreshed;

class MarketDataRefreshedHandler implements InfoHandler<MarketDataRefreshed> {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRefreshedHandler.class);

    private final AnalyticsCache cache;

    MarketDataRefreshedHandler(AnalyticsCache cache) {
        this.cache = cache;
    }

    @Override
    public void handle(MarketDataRefreshed info) {
        log.info("Market data refreshed for {} asset(s); invalidating analytics cache", info.assetSymbols().size());
        cache.clear();
    }
}
