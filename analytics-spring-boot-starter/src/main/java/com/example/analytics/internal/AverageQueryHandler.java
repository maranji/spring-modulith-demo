package com.example.analytics.internal;

import com.example.analytics.AverageQuery;
import com.example.analytics.PriceStatistics;
import com.example.messaging.MessageHandler;

class AverageQueryHandler implements MessageHandler<AverageQuery, PriceStatistics> {

    private final AnalyticsCalculationService service;

    AverageQueryHandler(AnalyticsCalculationService service) {
        this.service = service;
    }

    @Override
    public PriceStatistics handle(AverageQuery query) {
        return service.average(query.asset(), query.from(), query.to());
    }
}
