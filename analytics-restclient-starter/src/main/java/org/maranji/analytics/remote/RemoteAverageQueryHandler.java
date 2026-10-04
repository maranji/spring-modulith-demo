package org.maranji.analytics.remote;

import org.maranji.analytics.contracts.AverageQuery;
import org.maranji.analytics.contracts.PriceStatistics;
import org.maranji.contractbus.ContractHandler;

class RemoteAverageQueryHandler implements ContractHandler<AverageQuery, PriceStatistics> {

    private final RemoteAnalyticsClient client;

    RemoteAverageQueryHandler(RemoteAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public PriceStatistics handle(AverageQuery query) {
        return client.average(query.asset(), query.from(), query.to());
    }
}
