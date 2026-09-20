package com.example.analytics.internal;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceStatisticsCalculatorTest {

    @Test
    void computesAverage() {
        List<BigDecimal> prices = List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));

        assertThat(PriceStatisticsCalculator.average(prices)).isEqualTo(20.0);
    }

    @Test
    void computesSampleStandardDeviation() {
        List<BigDecimal> prices = List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));

        // media = 20, scarti quadratici = 100, 0, 100 -> somma 200 / (n-1=2) = 100 -> sqrt = 10
        assertThat(PriceStatisticsCalculator.standardDeviation(prices)).isEqualTo(10.0);
    }

    @Test
    void averageRequiresAtLeastOnePrice() {
        assertThatThrownBy(() -> PriceStatisticsCalculator.average(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void standardDeviationRequiresAtLeastTwoPrices() {
        assertThatThrownBy(() -> PriceStatisticsCalculator.standardDeviation(List.of(new BigDecimal("10"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
