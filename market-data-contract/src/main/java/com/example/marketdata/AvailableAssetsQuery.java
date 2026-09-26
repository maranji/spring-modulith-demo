package com.example.marketdata;

import java.util.Set;

import com.example.basecontract.Contract;

public record AvailableAssetsQuery() implements Contract<Set<String>> {
}
