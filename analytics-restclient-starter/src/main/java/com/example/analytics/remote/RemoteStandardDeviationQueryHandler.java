package com.example.analytics.remote;

import com.example.analytics.PriceStatistics;
import com.example.analytics.StandardDeviationQuery;
import com.example.messaging.MessageHandler;

class RemoteStandardDeviationQueryHandler implements MessageHandler<StandardDeviationQuery, PriceStatistics> {

    private final RemoteAnalyticsClient client;

    RemoteStandardDeviationQueryHandler(RemoteAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public PriceStatistics handle(StandardDeviationQuery query) {
        return client.standardDeviation(query.asset(), query.from(), query.to());
    }
}
