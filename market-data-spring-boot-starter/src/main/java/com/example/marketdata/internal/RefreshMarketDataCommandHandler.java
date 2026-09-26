package com.example.marketdata.internal;

import com.example.contractbus.ContractHandler;
import com.example.marketdata.RefreshMarketDataCommand;

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
