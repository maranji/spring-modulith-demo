package com.example.marketdata.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rappresentazione JSON di un singolo punto di prezzo, così come salvato
 * nei file su disco. Tenuta separata dal record pubblico {@code PricePoint}
 * per non accoppiare il formato di persistenza al contratto pubblico del modulo.
 */
record JsonPricePoint(LocalDate date, BigDecimal price) {
}
