package io.github.ryugurenachopper.texthospital.balance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ryugurenachopper.texthospital.balance.metric.NormalizedEntropyCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OptionalDistributionAnalyzerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final DatasetStatistics statistics = new DatasetStatistics();
    private final OptionalDistributionAnalyzer analyzer =
            new OptionalDistributionAnalyzer(new NormalizedEntropyCalculator());

    @Test
    void completeSourceAndMaterialMappingsAreAnalyzed() throws Exception {
        var samples = statistics.samples(mapper.readTree("""
                [
                  {"source":"a","materialSystem":"alpha"},
                  {"source":"b","material_system":"beta"}
                ]
                """));
        assertEquals("AVAILABLE", analyzer.analyzeSource(samples).status());
        assertEquals(1.0, analyzer.analyzeSource(samples).normalizedEntropy(), 1e-12);
        assertEquals("AVAILABLE", analyzer.analyzeMaterial(samples).status());
    }

    @Test
    void missingOrPartialMappingsAreNotGuessed() throws Exception {
        var missing = statistics.samples(mapper.readTree("[{},{}]"));
        assertEquals("NOT_AVAILABLE", analyzer.analyzeSource(missing).status());
        assertNull(analyzer.analyzeSource(missing).normalizedEntropy());

        var partial = statistics.samples(mapper.readTree("[{\"source\":\"a\"},{}]"));
        assertEquals("UNCOMPUTABLE", analyzer.analyzeSource(partial).status());
        assertEquals(1, analyzer.analyzeSource(partial).missingSampleCount());
    }

    @Test
    void conflictingAliasesAreRejected() throws Exception {
        var samples = statistics.samples(mapper.readTree(
                "[{\"materialSystem\":\"alpha\",\"material_system\":\"beta\"}]"));
        assertEquals("UNCOMPUTABLE", analyzer.analyzeMaterial(samples).status());
        assertEquals("material_mapping_invalid", analyzer.analyzeMaterial(samples).reason());
    }
}
