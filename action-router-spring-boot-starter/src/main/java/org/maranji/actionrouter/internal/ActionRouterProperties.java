package org.maranji.actionrouter.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "action-router")
class ActionRouterProperties {

    /** Packages scanned at startup for actions that must have a handler. */
    private List<String> basePackages = new ArrayList<>(List.of("org.maranji"));

    /** Actions on the classpath that are allowed to have no handler. */
    private Set<Class<?>> ignoredActions = new LinkedHashSet<>();

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public Set<Class<?>> getIgnoredActions() {
        return ignoredActions;
    }

    public void setIgnoredActions(Set<Class<?>> ignoredActions) {
        this.ignoredActions = ignoredActions;
    }
}
