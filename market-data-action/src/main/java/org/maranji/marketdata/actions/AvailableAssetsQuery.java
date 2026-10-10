package org.maranji.marketdata.actions;

import java.util.Set;

import org.maranji.actioncore.Action;

public record AvailableAssetsQuery() implements Action<Set<String>> {
}
