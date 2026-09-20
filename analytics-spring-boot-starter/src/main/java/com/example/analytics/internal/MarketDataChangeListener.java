package com.example.analytics.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;

import com.example.marketdata.MarketDataRefreshed;

/**
 * Invalida la cache di Analytics ogni volta che il modulo Market Data
 * ricarica i prezzi da disco.
 *
 * <p>È una classe a sé, senza interfacce: {@code @ApplicationModuleListener}
 * comporta esecuzione asincrona (è meta-annotata con {@code @Async}), e Spring
 * applica quel comportamento creando un proxy CGLIB della classe concreta.
 * Tenendo questo listener separato dagli handler dei messaggi (che
 * implementano {@code MessageHandler}, un'interfaccia) evitiamo qualunque
 * ambiguità tra proxy JDK basati su interfaccia e proxy CGLIB basati su classe.
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
