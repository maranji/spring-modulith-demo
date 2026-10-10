package org.maranji.marketdata.actions;

import java.time.Instant;
import java.util.Set;

import org.maranji.actioncore.Info;

public record MarketDataRefreshed(Set<String> assetSymbols, Instant occurredAt) implements Info {
}
