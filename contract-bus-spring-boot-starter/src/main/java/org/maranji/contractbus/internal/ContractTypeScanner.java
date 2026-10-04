package org.maranji.contractbus.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import org.maranji.basecontract.Contract;

/**
 * Finds the concrete {@link Contract} types on the classpath, so that a missing
 * implementation module surfaces at startup instead of on the first {@code send}.
 */
final class ContractTypeScanner {

    private ContractTypeScanner() {
    }

    static Set<Class<?>> scan(Collection<String> basePackages, ResourceLoader resourceLoader) {
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.setResourceLoader(resourceLoader);
        provider.addIncludeFilter(new AssignableTypeFilter(Contract.class));
        Set<Class<?>> contractTypes = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : provider.findCandidateComponents(basePackage)) {
                contractTypes.add(ClassUtils.resolveClassName(
                        candidate.getBeanClassName(), resourceLoader.getClassLoader()));
            }
        }
        return contractTypes;
    }
}
