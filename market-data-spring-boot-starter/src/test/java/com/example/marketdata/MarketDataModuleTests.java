package com.example.marketdata;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifica automatica dei confini del modulo: nessuna classe esterna può
 * dipendere da {@code com.example.marketdata.internal}, e non devono
 * esistere dipendenze cicliche. Fallisce la build in CI se qualcuno rompe
 * l'incapsulamento del modulo.
 */
class MarketDataModuleTests {

    private final ApplicationModules modules = ApplicationModules.of("com.example.marketdata");

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
