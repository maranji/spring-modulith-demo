package org.maranji.marketdata.internal;

import org.maranji.actionrouter.ActionHandler;
import org.maranji.marketdata.actions.RefreshMarketDataCommand;

class RefreshMarketDataCommandHandler implements ActionHandler<RefreshMarketDataCommand, Void> {

    private final MarketDataStore store;

    RefreshMarketDataCommandHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public Void handle(RefreshMarketDataCommand command) {
        store.refresh();
        return null;
    }
}
