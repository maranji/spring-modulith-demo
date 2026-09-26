package com.example.contractbus.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.basecontract.Contract;
import com.example.contractbus.ContractBus;
import com.example.contractbus.ContractHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleContractBusTest {

    record Ping(String value) implements Contract<String> {
    }

    record Other(String value) implements Contract<String> {
    }

    static class PingHandler implements ContractHandler<Ping, String> {
        @Override
        public String handle(Ping contract) {
            return "pong:" + contract.value();
        }
    }

    @Test
    void dispatchesToTheHandlerRegisteredForTheContractType() {
        ContractBus bus = new SimpleContractBus(() -> List.of(new PingHandler()));

        String result = bus.send(new Ping("hello"));

        assertThat(result).isEqualTo("pong:hello");
    }

    @Test
    void throwsWhenNoHandlerIsRegisteredForTheContractType() {
        ContractBus bus = new SimpleContractBus(() -> List.of(new PingHandler()));

        assertThatThrownBy(() -> bus.send(new Other("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Other");
    }

    @Test
    void doesNotResolveHandlersAtConstruction() {
        new SimpleContractBus(() -> {
            throw new AssertionError("handlers resolved too early");
        });
    }
}
