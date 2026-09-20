package com.example.analytics.remote;

import com.example.analytics.AverageQuery;
import com.example.analytics.PriceStatistics;
import com.example.messaging.MessageHandler;

class RemoteAverageQueryHandler implements MessageHandler<AverageQuery, PriceStatistics> {

    private final RemoteAnalyticsClient client;

    RemoteAverageQueryHandler(RemoteAnalyticsClient client) {
        this.client = client;
    }

    @Override
    public PriceStatistics handle(AverageQuery query) {
        return client.average(query.asset(), query.from(), query.to());
    }
}
