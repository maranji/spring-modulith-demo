package com.example.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Starters live outside {@code com.example.app}, so they are not scanned here:
 * each one is verified by its own module test.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
