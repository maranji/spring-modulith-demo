package org.maranji.analytics.remote;

import org.maranji.analytics.contracts.PriceStatistics;
import org.maranji.analytics.contracts.StandardDeviationQuery;
import org.maranji.contractbus.ContractHandler;

class RemoteStandardDeviationQueryHandler implements ContractHandler<StandardDeviationQuery, PriceStatistics> {

    private final RemoteAnalyticsClient client;

    RemoteStandardDeviationQueryHandler(RemoteAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public PriceStatistics handle(StandardDeviationQuery query) {
        return client.standardDeviation(query.asset(), query.from(), query.to());
    }
}
