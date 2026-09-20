package com.example.messaging.internal;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.example.messaging.MessageBus;
import com.example.messaging.MessageHandler;

/**
 * Auto-configurazione del message bus in-process. Basta questa dipendenza
 * sul classpath perché un {@link MessageBus} sia disponibile, raccogliendo
 * automaticamente tutti i bean {@link MessageHandler} registrati dagli
 * altri moduli presenti (market-data, analytics locale o remoto, ...).
 */
@AutoConfiguration
@ConditionalOnClass(MessageBus.class)
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MessageBus messageBus(List<MessageHandler<?, ?>> handlers) {
        return new SimpleMessageBus(handlers);
    }
}
