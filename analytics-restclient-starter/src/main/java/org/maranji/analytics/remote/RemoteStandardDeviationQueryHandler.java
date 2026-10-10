package org.maranji.analytics.remote;

import org.maranji.analytics.actions.PriceStatistics;
import org.maranji.analytics.actions.StandardDeviationQuery;
import org.maranji.actionrouter.ActionHandler;

class RemoteStandardDeviationQueryHandler implements ActionHandler<StandardDeviationQuery, PriceStatistics> {

    private final RemoteAnalyticsClient client;

    RemoteStandardDeviationQueryHandler(RemoteAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public PriceStatistics handle(StandardDeviationQuery query) {
        return client.standardDeviation(query.asset(), query.from(), query.to());
    }
}
