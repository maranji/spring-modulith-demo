package com.example.marketdata.internal;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione del modulo Market Data.
 *
 * <pre>{@code
 * market-data:
 *   directory: file:/opt/market-data/       # o classpath:data/ per la demo
 *   refresh-interval: PT5M
 *   refresh-enabled: true
 * }</pre>
 *
 * <p>Visibilità di package di proposito (vedi la nota su
 * {@link MarketDataStore} sul perché non basta il {@code verify()}
 * di Spring Modulith a coprire dipendenze tra artefatti Gradle diversi).
 */
@ConfigurationProperties(prefix = "market-data")
class MarketDataProperties {

    /**
     * Directory contenente un file JSON per asset (uno per asset, es.
     * {@code AAPL.json}). Accetta la sintassi delle {@code Resource} di
     * Spring: {@code classpath:data/} per risorse incluse nel jar (comodo
     * per la demo), oppure {@code file:/percorso/assoluto/} per leggere da
     * una cartella reale sul disco della macchina, come richiesto in produzione.
     */
    private String directory = "classpath:data/";

    /**
     * Intervallo con cui il modulo ricarica periodicamente i file dal disco.
     * Impostare {@code refresh-enabled: false} per disabilitare il refresh
     * automatico e ricaricare solo su richiesta esplicita (inviando un
     * {@code RefreshMarketDataCommand} al {@code MessageBus}).
     */
    private Duration refreshInterval = Duration.ofMinutes(5);

    private boolean refreshEnabled = true;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }
}
