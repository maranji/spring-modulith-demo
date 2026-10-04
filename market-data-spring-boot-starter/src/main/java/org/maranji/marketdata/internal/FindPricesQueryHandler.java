package org.maranji.marketdata.internal;

import java.util.List;

import org.maranji.contractbus.ContractHandler;
import org.maranji.marketdata.contracts.FindPricesQuery;
import org.maranji.marketdata.contracts.PricePoint;

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
