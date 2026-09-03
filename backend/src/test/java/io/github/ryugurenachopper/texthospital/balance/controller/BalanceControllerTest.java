package io.github.ryugurenachopper.texthospital.balance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BalanceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyzeAcceptsDatasetDirectly() throws Exception {
        mockMvc.perform(post("/api/balance/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"id":"a","ner":[[0,1,"x","A"]],"relations":[]},
                                  {"id":"b","ner":[[0,1,"y","B"]],"relations":[]}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleCount").value(2))
                .andExpect(jsonPath("$.entity.normalizedEntropy").value(1.0))
                .andExpect(jsonPath("$.entity.imbalanceRatio").value(1.0));
    }

    @Test
    void governReturnsDatasetAndBeforeAfterMetrics() throws Exception {
        mockMvc.perform(post("/api/balance/govern")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset":[{"id":"a","ner":[[0,1,"x","RARE"]],"relations":[]}],
                                  "targetKind":"ENTITY",
                                  "targetType":"RARE",
                                  "targetCount":3,
                                  "randomSeed":7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualTargetCount").value(3))
                .andExpect(jsonPath("$.governedDataset.length()").value(3))
                .andExpect(jsonPath("$.before.entity.totalOccurrences").value(1))
                .andExpect(jsonPath("$.after.entity.totalOccurrences").value(3));
    }

    @Test
    void invalidDatasetReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/api/balance/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("invalid_input"));
    }

    @Test
    void syntheticExampleIsAvailable() throws Exception {
        mockMvc.perform(get("/api/examples/imbalanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("imbalanced-01"));
    }
}
