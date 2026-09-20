package com.example.marketdata.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.marketdata.AssetNotFoundException;
import com.example.marketdata.MarketDataRefreshed;
import com.example.marketdata.PricePoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test unitario dell'implementazione: nessun contesto Spring, solo
 * costruzione diretta con collaboratori reali/mock.
 */
class MarketDataStoreTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private MarketDataStore store;

    @BeforeEach
    void setUp() {
        MarketDataProperties properties = new MarketDataProperties();
        properties.setDirectory("classpath:test-data/");

        store = new MarketDataStore(
                new PathMatchingResourcePatternResolver(), objectMapper, properties, events);
        store.init();
    }

    @Test
    void loadsPricesFromJsonFilesOnDisk() {
        assertThat(store.availableAssets()).containsExactly("DEMO");

        List<PricePoint> prices = store.findPrices("demo", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(prices).hasSize(3);
        assertThat(prices.get(0).date()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(prices.get(0).price()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(events).publishEvent(any(MarketDataRefreshed.class));
    }

    @Test
    void filtersByDateRange() {
        List<PricePoint> prices = store.findPrices("DEMO", LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 3));

        assertThat(prices).hasSize(1);
        assertThat(prices.get(0).price()).isEqualByComparingTo(new BigDecimal("102.50"));
    }

    @Test
    void throwsWhenAssetIsUnknown() {
        assertThatThrownBy(() -> store.findPrices("UNKNOWN", LocalDate.now(), LocalDate.now()))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void refreshReloadsDataAndPublishesEvent() {
        store.refresh();

        verify(events, org.mockito.Mockito.times(2)).publishEvent(any(MarketDataRefreshed.class));
    }
}
