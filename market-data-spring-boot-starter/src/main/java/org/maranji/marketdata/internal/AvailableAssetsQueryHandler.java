package org.maranji.marketdata.internal;

import java.util.Set;

import org.maranji.contractbus.ContractHandler;
import org.maranji.marketdata.contracts.AvailableAssetsQuery;

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
