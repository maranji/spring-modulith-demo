package com.example.marketdata;

import java.time.Instant;
import java.util.Set;

public record MarketDataRefreshed(Set<String> assetSymbols, Instant occurredAt) {
}
