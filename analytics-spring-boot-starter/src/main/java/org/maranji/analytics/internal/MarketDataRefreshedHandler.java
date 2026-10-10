package org.maranji.analytics.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.maranji.actionrouter.InfoHandler;
import org.maranji.marketdata.actions.MarketDataRefreshed;

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
