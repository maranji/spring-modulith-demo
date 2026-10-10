package org.maranji.marketdata.internal;

import java.util.Set;

import org.maranji.actionrouter.ActionHandler;
import org.maranji.marketdata.actions.AvailableAssetsQuery;

class AvailableAssetsQueryHandler implements ActionHandler<AvailableAssetsQuery, Set<String>> {

    private final MarketDataStore store;

    AvailableAssetsQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public Set<String> handle(AvailableAssetsQuery query) {
        return store.availableAssets();
    }
}
