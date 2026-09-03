package io.github.ryugurenachopper.texthospital.balance.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ryugurenachopper.texthospital.balance.dto.GovernanceRequest;
import io.github.ryugurenachopper.texthospital.balance.metric.ImbalanceMetricCalculator;
import io.github.ryugurenachopper.texthospital.balance.metric.NormalizedEntropyCalculator;
import io.github.ryugurenachopper.texthospital.balance.service.BalanceAnalysisService;
import io.github.ryugurenachopper.texthospital.balance.service.DatasetStatistics;
import io.github.ryugurenachopper.texthospital.balance.service.OptionalDistributionAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomResamplingServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private RandomResamplingService service;

    @BeforeEach
    void setUp() {
        DatasetStatistics statistics = new DatasetStatistics();
        NormalizedEntropyCalculator entropy = new NormalizedEntropyCalculator();
        BalanceAnalysisService analysis = new BalanceAnalysisService(
                statistics,
                new ImbalanceMetricCalculator(),
                entropy,
                new OptionalDistributionAnalyzer(entropy),
                10,
                0.4,
                "AND"
        );
        service = new RandomResamplingService(statistics, analysis, mapper);
    }

    @Test
    void resamplingDeepCopiesWholeSamplesAndReportsOvershoot() throws Exception {
        JsonNode input = mapper.readTree("""
                [
                  {"id":"s1","text":"synthetic","source":"a","materialSystem":"alpha",
                   "ner":[[0,1,"x","RARE"],[2,3,"y","RARE"]],
                   "relations":[[0,1,2,3,"RELATED"]]},
                  {"id":"s2","ner":[[0,1,"z","COMMON"]],"relations":[]}
                ]
                """);
        String original = input.toString();
        var result = service.govern(request(input, "ENTITY", "RARE", 3, 7L));

        assertEquals("COMPLETED", result.status());
        assertEquals(4, result.actualTargetCount());
        assertEquals(1, result.overshoot());
        assertEquals(1, result.copiedSampleCount());
        assertEquals("a", result.governedDataset().get(2).get("source").asText());
        assertEquals(original, input.toString(), "Input JSON must remain unchanged");
        assertNotSame(input.get(0), result.governedDataset().get(0));
    }

    @Test
    void sameSeedProducesIdenticalOutput() throws Exception {
        JsonNode firstInput = mapper.readTree("""
                [{"id":"a","ner":[[0,1,"a","T"]]},
                 {"id":"b","ner":[[0,1,"b","T"]]}]
                """);
        JsonNode secondInput = firstInput.deepCopy();
        var first = service.govern(request(firstInput, "ENTITY", "T", 6, 99L));
        var second = service.govern(request(secondInput, "ENTITY", "T", 6, 99L));
        assertEquals(first.governedDataset(), second.governedDataset());
    }

    @Test
    void generatedIdsRemainUniqueAndMissingIdGetsSafeFallback() throws Exception {
        JsonNode input = mapper.readTree("""
                [{"id":"s__rs_existing","ner":[[0,1,"a","T"]]},
                 {"ner":[[0,1,"b","T"]]}]
                """);
        var result = service.govern(request(input, "ENTITY", "T", 8, 3L));
        Set<String> ids = new HashSet<>();
        result.governedDataset().forEach(node -> {
            if (node.has("id")) assertTrue(ids.add(node.get("id").asText()));
        });
        assertTrue(result.governedDataset().toString().contains("rs_"));
        assertFalse(input.get(1).has("id"), "Original object without id must not be modified");
    }

    @Test
    void relationTargetUsesRelationLabel() throws Exception {
        JsonNode input = mapper.readTree(
                "[{\"id\":\"r1\",\"ner\":[],\"relations\":[[0,1,2,3,\"RARE_REL\"]]}]");
        var result = service.govern(request(input, "RELATION", "RARE_REL", 2, 11L));
        assertEquals(2, result.after().relation().totalOccurrences());
    }

    @Test
    void reachedTargetReturnsNoChangeWithIndependentOutput() throws Exception {
        JsonNode input = mapper.readTree("[{\"id\":\"s\",\"ner\":[[0,1,\"x\",\"T\"]]}]");
        var result = service.govern(request(input, "ENTITY", "T", 1, 1L));
        assertEquals("NO_CHANGE_NEEDED", result.status());
        assertEquals(0, result.copiedSampleCount());
        assertEquals(input, result.governedDataset());
        assertNotSame(input, result.governedDataset());
    }

    @Test
    void invalidInputIsRejectedWithStableReason() throws Exception {
        JsonNode input = mapper.readTree("[{\"id\":\"s\",\"ner\":[]}] ");
        assertEquals("invalid_target_kind", assertThrows(RandomResamplingException.class,
                () -> service.govern(request(input, "SOURCE", "T", 2, 1L))).getReason());
        assertEquals("invalid_target_count", assertThrows(RandomResamplingException.class,
                () -> service.govern(request(input, "ENTITY", "T", 0, 1L))).getReason());
        assertEquals("target_type_not_found", assertThrows(RandomResamplingException.class,
                () -> service.govern(request(input, "ENTITY", "MISSING", 2, 1L))).getReason());
    }

    private GovernanceRequest request(
            JsonNode dataset,
            String kind,
            String type,
            int targetCount,
            Long seed
    ) {
        return new GovernanceRequest(dataset, kind, type, targetCount, seed, 10.0, 0.4, "AND");
    }
}
