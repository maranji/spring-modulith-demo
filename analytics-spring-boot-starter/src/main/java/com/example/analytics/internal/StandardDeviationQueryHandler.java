package com.example.analytics.internal;

import com.example.analytics.PriceStatistics;
import com.example.analytics.StandardDeviationQuery;
import com.example.contractbus.ContractHandler;

class StandardDeviationQueryHandler implements ContractHandler<StandardDeviationQuery, PriceStatistics> {

    private final AnalyticsCalculationService service;

    StandardDeviationQueryHandler(AnalyticsCalculationService service) {
        this.service = service;
    }

    @Override
    public PriceStatistics handle(StandardDeviationQuery query) {
        return service.standardDeviation(query.asset(), query.from(), query.to());
    }
}
