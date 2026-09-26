package com.example.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PriceStatisticsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAvailableAssets() throws Exception {
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("AAPL", "MSFT")));
    }

    @Test
    void returnsAverageForKnownAsset() throws Exception {
        mockMvc.perform(get("/api/assets/AAPL/statistics/average")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asset").value("AAPL"))
                .andExpect(jsonPath("$.type").value("AVERAGE"))
                .andExpect(jsonPath("$.sampleSize").value(11));
    }

    @Test
    void returnsStandardDeviationForKnownAsset() throws Exception {
        mockMvc.perform(get("/api/assets/MSFT/statistics/standard-deviation")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asset").value("MSFT"))
                .andExpect(jsonPath("$.type").value("STANDARD_DEVIATION"));
    }

    @Test
    void returns404ForUnknownAsset() throws Exception {
        mockMvc.perform(get("/api/assets/UNKNOWN/statistics/average")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400WhenFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/api/assets/AAPL/statistics/average")
                        .param("from", "2026-02-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns422WhenNoDataInRange() throws Exception {
        mockMvc.perform(get("/api/assets/AAPL/statistics/average")
                        .param("from", "2020-01-01")
                        .param("to", "2020-01-02"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void manualRefreshIsAccepted() throws Exception {
        mockMvc.perform(post("/api/assets/refresh"))
                .andExpect(status().isAccepted());
    }
}
