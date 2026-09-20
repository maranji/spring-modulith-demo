package com.example.marketdata.internal;

import java.util.List;

import com.example.marketdata.FindPricesQuery;
import com.example.marketdata.PricePoint;
import com.example.messaging.MessageHandler;

class FindPricesQueryHandler implements MessageHandler<FindPricesQuery, List<PricePoint>> {

    private final MarketDataStore store;

    FindPricesQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public List<PricePoint> handle(FindPricesQuery query) {
        return store.findPrices(query.assetSymbol(), query.from(), query.to());
    }
}
