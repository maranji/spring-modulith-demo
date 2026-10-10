package org.maranji.analytics.remote;

import org.maranji.analytics.actions.AveragePrice;
import org.maranji.analytics.actions.PriceStatistics;
import org.maranji.actionrouter.ActionHandler;

class RemoteAverageQueryHandler implements ActionHandler<AveragePrice, PriceStatistics> {

    private final RemoteAnalyticsClient client;

    RemoteAverageQueryHandler(RemoteAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public PriceStatistics handle(AveragePrice query) {
        return client.average(query.asset(), query.from(), query.to());
    }
}
