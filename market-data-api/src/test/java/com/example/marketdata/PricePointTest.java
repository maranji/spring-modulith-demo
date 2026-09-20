package com.example.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricePointTest {

    @Test
    void rejectsNullDate() {
        assertThatThrownBy(() -> new PricePoint(null, new BigDecimal("10")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPrice() {
        assertThatThrownBy(() -> new PricePoint(LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
