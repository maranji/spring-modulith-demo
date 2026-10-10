package org.maranji.analytics.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.maranji.actionrouter.ActionRouter;
import org.maranji.marketdata.actions.FindPricesQuery;
import org.maranji.marketdata.actions.MarketDataRefreshed;
import org.maranji.marketdata.actions.PricePoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataRefreshedHandlerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    private final ActionRouter actionRouter = mock(ActionRouter.class);
    private final AnalyticsCache cache = new AnalyticsCache();
    private final AnalyticsCalculationService service = new AnalyticsCalculationService(actionRouter, cache);
    private final MarketDataRefreshedHandler handler = new MarketDataRefreshedHandler(cache);

    @Test
    void invalidatesCacheOnMarketDataRefreshed() {
        when(actionRouter.route(new FindPricesQuery("AAPL", FROM, TO))).thenReturn(List.of(
                new PricePoint(LocalDate.of(2026, 1, 2), new BigDecimal("10")),
                new PricePoint(LocalDate.of(2026, 1, 3), new BigDecimal("20"))
        ));

        service.average("AAPL", FROM, TO);
        service.average("AAPL", FROM, TO);
        verify(actionRouter, times(1)).route(new FindPricesQuery("AAPL", FROM, TO));

        handler.handle(new MarketDataRefreshed(Set.of("AAPL"), Instant.now()));
        service.average("AAPL", FROM, TO);

        verify(actionRouter, times(2)).route(new FindPricesQuery("AAPL", FROM, TO));
    }

    @Test
    void doesNotFailWhenCacheIsAlreadyEmpty() {
        handler.handle(new MarketDataRefreshed(Set.of(), Instant.now()));

        assertThat(cache).isNotNull();
    }
}
