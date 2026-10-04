package org.maranji.contractbus.internal;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import org.maranji.contractbus.ContractBus;
import org.maranji.contractbus.ContractHandler;
import org.maranji.contractbus.InfoHandler;

@AutoConfiguration
@ConditionalOnClass(ContractBus.class)
@EnableConfigurationProperties(ContractBusProperties.class)
public class ContractBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ContractBus contractBus(ObjectProvider<ContractHandler<?, ?>> contractHandlers,
                            ObjectProvider<InfoHandler<?>> infoHandlers,
                            ContractBusProperties properties,
                            ResourceLoader resourceLoader) {
        return new SimpleContractBus(
                () -> contractHandlers.orderedStream().toList(),
                () -> infoHandlers.orderedStream().toList(),
                () -> requiredContractTypes(properties, resourceLoader));
    }

    private static Set<Class<?>> requiredContractTypes(ContractBusProperties properties,
                                                       ResourceLoader resourceLoader) {
        Set<Class<?>> required = new HashSet<>(
                ContractTypeScanner.scan(properties.getBasePackages(), resourceLoader));
        required.removeAll(properties.getIgnoredContracts());
        return required;
    }
}
