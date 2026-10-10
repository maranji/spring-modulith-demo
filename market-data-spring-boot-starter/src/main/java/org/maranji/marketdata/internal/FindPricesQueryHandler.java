package org.maranji.marketdata.internal;

import java.util.List;

import org.maranji.actionrouter.ActionHandler;
import org.maranji.marketdata.actions.FindPricesQuery;
import org.maranji.marketdata.actions.PricePoint;

class FindPricesQueryHandler implements ActionHandler<FindPricesQuery, List<PricePoint>> {

    private final MarketDataStore store;

    FindPricesQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public List<PricePoint> handle(FindPricesQuery query) {
        return store.findPrices(query.assetSymbol(), query.from(), query.to());
    }
}
