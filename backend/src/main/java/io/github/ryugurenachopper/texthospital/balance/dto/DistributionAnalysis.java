package io.github.ryugurenachopper.texthospital.balance.dto;

import java.util.List;

public record DistributionAnalysis(
        String status,
        boolean applicable,
        String reason,
        int sampleCount,
        int mappedSampleCount,
        int missingSampleCount,
        double entropy,
        Double normalizedEntropy,
        List<TypeCount> countsAscending
) {
    public DistributionAnalysis {
        countsAscending = List.copyOf(countsAscending);
    }
}
