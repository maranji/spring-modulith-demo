package com.example.contractbus.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.ResolvableType;

import com.example.basecontract.Contract;
import com.example.contractbus.ContractBus;
import com.example.contractbus.ContractHandler;

/**
 * Handlers are resolved lazily: a handler may depend on the bus itself,
 * and injecting them in the constructor would create a cycle.
 */
class SimpleContractBus implements ContractBus, SmartInitializingSingleton {

    private final Supplier<List<ContractHandler<?, ?>>> handlerSource;

    @SuppressWarnings("rawtypes")
    private volatile Map<Class<?>, ContractHandler> handlersByContractType;

    SimpleContractBus(Supplier<List<ContractHandler<?, ?>>> handlerSource) {
        this.handlerSource = handlerSource;
    }

    // Index at startup, so configuration errors don't wait for the first send.
    @Override
    public void afterSingletonsInstantiated() {
        handlersByContractType();
    }

    @SuppressWarnings("rawtypes")
    private Map<Class<?>, ContractHandler> handlersByContractType() {
        Map<Class<?>, ContractHandler> index = handlersByContractType;
        if (index == null) {
            synchronized (this) {
                index = handlersByContractType;
                if (index == null) {
                    index = handlerSource.get().stream()
                            .collect(Collectors.toMap(this::resolveContractType, handler -> (ContractHandler) handler));
                    handlersByContractType = index;
                }
            }
        }
        return index;
    }

    private Class<?> resolveContractType(ContractHandler<?, ?> handler) {
        Class<?> contractType = ResolvableType.forClass(handler.getClass())
                .as(ContractHandler.class)
                .getGeneric(0)
                .resolve();
        if (contractType == null) {
            throw new IllegalStateException(
                    "Unable to determine the contract type handled by " + handler.getClass().getName()
                            + "; make sure it implements ContractHandler<SomeContract, SomeResponse> directly.");
        }
        return contractType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Contract<R> contract) {
        ContractHandler<Contract<R>, R> handler = handlersByContractType().get(contract.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No ContractHandler registered for contract type " + contract.getClass().getName()
                            + "; check that the module implementing it is on the classpath.");
        }
        return handler.handle(contract);
    }
}
