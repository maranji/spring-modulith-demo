package org.maranji.actionrouter.internal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.ResolvableType;

import org.maranji.actioncore.Action;
import org.maranji.actioncore.Info;
import org.maranji.actionrouter.ActionRouter;
import org.maranji.actionrouter.ActionHandler;
import org.maranji.actionrouter.InfoHandler;

/**
 * Handlers are resolved lazily: a handler may depend on the router itself,
 * and injecting them in the constructor would create a cycle.
 *
 * <p>Info notifications are dispatched synchronously, in the caller's thread. A failing
 * handler is only logged: the broadcaster is not affected and the following handlers still run.
 */
class SimpleActionRouter implements ActionRouter, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(SimpleActionRouter.class);

    private final Supplier<List<ActionHandler<?, ?>>> actionHandlerSource;
    private final Supplier<List<InfoHandler<?>>> infoHandlerSource;
    private final Supplier<Set<Class<?>>> requiredActionTypes;

    private volatile HandlerRegistry registry;

    SimpleActionRouter(Supplier<List<ActionHandler<?, ?>>> actionHandlerSource,
                      Supplier<List<InfoHandler<?>>> infoHandlerSource,
                      Supplier<Set<Class<?>>> requiredActionTypes) {
        this.actionHandlerSource = actionHandlerSource;
        this.infoHandlerSource = infoHandlerSource;
        this.requiredActionTypes = requiredActionTypes;
    }

    // Prefilled at startup, so configuration errors don't wait for the first route.
    @Override
    public void afterSingletonsInstantiated() {
        verifyEveryActionHasAHandler(registry());
    }

    private void verifyEveryActionHasAHandler(HandlerRegistry registry) {
        List<String> unhandled = requiredActionTypes.get().stream()
                .filter(type -> !registry.actionHandlers().containsKey(type))
                .map(Class::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (!unhandled.isEmpty()) {
            throw new IllegalStateException(
                    "No ActionHandler registered for action types " + unhandled
                            + "; add the module implementing them to the classpath,"
                            + " or list them in 'action-router.ignored-actions'.");
        }
    }

    private HandlerRegistry registry() {
        HandlerRegistry current = registry;
        if (current == null) {
            synchronized (this) {
                current = registry;
                if (current == null) {
                    current = new HandlerRegistry(
                            indexActionHandlers(actionHandlerSource.get()),
                            infoHandlerSource.get().stream()
                                    .collect(Collectors.groupingBy(
                                            handler -> resolveHandledType(handler, InfoHandler.class))));
                    registry = current;
                }
            }
        }
        return current;
    }

    private static Map<Class<?>, ActionHandler<?, ?>> indexActionHandlers(List<ActionHandler<?, ?>> handlers) {
        Map<Class<?>, List<ActionHandler<?, ?>>> byType = handlers.stream()
                .collect(Collectors.groupingBy(handler -> resolveHandledType(handler, ActionHandler.class)));
        List<String> duplicates = byType.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey().getName() + " -> " + entry.getValue().stream()
                        .map(handler -> handler.getClass().getName())
                        .toList())
                .sorted(Comparator.naturalOrder())
                .toList();
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException(
                    "More than one ActionHandler registered for the same action type: " + duplicates
                            + "; keep only one implementation module on the classpath.");
        }
        return byType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
    }

    private static Class<?> resolveHandledType(Object handler, Class<?> handlerInterface) {
        Class<?> handledType = ResolvableType.forClass(handler.getClass())
                .as(handlerInterface)
                .getGeneric(0)
                .resolve();
        if (handledType == null) {
            throw new IllegalStateException(
                    "Unable to determine the type handled by " + handler.getClass().getName()
                            + "; make sure it implements " + handlerInterface.getSimpleName()
                            + " with concrete type arguments directly.");
        }
        return handledType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R route(Action<R> action) {
        var handler = (ActionHandler<Action<R>, R>) registry().actionHandlers().get(action.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No ActionHandler registered for action type " + action.getClass().getName()
                            + "; check that the module implementing it is on the classpath.");
        }
        return handler.handle(action);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void broadcast(Info info) {
        for (InfoHandler handler : registry().infoHandlers().getOrDefault(info.getClass(), List.of())) {
            try {
                handler.handle(info);
            } catch (RuntimeException e) {
                log.error("InfoHandler {} failed on {}", handler.getClass().getName(), info, e);
            }
        }
    }

    private record HandlerRegistry(Map<Class<?>, ActionHandler<?, ?>> actionHandlers,
                                   Map<Class<?>, List<InfoHandler<?>>> infoHandlers) {
    }
}
