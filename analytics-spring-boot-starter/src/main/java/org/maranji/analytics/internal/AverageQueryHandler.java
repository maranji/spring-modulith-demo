package org.maranji.analytics.internal;

import org.maranji.analytics.contracts.AverageQuery;
import org.maranji.analytics.contracts.PriceStatistics;
import org.maranji.contractbus.ContractHandler;

class AverageQueryHandler implements ContractHandler<AverageQuery, PriceStatistics> {

    private final AnalyticsCalculationService service;

    AverageQueryHandler(AnalyticsCalculationService service) {
        this.service = service;
    }

    @Override
    public PriceStatistics handle(AverageQuery query) {
        return service.average(query.asset(), query.from(), query.to());
    }
}
