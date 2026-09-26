package com.example.analytics.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;

import com.example.marketdata.MarketDataRefreshed;

/**
 * A separate class with no interfaces: {@code @ApplicationModuleListener} is
 * {@code @Async}, so Spring proxies this class with CGLIB. Keeping it apart from
 * the handlers (which implement {@code ContractHandler}) avoids mixing JDK and CGLIB proxies.
 */
class MarketDataChangeListener {

    private static final Logger log = LoggerFactory.getLogger(MarketDataChangeListener.class);

    private final AnalyticsCache cache;

    MarketDataChangeListener(AnalyticsCache cache) {
        this.cache = cache;
    }

    @ApplicationModuleListener
    void on(MarketDataRefreshed event) {
        log.info("Market data refreshed for {} asset(s); invalidating analytics cache", event.assetSymbols().size());
        cache.clear();
    }
}
