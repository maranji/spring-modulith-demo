package com.example.analytics.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.marketdata.FindPricesQuery;
import com.example.marketdata.MarketDataRefreshed;
import com.example.marketdata.PricePoint;
import com.example.messaging.MessageBus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataChangeListenerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    private final MessageBus messageBus = mock(MessageBus.class);
    private final AnalyticsCache cache = new AnalyticsCache();
    private final AnalyticsCalculationService service = new AnalyticsCalculationService(messageBus, cache);
    private final MarketDataChangeListener listener = new MarketDataChangeListener(cache);

    @Test
    void invalidatesCacheOnMarketDataRefreshed() {
        when(messageBus.send(new FindPricesQuery("AAPL", FROM, TO))).thenReturn(List.of(
                new PricePoint(LocalDate.of(2026, 1, 2), new BigDecimal("10")),
                new PricePoint(LocalDate.of(2026, 1, 3), new BigDecimal("20"))
        ));

        service.average("AAPL", FROM, TO);
        service.average("AAPL", FROM, TO);
        verify(messageBus, times(1)).send(new FindPricesQuery("AAPL", FROM, TO));

        listener.on(new MarketDataRefreshed(Set.of("AAPL"), Instant.now()));
        service.average("AAPL", FROM, TO);

        verify(messageBus, times(2)).send(new FindPricesQuery("AAPL", FROM, TO));
    }

    @Test
    void doesNotFailWhenCacheIsAlreadyEmpty() {
        listener.on(new MarketDataRefreshed(Set.of(), Instant.now()));

        assertThat(cache).isNotNull();
    }
}
