package org.maranji.marketdata.contracts;

import org.maranji.basecontract.Contract;

/** The handler broadcasts {@link MarketDataRefreshed} when done. */
public record RefreshMarketDataCommand() implements Contract<Void> {
}
