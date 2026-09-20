package com.example.messaging.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.messaging.Message;
import com.example.messaging.MessageBus;
import com.example.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleMessageBusTest {

    record Ping(String value) implements Message<String> {
    }

    record Other(String value) implements Message<String> {
    }

    static class PingHandler implements MessageHandler<Ping, String> {
        @Override
        public String handle(Ping message) {
            return "pong:" + message.value();
        }
    }

    @Test
    void dispatchesToTheHandlerRegisteredForTheMessageType() {
        MessageBus bus = new SimpleMessageBus(List.of(new PingHandler()));

        String result = bus.send(new Ping("hello"));

        assertThat(result).isEqualTo("pong:hello");
    }

    @Test
    void throwsWhenNoHandlerIsRegisteredForTheMessageType() {
        MessageBus bus = new SimpleMessageBus(List.of(new PingHandler()));

        assertThatThrownBy(() -> bus.send(new Other("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Other");
    }
}
