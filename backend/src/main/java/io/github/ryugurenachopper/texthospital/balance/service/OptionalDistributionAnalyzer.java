package io.github.ryugurenachopper.texthospital.balance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ryugurenachopper.texthospital.balance.dto.DistributionAnalysis;
import io.github.ryugurenachopper.texthospital.balance.dto.TypeCount;
import io.github.ryugurenachopper.texthospital.balance.metric.NormalizedEntropyCalculator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class OptionalDistributionAnalyzer {
    private static final List<String> SOURCE_FIELDS = List.of(
            "source", "sourcePdf", "source_pdf", "literatureSourceId", "literature_source_id");
    private static final List<String> MATERIAL_FIELDS = List.of("materialSystem", "material_system");
    private static final Set<String> PLACEHOLDERS = Set.of("", "unknown", "none", "null", "n/a", "?");

    private final NormalizedEntropyCalculator entropyCalculator;

    public OptionalDistributionAnalyzer(NormalizedEntropyCalculator entropyCalculator) {
        this.entropyCalculator = entropyCalculator;
    }

    public DistributionAnalysis analyzeSource(List<ObjectNode> samples) {
        return analyze(samples, SOURCE_FIELDS, "source");
    }

    public DistributionAnalysis analyzeMaterial(List<ObjectNode> samples) {
        return analyze(samples, MATERIAL_FIELDS, "material");
    }

    private DistributionAnalysis analyze(List<ObjectNode> samples, List<String> aliases, String label) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int missing = 0;
        for (ObjectNode sample : samples) {
            FieldValue fieldValue = readAliasedValue(sample, aliases);
            if (fieldValue.invalid()) {
                return result("UNCOMPUTABLE", false, label + "_mapping_invalid",
                        samples.size(), counts, missing + 1);
            }
            if (fieldValue.value() == null) {
                missing++;
            } else {
                counts.merge(fieldValue.value(), 1, Integer::sum);
            }
        }

        if (counts.isEmpty()) {
            return result("NOT_AVAILABLE", false, label + "_mapping_missing",
                    samples.size(), counts, samples.size());
        }
        if (missing > 0) {
            return result("UNCOMPUTABLE", false, label + "_mapping_incomplete",
                    samples.size(), counts, missing);
        }
        if (counts.size() == 1) {
            return result("NOT_APPLICABLE", false, "single_category",
                    samples.size(), counts, 0);
        }
        return result("AVAILABLE", true, label + "_distribution_available",
                samples.size(), counts, 0);
    }

    private DistributionAnalysis result(
            String status,
            boolean applicable,
            String reason,
            int sampleCount,
            Map<String, Integer> counts,
            int missing
    ) {
        int mapped = counts.values().stream().mapToInt(Integer::intValue).sum();
        double entropy = entropyCalculator.shannonEntropy(counts.values());
        Double normalized = entropyCalculator.calculate(counts.values());
        return new DistributionAnalysis(status, applicable, reason, sampleCount, mapped,
                missing, entropy, normalized, sorted(counts));
    }

    private FieldValue readAliasedValue(ObjectNode sample, List<String> aliases) {
        List<String> values = new ArrayList<>();
        for (String alias : aliases) {
            if (!sample.has(alias)) {
                continue;
            }
            JsonNode value = sample.get(alias);
            if (value == null || value.isNull()) {
                continue;
            }
            if (!value.isValueNode()) {
                return new FieldValue(null, true);
            }
            String normalized = normalize(value.asText());
            if (normalized != null && !values.contains(normalized)) {
                values.add(normalized);
            }
        }
        return values.size() > 1
                ? new FieldValue(null, true)
                : new FieldValue(values.isEmpty() ? null : values.get(0), false);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return PLACEHOLDERS.contains(normalized.toLowerCase(Locale.ROOT)) ? null : normalized;
    }

    private List<TypeCount> sorted(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .map(entry -> new TypeCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(TypeCount::count).thenComparing(TypeCount::type))
                .toList();
    }

    private record FieldValue(String value, boolean invalid) {
    }
}
