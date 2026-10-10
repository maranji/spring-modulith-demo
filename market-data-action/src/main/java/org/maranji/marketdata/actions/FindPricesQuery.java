package org.maranji.marketdata.actions;

import java.time.LocalDate;
import java.util.List;

import org.maranji.actioncore.Action;

public record FindPricesQuery(String assetSymbol, LocalDate from, LocalDate to) implements Action<List<PricePoint>> {
}
