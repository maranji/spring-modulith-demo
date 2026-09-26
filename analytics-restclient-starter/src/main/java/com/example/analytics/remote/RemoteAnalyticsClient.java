package com.example.analytics.remote;

import java.time.LocalDate;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.example.analytics.InsufficientDataException;
import com.example.analytics.PriceStatistics;
import com.example.analytics.StatisticType;
import com.example.marketdata.AssetNotFoundException;

/**
 * Translates HTTP 404/422 into the same domain exceptions as the local
 * implementation, so callers can't tell the two apart.
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
