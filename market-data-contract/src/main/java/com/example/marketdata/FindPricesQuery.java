package com.example.marketdata;

import java.time.LocalDate;
import java.util.List;

import com.example.basecontract.Contract;

public record FindPricesQuery(String assetSymbol, LocalDate from, LocalDate to) implements Contract<List<PricePoint>> {
}
