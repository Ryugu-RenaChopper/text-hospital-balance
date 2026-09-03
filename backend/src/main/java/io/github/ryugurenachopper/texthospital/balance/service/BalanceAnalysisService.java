package io.github.ryugurenachopper.texthospital.balance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ryugurenachopper.texthospital.balance.dto.AnalysisResponse;
import io.github.ryugurenachopper.texthospital.balance.dto.CategoryAnalysis;
import io.github.ryugurenachopper.texthospital.balance.dto.ImbalanceMetricResult;
import io.github.ryugurenachopper.texthospital.balance.metric.ImbalanceMetricCalculator;
import io.github.ryugurenachopper.texthospital.balance.metric.NormalizedEntropyCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BalanceAnalysisService {
    private final DatasetStatistics statistics;
    private final ImbalanceMetricCalculator metricCalculator;
    private final NormalizedEntropyCalculator entropyCalculator;
    private final OptionalDistributionAnalyzer distributionAnalyzer;
    private final double defaultImbalanceRatioThreshold;
    private final double defaultGiniThreshold;
    private final String defaultThresholdMode;

    public BalanceAnalysisService(
            DatasetStatistics statistics,
            ImbalanceMetricCalculator metricCalculator,
            NormalizedEntropyCalculator entropyCalculator,
            OptionalDistributionAnalyzer distributionAnalyzer,
            @Value("${balance.thresholds.imbalance-ratio:10.0}") double defaultImbalanceRatioThreshold,
            @Value("${balance.thresholds.gini:0.4}") double defaultGiniThreshold,
            @Value("${balance.thresholds.mode:AND}") String defaultThresholdMode
    ) {
        this.statistics = statistics;
        this.metricCalculator = metricCalculator;
        this.entropyCalculator = entropyCalculator;
        this.distributionAnalyzer = distributionAnalyzer;
        this.defaultImbalanceRatioThreshold = defaultImbalanceRatioThreshold;
        this.defaultGiniThreshold = defaultGiniThreshold;
        this.defaultThresholdMode = defaultThresholdMode;
    }

    public AnalysisResponse analyze(JsonNode dataset) {
        return analyze(dataset, null, null, null);
    }

    public AnalysisResponse analyze(
            JsonNode dataset,
            Double imbalanceRatioThreshold,
            Double giniThreshold,
            String thresholdMode
    ) {
        double effectiveIr = imbalanceRatioThreshold == null
                ? defaultImbalanceRatioThreshold : imbalanceRatioThreshold;
        double effectiveGini = giniThreshold == null ? defaultGiniThreshold : giniThreshold;
        String effectiveMode = thresholdMode == null || thresholdMode.isBlank()
                ? defaultThresholdMode : thresholdMode;
        List<ObjectNode> samples = statistics.samples(dataset);
        Map<String, Integer> entityCounts = statistics.countLabels(samples, "ENTITY");
        Map<String, Integer> relationCounts = statistics.countLabels(samples, "RELATION");
        return new AnalysisResponse(
                samples.size(),
                categoryAnalysis(entityCounts, effectiveIr, effectiveGini, effectiveMode),
                categoryAnalysis(relationCounts, effectiveIr, effectiveGini, effectiveMode),
                distributionAnalyzer.analyzeSource(samples),
                distributionAnalyzer.analyzeMaterial(samples)
        );
    }

    private CategoryAnalysis categoryAnalysis(
            Map<String, Integer> counts,
            double imbalanceRatioThreshold,
            double giniThreshold,
            String thresholdMode
    ) {
        ImbalanceMetricResult metric = metricCalculator.calculate(
                counts, imbalanceRatioThreshold, giniThreshold, thresholdMode);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        return new CategoryAnalysis(
                total,
                metric.positiveCategoryCount(),
                entropyCalculator.shannonEntropy(counts.values()),
                entropyCalculator.calculate(counts.values()),
                metric.applicable(),
                metric.imbalanceRatio(),
                metric.gini(),
                metric.significantlyImbalanced(),
                metric.reason(),
                metric.thresholdMode(),
                metric.imbalanceRatioThreshold(),
                metric.giniThreshold(),
                metric.typeCountsAscending()
        );
    }
}
