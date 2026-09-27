package com.example.marketdata;

import java.time.Instant;
import java.util.Set;

import com.example.basecontract.Info;

public record MarketDataRefreshed(Set<String> assetSymbols, Instant occurredAt) implements Info {
}
