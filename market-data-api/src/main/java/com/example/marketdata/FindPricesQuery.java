package com.example.marketdata;

import java.time.LocalDate;
import java.util.List;

import com.example.messaging.Message;

/**
 * Richiede i prezzi di un asset nell'intervallo di date indicato (estremi
 * inclusi). Gestita dal modulo Market Data tramite il {@code MessageBus}:
 * chi invia questo messaggio non conosce, e non deve conoscere,
 * l'implementazione che lo gestisce (file su disco, database, servizio
 * esterno, ...).
 *
 * @param assetSymbol il simbolo dell'asset (non case-sensitive)
 * @param from        data di inizio periodo, inclusa
 * @param to          data di fine periodo, inclusa
 * @throws AssetNotFoundException (sollevata dall'handler) se l'asset non è mai stato caricato
 */
public record FindPricesQuery(String assetSymbol, LocalDate from, LocalDate to) implements Message<List<PricePoint>> {
}
