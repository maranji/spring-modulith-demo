package org.maranji.actionrouter.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import org.maranji.actioncore.Action;

/**
 * Finds the concrete {@link Action} types on the classpath, so that a missing
 * implementation module surfaces at startup instead of on the first {@code route}.
 */
final class ActionTypeScanner {

    private ActionTypeScanner() {
    }

    static Set<Class<?>> scan(Collection<String> basePackages, ResourceLoader resourceLoader) {
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.setResourceLoader(resourceLoader);
        provider.addIncludeFilter(new AssignableTypeFilter(Action.class));
        Set<Class<?>> actionTypes = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : provider.findCandidateComponents(basePackage)) {
                actionTypes.add(ClassUtils.resolveClassName(
                        candidate.getBeanClassName(), resourceLoader.getClassLoader()));
            }
        }
        return actionTypes;
    }
}
