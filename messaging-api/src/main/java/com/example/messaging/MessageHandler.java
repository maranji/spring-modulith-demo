package com.example.messaging;

/**
 * Gestisce un tipo di {@link Message}, producendone la risposta.
 *
 * <p>Un modulo che vuole rispondere a un certo messaggio registra un bean
 * che implementa questa interfaccia (tipicamente in un package
 * {@code internal}, mai esposto pubblicamente): il {@link MessageBus} lo
 * scopre e lo indicizza per tipo di messaggio, senza che il chiamante
 * debba conoscerne l'esistenza.
 *
 * @param <M> il tipo di messaggio gestito
 * @param <R> il tipo di risposta prodotto
 */
public interface MessageHandler<M extends Message<R>, R> {

    R handle(M message);
}
