package io.github.ryugurenachopper.texthospital.balance.dto;

import java.util.List;

public record CategoryAnalysis(
        int totalOccurrences,
        int categoryCount,
        double entropy,
        Double normalizedEntropy,
        boolean applicable,
        Double imbalanceRatio,
        Double gini,
        Boolean significantlyImbalanced,
        String reason,
        String thresholdMode,
        double imbalanceRatioThreshold,
        double giniThreshold,
        List<TypeCount> countsAscending
) {
    public CategoryAnalysis {
        countsAscending = List.copyOf(countsAscending);
    }
}
