package com.example.contractbus.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.basecontract.Contract;
import com.example.basecontract.Info;
import com.example.contractbus.ContractBus;
import com.example.contractbus.ContractHandler;
import com.example.contractbus.InfoHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleContractBusTest {

    record Ping(String value) implements Contract<String> {
    }

    record Other(String value) implements Contract<String> {
    }

    record Happened(String value) implements Info {
    }

    record Unobserved() implements Info {
    }

    static class PingHandler implements ContractHandler<Ping, String> {
        @Override
        public String handle(Ping contract) {
            return "pong:" + contract.value();
        }
    }

    static class RecordingHandler implements InfoHandler<Happened> {
        private final String name;
        private final List<String> log;

        RecordingHandler(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void handle(Happened info) {
            log.add(name + ":" + info.value());
        }
    }

    static class FailingHandler implements InfoHandler<Happened> {
        @Override
        public void handle(Happened info) {
            throw new IllegalStateException("boom");
        }
    }

    private static SimpleContractBus busWith(List<ContractHandler<?, ?>> contractHandlers,
                                             List<InfoHandler<?>> infoHandlers) {
        return new SimpleContractBus(() -> contractHandlers, () -> infoHandlers);
    }

    @Test
    void dispatchesToTheHandlerRegisteredForTheContractType() {
        ContractBus bus = busWith(List.of(new PingHandler()), List.of());

        String result = bus.send(new Ping("hello"));

        assertThat(result).isEqualTo("pong:hello");
    }

    @Test
    void throwsWhenNoHandlerIsRegisteredForTheContractType() {
        ContractBus bus = busWith(List.of(new PingHandler()), List.of());

        assertThatThrownBy(() -> bus.send(new Other("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Other");
    }

    @Test
    void failsIndexingWhenTwoHandlersClaimTheSameContractType() {
        SimpleContractBus bus = busWith(List.of(new PingHandler(), new PingHandler()), List.of());

        assertThatThrownBy(bus::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void doesNotResolveHandlersAtConstruction() {
        new SimpleContractBus(
                () -> {
                    throw new AssertionError("contract handlers resolved too early");
                },
                () -> {
                    throw new AssertionError("info handlers resolved too early");
                });
    }

    @Test
    void broadcastsToEveryHandlerRegisteredForTheInfoType() {
        List<String> log = new ArrayList<>();
        ContractBus bus = busWith(List.of(),
                List.of(new RecordingHandler("first", log), new RecordingHandler("second", log)));

        bus.broadcast(new Happened("x"));

        assertThat(log).containsExactly("first:x", "second:x");
    }

    @Test
    void broadcastingAnInfoWithNoHandlersIsANoOp() {
        ContractBus bus = busWith(List.of(), List.of());

        bus.broadcast(new Unobserved());
    }

    @Test
    void aFailingInfoHandlerDoesNotReachTheCallerNorStopTheFollowingOnes() {
        List<String> log = new ArrayList<>();
        ContractBus bus = busWith(List.of(),
                List.of(new FailingHandler(), new RecordingHandler("after", log)));

        bus.broadcast(new Happened("x"));

        assertThat(log).containsExactly("after:x");
    }
}
