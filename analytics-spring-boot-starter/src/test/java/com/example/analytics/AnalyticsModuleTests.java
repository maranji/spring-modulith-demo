package com.example.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class AnalyticsModuleTests {

    private final ApplicationModules modules = ApplicationModules.of("com.example.analytics");

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
