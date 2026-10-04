package org.maranji.analytics.internal;

import org.maranji.analytics.contracts.PriceStatistics;
import org.maranji.analytics.contracts.StandardDeviationQuery;
import org.maranji.contractbus.ContractHandler;

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
