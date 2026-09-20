package com.example.marketdata.internal;

import com.example.marketdata.RefreshMarketDataCommand;
import com.example.messaging.MessageHandler;

class RefreshMarketDataCommandHandler implements MessageHandler<RefreshMarketDataCommand, Void> {

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
