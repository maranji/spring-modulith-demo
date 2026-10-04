package org.maranji.analytics.contracts;

import java.time.LocalDate;

import org.maranji.basecontract.Contract;

public record AverageQuery(String asset, LocalDate from, LocalDate to) implements Contract<PriceStatistics> {
}
