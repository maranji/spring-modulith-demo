package org.maranji.marketdata.actions;

import org.maranji.actioncore.Action;

/** The handler broadcasts {@link MarketDataRefreshed} when done. */
public record RefreshMarketData() implements Action<Void> {
}
