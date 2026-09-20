package com.example.analytics.remote;

import java.time.LocalDate;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.example.analytics.InsufficientDataException;
import com.example.analytics.PriceStatistics;
import com.example.analytics.StatisticType;
import com.example.marketdata.AssetNotFoundException;

/**
 * Chiama un servizio Analytics esterno via HTTP, invece di calcolare
 * in-process. Chiama esattamente lo stesso contratto REST già esposto da
 * {@code PriceStatisticsController} nel modulo "app": se in futuro il
 * modulo Analytics (con la sua dipendenza da Market Data) viene estratto
 * come servizio indipendente, questa classe (avvolta dai due
 * {@code MessageHandler} di questo package) è l'adapter lato client che lo
 * sostituisce nell'applicazione originale.
 *
 * <p>Traduce gli errori HTTP (404, 422) nelle stesse eccezioni di dominio
 * sollevate dall'implementazione locale, in modo che il comportamento sia
 * indistinguibile per chi chiama.
 */
class RemoteAnalyticsClient {

    private final RestClient restClient;

    RemoteAnalyticsClient(RestClient restClient) {
        this.restClient = restClient;
    }

    PriceStatistics average(String asset, LocalDate from, LocalDate to) {
        return fetch(asset, from, to, "average", StatisticType.AVERAGE);
    }

    PriceStatistics standardDeviation(String asset, LocalDate from, LocalDate to) {
        return fetch(asset, from, to, "standard-deviation", StatisticType.STANDARD_DEVIATION);
    }

    private PriceStatistics fetch(String asset, LocalDate from, LocalDate to, String operation, StatisticType type) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/assets/{symbol}/statistics/" + operation)
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .build(asset))
                    .retrieve()
                    .body(PriceStatistics.class);
        } catch (HttpClientErrorException e) {
            throw translate(e, asset, from, to, type);
        }
    }

    private RuntimeException translate(HttpClientErrorException e, String asset, LocalDate from, LocalDate to,
                                        StatisticType type) {
        int status = e.getStatusCode().value();
        if (status == 404) {
            return new AssetNotFoundException(asset);
        }
        if (status == 422) {
            return new InsufficientDataException(asset, from, to, type);
        }
        return e;
    }
}
