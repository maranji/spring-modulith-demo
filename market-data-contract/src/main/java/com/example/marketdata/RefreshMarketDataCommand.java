package com.example.marketdata;

import com.example.basecontract.Contract;

/** The handler publishes {@link MarketDataRefreshed} when done. */
public record RefreshMarketDataCommand() implements Contract<Void> {
}
