package com.example.analytics.internal;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.analytics.AverageQuery;
import com.example.analytics.PriceStatistics;
import com.example.analytics.StandardDeviationQuery;
import com.example.analytics.StatisticType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica che i due handler siano semplici delegati verso
 * {@link AnalyticsCalculationService} — la logica vera è già coperta da
 * {@link AnalyticsCalculationServiceTest}.
 */
class AnalyticsQueryHandlersTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    private final AnalyticsCalculationService service = mock(AnalyticsCalculationService.class);

    @Test
    void averageQueryHandlerDelegatesToTheService() {
        PriceStatistics expected = new PriceStatistics("AAPL", FROM, TO, StatisticType.AVERAGE, 15.0, 2);
        when(service.average("AAPL", FROM, TO)).thenReturn(expected);

        PriceStatistics result = new AverageQueryHandler(service).handle(new AverageQuery("AAPL", FROM, TO));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void standardDeviationQueryHandlerDelegatesToTheService() {
        PriceStatistics expected = new PriceStatistics("AAPL", FROM, TO, StatisticType.STANDARD_DEVIATION, 10.0, 2);
        when(service.standardDeviation("AAPL", FROM, TO)).thenReturn(expected);

        PriceStatistics result = new StandardDeviationQueryHandler(service)
                .handle(new StandardDeviationQuery("AAPL", FROM, TO));

        assertThat(result).isEqualTo(expected);
    }
}
