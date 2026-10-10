package org.maranji.marketdata.internal;

import java.util.List;

import org.maranji.actionrouter.ActionHandler;
import org.maranji.marketdata.actions.AssetPrices;
import org.maranji.marketdata.actions.PricePoint;

class FindPricesQueryHandler implements ActionHandler<AssetPrices, List<PricePoint>> {

    private final MarketDataStore store;

    FindPricesQueryHandler(MarketDataStore store) {
        this.store = store;
    }

    @Override
    public List<PricePoint> handle(AssetPrices query) {
        return store.findPrices(query.assetSymbol(), query.from(), query.to());
    }
}
