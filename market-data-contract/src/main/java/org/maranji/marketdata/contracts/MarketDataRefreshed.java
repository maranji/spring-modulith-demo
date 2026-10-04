package org.maranji.marketdata.contracts;

import java.time.Instant;
import java.util.Set;

import org.maranji.basecontract.Info;

public record MarketDataRefreshed(Set<String> assetSymbols, Instant occurredAt) implements Info {
}
