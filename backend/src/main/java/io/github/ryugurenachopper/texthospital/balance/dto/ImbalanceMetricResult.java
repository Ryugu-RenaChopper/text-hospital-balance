package io.github.ryugurenachopper.texthospital.balance.dto;

import java.util.List;

public record ImbalanceMetricResult(
        boolean applicable,
        Double imbalanceRatio,
        Double gini,
        Boolean significantlyImbalanced,
        int positiveCategoryCount,
        Integer maxCount,
        Integer minPositiveCount,
        String thresholdMode,
        double imbalanceRatioThreshold,
        double giniThreshold,
        String reason,
        List<TypeCount> typeCountsAscending
) {
    public ImbalanceMetricResult {
        typeCountsAscending = List.copyOf(typeCountsAscending);
    }
}
