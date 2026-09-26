package com.example.marketdata.internal;

import java.util.Set;

import com.example.contractbus.ContractHandler;
import com.example.marketdata.AvailableAssetsQuery;

class AvailableAssetsQueryHandler implements ContractHandler<AvailableAssetsQuery, Set<String>> {

    private final MarketDataStore store;

    AvailableAssetsQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public Set<String> handle(AvailableAssetsQuery query) {
        return store.availableAssets();
    }
}
