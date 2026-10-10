package org.maranji.analytics.internal;

import org.maranji.analytics.actions.AverageQuery;
import org.maranji.analytics.actions.PriceStatistics;
import org.maranji.actionrouter.ActionHandler;

class AverageQueryHandler implements ActionHandler<AverageQuery, PriceStatistics> {

    private final AnalyticsCalculationService service;

    AverageQueryHandler(AnalyticsCalculationService service) {
        this.service = service;
    }

    @Override
    public PriceStatistics handle(AverageQuery query) {
        return service.average(query.asset(), query.from(), query.to());
    }
}
