package com.example.marketdata.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.marketdata.AvailableAssetsQuery;
import com.example.marketdata.FindPricesQuery;
import com.example.marketdata.PricePoint;
import com.example.marketdata.RefreshMarketDataCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica che i tre handler siano semplici delegati verso {@link MarketDataStore}
 * — la logica vera è già coperta da {@link MarketDataStoreTest}.
 */
class MarketDataQueryHandlersTest {

    private final MarketDataStore store = mock(MarketDataStore.class);

    @Test
    void findPricesQueryHandlerDelegatesToTheStore() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        List<PricePoint> prices = List.of(new PricePoint(from, new BigDecimal("10")));
        when(store.findPrices("AAPL", from, to)).thenReturn(prices);

        List<PricePoint> result = new FindPricesQueryHandler(store).handle(new FindPricesQuery("AAPL", from, to));

        assertThat(result).isEqualTo(prices);
    }

    @Test
    void availableAssetsQueryHandlerDelegatesToTheStore() {
        when(store.availableAssets()).thenReturn(Set.of("AAPL", "MSFT"));

        Set<String> result = new AvailableAssetsQueryHandler(store).handle(new AvailableAssetsQuery());

        assertThat(result).containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    void refreshMarketDataCommandHandlerDelegatesToTheStore() {
        Void result = new RefreshMarketDataCommandHandler(store).handle(new RefreshMarketDataCommand());

        assertThat(result).isNull();
        verify(store).refresh();
    }
}
