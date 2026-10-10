package org.maranji.actionrouter.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.maranji.actioncore.Action;
import org.maranji.actioncore.Info;
import org.maranji.actionrouter.ActionRouter;
import org.maranji.actionrouter.ActionHandler;
import org.maranji.actionrouter.InfoHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleActionRouterTest {

    record Ping(String value) implements Action<String> {
    }

    record Other(String value) implements Action<String> {
    }

    record Happened(String value) implements Info {
    }

    record Unobserved() implements Info {
    }

    static class PingHandler implements ActionHandler<Ping, String> {
        @Override
        public String handle(Ping action) {
            return "pong:" + action.value();
        }
    }

    static class AlternativePingHandler implements ActionHandler<Ping, String> {
        @Override
        public String handle(Ping action) {
            return "alternative-pong:" + action.value();
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

    private static SimpleActionRouter routerWith(List<ActionHandler<?, ?>> actionHandlers,
                                             List<InfoHandler<?>> infoHandlers) {
        return routerWith(actionHandlers, infoHandlers, Set.of());
    }

    private static SimpleActionRouter routerWith(List<ActionHandler<?, ?>> actionHandlers,
                                             List<InfoHandler<?>> infoHandlers,
                                             Set<Class<?>> requiredActionTypes) {
        return new SimpleActionRouter(() -> actionHandlers, () -> infoHandlers, () -> requiredActionTypes);
    }

    @Test
    void dispatchesToTheHandlerRegisteredForTheActionType() {
        ActionRouter router = routerWith(List.of(new PingHandler()), List.of());

        String result = router.route(new Ping("hello"));

        assertThat(result).isEqualTo("pong:hello");
    }

    @Test
    void throwsWhenNoHandlerIsRegisteredForTheActionType() {
        ActionRouter router = routerWith(List.of(new PingHandler()), List.of());

        assertThatThrownBy(() -> router.route(new Other("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Other");
    }

    @Test
    void failsIndexingWhenTwoHandlersClaimTheSameActionType() {
        SimpleActionRouter router = routerWith(List.of(new PingHandler(), new AlternativePingHandler()), List.of());

        assertThatThrownBy(router::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Ping.class.getName())
                .hasMessageContaining(PingHandler.class.getName())
                .hasMessageContaining(AlternativePingHandler.class.getName());
    }

    @Test
    void doesNotResolveHandlersAtConstruction() {
        new SimpleActionRouter(
                () -> {
                    throw new AssertionError("action handlers resolved too early");
                },
                () -> {
                    throw new AssertionError("info handlers resolved too early");
                },
                () -> {
                    throw new AssertionError("required action types resolved too early");
                });
    }

    @Test
    void failsAtStartupWhenARequiredActionHasNoHandler() {
        SimpleActionRouter router = routerWith(List.of(new PingHandler()), List.of(), Set.of(Ping.class, Other.class));

        assertThatThrownBy(router::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Other.class.getName())
                .hasMessageNotContaining(Ping.class.getName());
    }

    @Test
    void startsWhenEveryRequiredActionHasAHandler() {
        SimpleActionRouter router = routerWith(List.of(new PingHandler()), List.of(), Set.of(Ping.class));

        router.afterSingletonsInstantiated();
    }

    @Test
    void broadcastsToEveryHandlerRegisteredForTheInfoType() {
        List<String> log = new ArrayList<>();
        ActionRouter router = routerWith(List.of(),
                List.of(new RecordingHandler("first", log), new RecordingHandler("second", log)));

        router.broadcast(new Happened("x"));

        assertThat(log).containsExactly("first:x", "second:x");
    }

    @Test
    void broadcastingAnInfoWithNoHandlersIsANoOp() {
        ActionRouter router = routerWith(List.of(), List.of());

        router.broadcast(new Unobserved());
    }

    @Test
    void aFailingInfoHandlerDoesNotReachTheCallerNorStopTheFollowingOnes() {
        List<String> log = new ArrayList<>();
        ActionRouter router = routerWith(List.of(),
                List.of(new FailingHandler(), new RecordingHandler("after", log)));

        router.broadcast(new Happened("x"));

        assertThat(log).containsExactly("after:x");
    }
}
