package com.example.messaging;

/**
 * Marcatore per una richiesta (query o comando) che un modulo può inviare
 * a un altro attraverso il {@link MessageBus}, invece di chiamare
 * direttamente un'interfaccia di servizio di quel modulo.
 *
 * <p>{@code R} è il tipo della risposta attesa (usare {@code Void} — con
 * l'handler che restituisce {@code null} — per un comando senza risposta
 * significativa, come {@code RefreshMarketDataCommand}).
 *
 * <p>Le implementazioni sono tipicamente record immutabili definiti nel
 * modulo "-api" del dominio che le gestisce (es. {@code analytics-api}),
 * cioè l'unica cosa che un chiamante come "app" deve importare: mai
 * l'interfaccia di servizio o l'implementazione di quel modulo.
 *
 * @param <R> il tipo di risposta prodotto dall'handler di questo messaggio
 */
public interface Message<R> {
}
