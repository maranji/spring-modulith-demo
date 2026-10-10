package org.maranji.marketdata.internal;

import java.util.Set;

import org.maranji.actionrouter.ActionHandler;
import org.maranji.marketdata.actions.AvailableAssets;

class AvailableAssetsQueryHandler implements ActionHandler<AvailableAssets, Set<String>> {

    private final MarketDataStore store;

    AvailableAssetsQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public Set<String> handle(AvailableAssets query) {
        return store.availableAssets();
    }
}
