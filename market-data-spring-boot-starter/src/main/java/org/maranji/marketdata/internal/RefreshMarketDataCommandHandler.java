package org.maranji.marketdata.internal;

import org.maranji.contractbus.ContractHandler;
import org.maranji.marketdata.contracts.RefreshMarketDataCommand;

class RefreshMarketDataCommandHandler implements ContractHandler<RefreshMarketDataCommand, Void> {

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
