package com.example.analytics.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.analytics.InsufficientDataException;
import com.example.analytics.PriceStatistics;
import com.example.analytics.StatisticType;
import com.example.marketdata.FindPricesQuery;
import com.example.marketdata.PricePoint;
import com.example.messaging.MessageBus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsCalculationServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    private final MessageBus messageBus = mock(MessageBus.class);
    private final AnalyticsCache cache = new AnalyticsCache();
    private final AnalyticsCalculationService service = new AnalyticsCalculationService(messageBus, cache);

    @Test
    void computesAverageFromPricesReturnedByTheMessageBus() {
        when(messageBus.send(new FindPricesQuery("AAPL", FROM, TO))).thenReturn(List.of(
                new PricePoint(LocalDate.of(2026, 1, 2), new BigDecimal("10")),
                new PricePoint(LocalDate.of(2026, 1, 3), new BigDecimal("20"))
        ));

        PriceStatistics result = service.average("aapl", FROM, TO);

        assertThat(result.asset()).isEqualTo("AAPL");
        assertThat(result.type()).isEqualTo(StatisticType.AVERAGE);
        assertThat(result.value()).isEqualTo(15.0);
        assertThat(result.sampleSize()).isEqualTo(2);
    }

    @Test
    void cachesResultsUntilCacheIsCleared() {
        when(messageBus.send(new FindPricesQuery("AAPL", FROM, TO))).thenReturn(List.of(
                new PricePoint(LocalDate.of(2026, 1, 2), new BigDecimal("10")),
                new PricePoint(LocalDate.of(2026, 1, 3), new BigDecimal("20"))
        ));

        service.average("AAPL", FROM, TO);
        service.average("AAPL", FROM, TO);
        verify(messageBus, times(1)).send(new FindPricesQuery("AAPL", FROM, TO));

        cache.clear();
        service.average("AAPL", FROM, TO);

        verify(messageBus, times(2)).send(new FindPricesQuery("AAPL", FROM, TO));
    }

    @Test
    void throwsWhenNoPricesAvailableForAverage() {
        when(messageBus.send(new FindPricesQuery("AAPL", FROM, TO))).thenReturn(List.of());

        assertThatThrownBy(() -> service.average("AAPL", FROM, TO))
                .isInstanceOf(InsufficientDataException.class);
    }

    @Test
    void standardDeviationRequiresAtLeastTwoPoints() {
        when(messageBus.send(new FindPricesQuery("AAPL", FROM, TO))).thenReturn(List.of(
                new PricePoint(LocalDate.of(2026, 1, 2), new BigDecimal("10"))
        ));

        assertThatThrownBy(() -> service.standardDeviation("AAPL", FROM, TO))
                .isInstanceOf(InsufficientDataException.class);
    }
}
