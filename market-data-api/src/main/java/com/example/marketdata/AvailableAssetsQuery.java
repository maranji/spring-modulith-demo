package com.example.marketdata;

import java.util.Set;

import com.example.messaging.Message;

/** Richiede l'elenco dei simboli per cui sono disponibili dati di prezzo. */
public record AvailableAssetsQuery() implements Message<Set<String>> {
}
