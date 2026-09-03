package io.github.ryugurenachopper.texthospital.balance.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record GovernanceResponse(
        String status,
        String targetKind,
        String targetType,
        int requestedTargetCount,
        int beforeTargetCount,
        int actualTargetCount,
        int overshoot,
        int copiedSampleCount,
        long randomSeed,
        AnalysisResponse before,
        AnalysisResponse after,
        JsonNode governedDataset,
        String warning
) {
}
