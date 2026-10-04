package org.maranji.marketdata.contracts;

import java.util.Set;

import org.maranji.basecontract.Contract;

public record AvailableAssetsQuery() implements Contract<Set<String>> {
}
