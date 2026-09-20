package com.example.marketdata;

import java.time.Instant;
import java.util.Set;

/**
 * Evento applicativo pubblicato dal modulo Market Data ogni volta che i
 * dati di prezzo vengono (ri)caricati dal disco.
 *
 * <p>Altri moduli (es. Analytics) possono reagire a questo evento con
 * {@code @ApplicationModuleListener}, ad esempio per invalidare una cache,
 * senza avere alcuna dipendenza diretta dalle classi interne di questo modulo.
 *
 * @param assetSymbols i simboli degli asset per cui sono ora disponibili dati aggiornati
 * @param occurredAt   il momento in cui il ricaricamento è avvenuto
 */
public record MarketDataRefreshed(Set<String> assetSymbols, Instant occurredAt) {
}
