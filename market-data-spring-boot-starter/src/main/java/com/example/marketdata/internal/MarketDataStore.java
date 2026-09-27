package com.example.marketdata.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.contractbus.ContractBus;
import com.example.marketdata.AssetNotFoundException;
import com.example.marketdata.MarketDataRefreshed;
import com.example.marketdata.PricePoint;

import jakarta.annotation.PostConstruct;

/**
 * Not a {@code @Component}: registered by {@link MarketDataAutoConfiguration},
 * since host apps don't component-scan starter packages.
 */
class MarketDataStore {

    private static final Logger log = LoggerFactory.getLogger(MarketDataStore.class);

    private final ResourcePatternResolver resourceResolver;
    private final ObjectMapper objectMapper;
    private final MarketDataProperties properties;
    private final ContractBus contractBus;

    private volatile Map<String, List<PricePoint>> pricesByAsset = Map.of();

    MarketDataStore(ResourcePatternResolver resourceResolver,
                     ObjectMapper objectMapper,
                     MarketDataProperties properties,
                     ContractBus contractBus) {
        this.resourceResolver = resourceResolver;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.contractBus = contractBus;
    }

    // No Info broadcast here: broadcasting would index every handler, including those that
    // depend on this bean while it is still being created (a startup cycle).
    @PostConstruct
    void init() {
        load();
    }

    @Scheduled(fixedDelayString = "${market-data.refresh-interval:PT5M}",
            initialDelayString = "${market-data.refresh-interval:PT5M}")
    void scheduledRefresh() {
        if (properties.isRefreshEnabled()) {
            doRefresh();
        }
    }

    void refresh() {
        doRefresh();
    }

    List<PricePoint> findPrices(String assetSymbol, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' (%s) must not be after 'to' (%s)".formatted(from, to));
        }
        String key = normalize(assetSymbol);
        List<PricePoint> all = pricesByAsset.get(key);
        if (all == null) {
            throw new AssetNotFoundException(assetSymbol);
        }
        return all.stream()
                .filter(point -> !point.date().isBefore(from) && !point.date().isAfter(to))
                .toList();
    }

    Set<String> availableAssets() {
        return pricesByAsset.keySet();
    }

    private synchronized void doRefresh() {
        Set<String> assets = load();
        contractBus.broadcast(new MarketDataRefreshed(assets, Instant.now()));
    }

    private synchronized Set<String> load() {
        Map<String, List<PricePoint>> loaded = loadFromDisk();
        this.pricesByAsset = loaded;
        log.info("Market data (re)loaded from '{}' for assets: {}", properties.getDirectory(), loaded.keySet());
        return loaded.keySet();
    }

    private Map<String, List<PricePoint>> loadFromDisk() {
        String pattern = normalizeDirectory(properties.getDirectory()) + "*.json";
        try {
            Resource[] resources = resourceResolver.getResources(pattern);
            if (resources.length == 0) {
                log.warn("No market data JSON files found matching '{}'; check the 'market-data.directory' property", pattern);
            }
            Map<String, List<PricePoint>> result = new LinkedHashMap<>();
            for (Resource resource : resources) {
                JsonAssetPriceFile file = readFile(resource);
                String symbol = normalize(file.asset());
                List<PricePoint> points = file.prices().stream()
                        .map(p -> new PricePoint(p.date(), p.price()))
                        .sorted(Comparator.comparing(PricePoint::date))
                        .toList();
                result.put(symbol, points);
            }
            return Collections.unmodifiableMap(result);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to list market data files in '" + properties.getDirectory() + "'", e);
        }
    }

    private JsonAssetPriceFile readFile(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, JsonAssetPriceFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read market data file '" + resource.getFilename() + "'", e);
        }
    }

    private static String normalize(String assetSymbol) {
        return assetSymbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeDirectory(String directory) {
        return directory.endsWith("/") ? directory : directory + "/";
    }
}
