package org.maranji.app.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.maranji.analytics.actions.AveragePrice;
import org.maranji.analytics.actions.PriceStatistics;
import org.maranji.analytics.actions.StandardDeviationQuery;
import org.maranji.actionrouter.ActionRouter;

@RestController
@RequestMapping("/api/assets/{symbol}/statistics")
class PriceStatisticsController {

    private final ActionRouter actionRouter;

    PriceStatisticsController(ActionRouter actionRouter) {
        this.actionRouter = actionRouter;
    }

    @GetMapping("/average")
    PriceStatistics average(@PathVariable String symbol,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return actionRouter.route(new AveragePrice(symbol, from, to));
    }

    @GetMapping("/standard-deviation")
    PriceStatistics standardDeviation(@PathVariable String symbol,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return actionRouter.route(new StandardDeviationQuery(symbol, from, to));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' (%s) must not be after 'to' (%s)".formatted(from, to));
        }
    }
}
