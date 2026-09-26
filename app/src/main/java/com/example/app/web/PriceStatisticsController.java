package com.example.app.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.analytics.AverageQuery;
import com.example.analytics.PriceStatistics;
import com.example.analytics.StandardDeviationQuery;
import com.example.contractbus.ContractBus;

@RestController
@RequestMapping("/api/assets/{symbol}/statistics")
class PriceStatisticsController {

    private final ContractBus contractBus;

    PriceStatisticsController(ContractBus contractBus) {
        this.contractBus = contractBus;
    }

    @GetMapping("/average")
    PriceStatistics average(@PathVariable String symbol,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return contractBus.send(new AverageQuery(symbol, from, to));
    }

    @GetMapping("/standard-deviation")
    PriceStatistics standardDeviation(@PathVariable String symbol,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return contractBus.send(new StandardDeviationQuery(symbol, from, to));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' (%s) must not be after 'to' (%s)".formatted(from, to));
        }
    }
}
