package com.example.marketdata.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Kept separate from {@code PricePoint} so the file format isn't coupled to the public contract. */
record JsonPricePoint(LocalDate date, BigDecimal price) {
}
