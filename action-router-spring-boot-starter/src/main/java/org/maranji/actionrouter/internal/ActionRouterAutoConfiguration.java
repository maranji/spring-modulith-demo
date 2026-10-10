package org.maranji.actionrouter.internal;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import org.maranji.actionrouter.ActionRouter;
import org.maranji.actionrouter.ActionHandler;
import org.maranji.actionrouter.InfoHandler;

@AutoConfiguration
@ConditionalOnClass(ActionRouter.class)
@EnableConfigurationProperties(ActionRouterProperties.class)
public class ActionRouterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ActionRouter actionRouter(ObjectProvider<ActionHandler<?, ?>> actionHandlers,
                            ObjectProvider<InfoHandler<?>> infoHandlers,
                            ActionRouterProperties properties,
                            ResourceLoader resourceLoader) {
        return new SimpleActionRouter(
                () -> actionHandlers.orderedStream().toList(),
                () -> infoHandlers.orderedStream().toList(),
                () -> requiredActionTypes(properties, resourceLoader));
    }

    private static Set<Class<?>> requiredActionTypes(ActionRouterProperties properties,
                                                       ResourceLoader resourceLoader) {
        Set<Class<?>> required = new HashSet<>(
                ActionTypeScanner.scan(properties.getBasePackages(), resourceLoader));
        required.removeAll(properties.getIgnoredActions());
        return required;
    }
}
