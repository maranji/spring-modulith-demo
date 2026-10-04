package org.maranji.contractbus.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contract-bus")
class ContractBusProperties {

    /** Packages scanned at startup for contracts that must have a handler. */
    private List<String> basePackages = new ArrayList<>(List.of("org.maranji"));

    /** Contracts on the classpath that are allowed to have no handler. */
    private Set<Class<?>> ignoredContracts = new LinkedHashSet<>();

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public Set<Class<?>> getIgnoredContracts() {
        return ignoredContracts;
    }

    public void setIgnoredContracts(Set<Class<?>> ignoredContracts) {
        this.ignoredContracts = ignoredContracts;
    }
}
