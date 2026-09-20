package com.example.marketdata.internal;

import java.util.List;

/**
 * Formato di un file JSON di dati di prezzo, uno per ogni asset. Esempio:
 *
 * <pre>{@code
 * {
 *   "asset": "AAPL",
 *   "currency": "USD",
 *   "prices": [
 *     { "date": "2026-01-02", "price": 228.50 },
 *     { "date": "2026-01-05", "price": 231.10 }
 *   ]
 * }
 * }</pre>
 */
record JsonAssetPriceFile(String asset, String currency, List<JsonPricePoint> prices) {
}
