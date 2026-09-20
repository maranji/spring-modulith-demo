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
import com.example.messaging.MessageBus;

/**
 * Espone il calcolo di media e deviazione standard del prezzo di un asset
 * in un periodo di tempo.
 *
 * <p>Nota di progettazione: questo controller invia messaggi
 * (({@link AverageQuery}, {@link StandardDeviationQuery}) al
 * {@link MessageBus} — non importa né conosce alcuna interfaccia di
 * servizio del modulo Analytics. L'unica cosa che condivide con quel
 * modulo è il contratto (i messaggi e {@link PriceStatistics}), mai un
 * riferimento alla sua implementazione: locale o remota, "app" non lo sa
 * né le interessa (vedi README, sezione sul disaccoppiamento via message bus).
 *
 * <pre>
 * GET /api/assets/AAPL/statistics/average?from=2026-01-01&amp;to=2026-01-31
 * GET /api/assets/AAPL/statistics/standard-deviation?from=2026-01-01&amp;to=2026-01-31
 * </pre>
 */
@RestController
@RequestMapping("/api/assets/{symbol}/statistics")
class PriceStatisticsController {

    private final MessageBus messageBus;

    PriceStatisticsController(MessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @GetMapping("/average")
    PriceStatistics average(@PathVariable String symbol,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return messageBus.send(new AverageQuery(symbol, from, to));
    }

    @GetMapping("/standard-deviation")
    PriceStatistics standardDeviation(@PathVariable String symbol,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return messageBus.send(new StandardDeviationQuery(symbol, from, to));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' (%s) must not be after 'to' (%s)".formatted(from, to));
        }
    }
}
