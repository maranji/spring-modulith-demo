package com.example.messaging.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.ResolvableType;

import com.example.messaging.Message;
import com.example.messaging.MessageBus;
import com.example.messaging.MessageHandler;

/**
 * Implementazione in-process, in memoria, di {@link MessageBus}.
 *
 * <p>All'avvio riceve tutti i bean {@link MessageHandler} presenti nel
 * contesto Spring (registrati dalle varie auto-configuration dei moduli:
 * market-data, analytics locale o remoto, ecc.) e li indicizza per il tipo
 * concreto di {@link Message} che dichiarano di gestire, ricavato via
 * reflection dal parametro generico dell'interfaccia. Da quel momento,
 * {@link #send(Message)} instrada ogni messaggio all'handler giusto senza
 * che il chiamante (es. i controller REST di "app") debba conoscere quale
 * modulo/classe lo gestisce davvero.
 */
class SimpleMessageBus implements MessageBus {

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, MessageHandler> handlersByMessageType;

    @SuppressWarnings("rawtypes")
    SimpleMessageBus(List<MessageHandler<?, ?>> handlers) {
        this.handlersByMessageType = handlers.stream()
                .collect(Collectors.toMap(this::resolveMessageType, handler -> (MessageHandler) handler));
    }

    private Class<?> resolveMessageType(MessageHandler<?, ?> handler) {
        Class<?> messageType = ResolvableType.forClass(handler.getClass())
                .as(MessageHandler.class)
                .getGeneric(0)
                .resolve();
        if (messageType == null) {
            throw new IllegalStateException(
                    "Unable to determine the Message type handled by " + handler.getClass().getName()
                            + "; make sure it implements MessageHandler<SomeMessage, SomeResponse> directly.");
        }
        return messageType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Message<R> message) {
        MessageHandler<Message<R>, R> handler = handlersByMessageType.get(message.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No MessageHandler registered for message type " + message.getClass().getName()
                            + "; check that the module implementing it is on the classpath.");
        }
        return handler.handle(message);
    }
}
