package com.example.contractbus.internal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.example.contractbus.ContractBus;
import com.example.contractbus.ContractHandler;
import com.example.contractbus.InfoHandler;

@AutoConfiguration
@ConditionalOnClass(ContractBus.class)
public class ContractBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ContractBus contractBus(ObjectProvider<ContractHandler<?, ?>> contractHandlers,
                            ObjectProvider<InfoHandler<?>> infoHandlers) {
        return new SimpleContractBus(
                () -> contractHandlers.orderedStream().toList(),
                () -> infoHandlers.orderedStream().toList());
    }
}
