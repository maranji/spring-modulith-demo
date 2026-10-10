package org.maranji.marketdata.internal;

import org.maranji.actionrouter.ActionHandler;
import org.maranji.marketdata.actions.RefreshMarketData;

class RefreshMarketDataCommandHandler implements ActionHandler<RefreshMarketData, Void> {

    private final MarketDataStore store;

    RefreshMarketDataCommandHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public Void handle(RefreshMarketData command) {
        store.refresh();
        return null;
    }
}
