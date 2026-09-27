package com.example.contractbus.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.ResolvableType;

import com.example.basecontract.Contract;
import com.example.basecontract.Info;
import com.example.contractbus.ContractBus;
import com.example.contractbus.ContractHandler;
import com.example.contractbus.InfoHandler;

/**
 * Handlers are resolved lazily: a handler may depend on the bus itself,
 * and injecting them in the constructor would create a cycle.
 *
 * <p>Info notifications are dispatched synchronously, in the caller's thread. A failing
 * handler is only logged: the broadcaster is not affected and the following handlers still run.
 */
class SimpleContractBus implements ContractBus, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(SimpleContractBus.class);

    private final Supplier<List<ContractHandler<?, ?>>> contractHandlerSource;
    private final Supplier<List<InfoHandler<?>>> infoHandlerSource;

    private volatile HandlerIndex index;

    SimpleContractBus(Supplier<List<ContractHandler<?, ?>>> contractHandlerSource,
                      Supplier<List<InfoHandler<?>>> infoHandlerSource) {
        this.contractHandlerSource = contractHandlerSource;
        this.infoHandlerSource = infoHandlerSource;
    }

    // Index at startup, so configuration errors don't wait for the first send.
    @Override
    public void afterSingletonsInstantiated() {
        index();
    }

    private HandlerIndex index() {
        HandlerIndex current = index;
        if (current == null) {
            synchronized (this) {
                current = index;
                if (current == null) {
                    current = new HandlerIndex(
                            contractHandlerSource.get().stream()
                                    .collect(Collectors.toMap(
                                            handler -> resolveHandledType(handler, ContractHandler.class),
                                            handler -> handler)),
                            infoHandlerSource.get().stream()
                                    .collect(Collectors.groupingBy(
                                            handler -> resolveHandledType(handler, InfoHandler.class))));
                    index = current;
                }
            }
        }
        return current;
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
    public <R> R send(Contract<R> contract) {
        var handler = (ContractHandler<Contract<R>, R>) index().contractHandlers().get(contract.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No ContractHandler registered for contract type " + contract.getClass().getName()
                            + "; check that the module implementing it is on the classpath.");
        }
        return handler.handle(contract);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void broadcast(Info info) {
        for (InfoHandler handler : index().infoHandlers().getOrDefault(info.getClass(), List.of())) {
            try {
                handler.handle(info);
            } catch (RuntimeException e) {
                log.error("InfoHandler {} failed on {}", handler.getClass().getName(), info, e);
            }
        }
    }

    private record HandlerIndex(Map<Class<?>, ContractHandler<?, ?>> contractHandlers,
                                Map<Class<?>, List<InfoHandler<?>>> infoHandlers) {
    }
}
