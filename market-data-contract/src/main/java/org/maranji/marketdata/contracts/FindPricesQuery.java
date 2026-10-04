package org.maranji.marketdata.contracts;

import java.time.LocalDate;
import java.util.List;

import org.maranji.basecontract.Contract;

public record FindPricesQuery(String assetSymbol, LocalDate from, LocalDate to) implements Contract<List<PricePoint>> {
}
