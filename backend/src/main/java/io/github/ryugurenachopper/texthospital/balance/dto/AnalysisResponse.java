package io.github.ryugurenachopper.texthospital.balance.dto;

public record AnalysisResponse(
        int sampleCount,
        CategoryAnalysis entity,
        CategoryAnalysis relation,
        DistributionAnalysis source,
        DistributionAnalysis material
) {
}
