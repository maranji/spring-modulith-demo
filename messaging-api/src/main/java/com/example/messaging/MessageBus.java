package com.example.messaging;

/**
 * Punto di ingresso unico attraverso cui un modulo (tipicamente "app", ma
 * potenzialmente anche un altro modulo di dominio) invoca la funzionalità
 * di un altro, conoscendo solo il {@link Message} di richiesta e il tipo
 * di risposta — mai l'interfaccia di servizio né l'implementazione che lo
 * gestisce.
 *
 * <p>In questa demo l'unica implementazione è in-process
 * ({@code messaging-spring-boot-starter}, un dispatcher in memoria), ma la
 * stessa interfaccia potrebbe domani essere implementata inoltrando il
 * messaggio su HTTP, su una coda, ecc. senza che il codice chiamante
 * cambi: esattamente il "cambio di trasporto, non di design" descritto nel
 * documento di riferimento.
 */
public interface MessageBus {

    <R> R send(Message<R> message);
}
