package org.maranji.analytics.contracts;

import java.time.LocalDate;

import org.maranji.basecontract.Contract;

public record StandardDeviationQuery(String asset, LocalDate from, LocalDate to) implements Contract<PriceStatistics> {
}
