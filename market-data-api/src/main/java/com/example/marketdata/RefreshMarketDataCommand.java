package com.example.marketdata;

import com.example.messaging.Message;

/**
 * Comando che forza un ricaricamento immediato dei dati di prezzo.
 * L'handler pubblica un evento {@link MarketDataRefreshed} al termine.
 * Nessuna risposta significativa (l'handler restituisce {@code null}).
 */
public record RefreshMarketDataCommand() implements Message<Void> {
}
