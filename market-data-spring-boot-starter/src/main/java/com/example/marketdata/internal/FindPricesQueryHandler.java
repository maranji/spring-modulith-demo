package com.example.marketdata.internal;

import java.util.List;

import com.example.contractbus.ContractHandler;
import com.example.marketdata.FindPricesQuery;
import com.example.marketdata.PricePoint;

class FindPricesQueryHandler implements ContractHandler<FindPricesQuery, List<PricePoint>> {

    private final MarketDataStore store;

    FindPricesQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public List<PricePoint> handle(FindPricesQuery query) {
        return store.findPrices(query.assetSymbol(), query.from(), query.to());
    }
}
