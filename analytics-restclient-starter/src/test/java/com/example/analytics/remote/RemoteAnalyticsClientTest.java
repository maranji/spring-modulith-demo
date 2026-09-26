package com.example.analytics.remote;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.analytics.InsufficientDataException;
import com.example.analytics.PriceStatistics;
import com.example.marketdata.AssetNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteAnalyticsClientTest {

    private static final String BASE_URL = "http://analytics.example.test";

    private MockRestServiceServer server;
    private RemoteAnalyticsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RemoteAnalyticsClient(builder.build());
    }

    @Test
    void fetchesAverageFromRemoteService() {
        server.expect(requestTo(BASE_URL + "/api/assets/AAPL/statistics/average?from=2026-01-01&to=2026-01-31"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"asset":"AAPL","from":"2026-01-01","to":"2026-01-31","type":"AVERAGE","value":15.0,"sampleSize":2}
                        """, MediaType.APPLICATION_JSON));

        PriceStatistics result = client.average("AAPL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(result.asset()).isEqualTo("AAPL");
        assertThat(result.value()).isEqualTo(15.0);
        assertThat(result.sampleSize()).isEqualTo(2);
        server.verify();
    }

    @Test
    void translatesNotFoundIntoAssetNotFoundException() {
        server.expect(requestTo(BASE_URL + "/api/assets/UNKNOWN/statistics/average?from=2026-01-01&to=2026-01-31"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.average("UNKNOWN", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void translatesUnprocessableEntityIntoInsufficientDataException() {
        server.expect(requestTo(BASE_URL + "/api/assets/AAPL/statistics/average?from=2020-01-01&to=2020-01-02"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.average("AAPL", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)))
                .isInstanceOf(InsufficientDataException.class);
    }
}
