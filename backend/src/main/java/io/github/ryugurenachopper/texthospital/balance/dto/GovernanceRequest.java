package io.github.ryugurenachopper.texthospital.balance.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record GovernanceRequest(
        JsonNode dataset,
        String targetKind,
        String targetType,
        Integer targetCount,
        Long randomSeed,
        Double imbalanceRatioThreshold,
        Double giniThreshold,
        String thresholdMode
) {
}
