package com.example.marketdata.internal;

import java.util.Set;

import com.example.marketdata.AvailableAssetsQuery;
import com.example.messaging.MessageHandler;

class AvailableAssetsQueryHandler implements MessageHandler<AvailableAssetsQuery, Set<String>> {

    private final MarketDataStore store;

    AvailableAssetsQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public Set<String> handle(AvailableAssetsQuery query) {
        return store.availableAssets();
    }
}
