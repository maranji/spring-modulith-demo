package com.example.marketdata;

/**
 * Sollevata quando viene richiesto un asset per cui non esiste alcun file
 * di dati di prezzo.
 */
public class AssetNotFoundException extends RuntimeException {

    private final String assetSymbol;

    public AssetNotFoundException(String assetSymbol) {
        super("No market data available for asset '%s'".formatted(assetSymbol));
        this.assetSymbol = assetSymbol;
    }

    public String assetSymbol() {
        return assetSymbol;
    }
}
