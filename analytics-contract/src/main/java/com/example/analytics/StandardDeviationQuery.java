package com.example.analytics;

import java.time.LocalDate;

import com.example.basecontract.Contract;

public record StandardDeviationQuery(String asset, LocalDate from, LocalDate to) implements Contract<PriceStatistics> {
}
