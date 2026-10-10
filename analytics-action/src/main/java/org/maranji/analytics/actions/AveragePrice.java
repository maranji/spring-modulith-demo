package org.maranji.analytics.actions;

import java.time.LocalDate;

import org.maranji.actioncore.Action;

public record AveragePrice(String asset, LocalDate from, LocalDate to) implements Action<PriceStatistics> {
}
