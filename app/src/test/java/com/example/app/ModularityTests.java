package com.example.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifica la struttura modulare dell'applicazione a partire dal package
 * radice ({@code com.example.app}).
 *
 * <p><b>Nota:</b> gli starter {@code market-data} e {@code analytics} vivono
 * in package (e artefatti Gradle) esterni a {@code com.example.app}, quindi
 * non vengono scansionati da {@code ApplicationModules.of(Application.class)}
 * e non sono soggetti a questa verifica: sono validati singolarmente dai
 * rispettivi test di modulo ({@code MarketDataModuleTests},
 * {@code AnalyticsModuleTests}). Questo è il comportamento atteso per un
 * modulo distribuito come starter riusabile (vedi il caveat nella
 * documentazione di Spring Modulith su starter esterni al base package).
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
