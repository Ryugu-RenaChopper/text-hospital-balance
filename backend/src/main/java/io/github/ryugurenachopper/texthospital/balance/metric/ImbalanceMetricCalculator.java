package io.github.ryugurenachopper.texthospital.balance.metric;

import io.github.ryugurenachopper.texthospital.balance.dto.ImbalanceMetricResult;
import io.github.ryugurenachopper.texthospital.balance.dto.TypeCount;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ImbalanceMetricCalculator {
    public ImbalanceMetricResult calculate(
            Map<String, Integer> typeCounts,
            double imbalanceRatioThreshold,
            double giniThreshold,
            String thresholdMode
    ) {
        validateThresholds(imbalanceRatioThreshold, giniThreshold);
        String mode = normalizeMode(thresholdMode);
        List<TypeCount> positiveCounts = new ArrayList<>();

        if (typeCounts != null) {
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                Integer count = entry.getValue();
                if (count == null) {
                    continue;
                }
                if (count < 0) {
                    throw new IllegalArgumentException("Type count must not be negative: " + entry.getKey());
                }
                if (count > 0) {
                    positiveCounts.add(new TypeCount(entry.getKey(), count));
                }
            }
        }

        positiveCounts.sort(Comparator.comparingInt(TypeCount::count)
                .thenComparing(TypeCount::type, Comparator.nullsFirst(String::compareTo)));
        int categoryCount = positiveCounts.size();
        if (categoryCount < 2) {
            String reason = categoryCount == 0
                    ? "No positive-frequency category"
                    : "Only one positive-frequency category";
            Integer onlyCount = categoryCount == 0 ? null : positiveCounts.get(0).count();
            return new ImbalanceMetricResult(false, null, null, null, categoryCount,
                    onlyCount, onlyCount, mode, imbalanceRatioThreshold, giniThreshold,
                    reason, positiveCounts);
        }

        int minPositiveCount = positiveCounts.get(0).count();
        int maxCount = positiveCounts.get(categoryCount - 1).count();
        double imbalanceRatio = (double) maxCount / minPositiveCount;
        double gini = calculateGini(positiveCounts);
        boolean significantlyImbalanced = "AND".equals(mode)
                ? imbalanceRatio >= imbalanceRatioThreshold && gini >= giniThreshold
                : imbalanceRatio >= imbalanceRatioThreshold || gini >= giniThreshold;

        return new ImbalanceMetricResult(true, imbalanceRatio, gini, significantlyImbalanced,
                categoryCount, maxCount, minPositiveCount, mode, imbalanceRatioThreshold,
                giniThreshold, null, positiveCounts);
    }

    private double calculateGini(List<TypeCount> positiveCounts) {
        long weightedRankSum = 0L;
        long total = 0L;
        for (int index = 0; index < positiveCounts.size(); index++) {
            int count = positiveCounts.get(index).count();
            weightedRankSum += (long) (index + 1) * count;
            total += count;
        }
        int categoryCount = positiveCounts.size();
        double gini = (2.0 * weightedRankSum) / (categoryCount * (double) total)
                - (categoryCount + 1.0) / categoryCount;
        if (gini < 0.0 && gini > -1e-12) return 0.0;
        if (gini > 1.0 && gini < 1.0 + 1e-12) return 1.0;
        return gini;
    }

    private void validateThresholds(double imbalanceRatioThreshold, double giniThreshold) {
        if (!Double.isFinite(imbalanceRatioThreshold) || imbalanceRatioThreshold < 0) {
            throw new IllegalArgumentException("Imbalance-ratio threshold must be finite and non-negative");
        }
        if (!Double.isFinite(giniThreshold) || giniThreshold < 0 || giniThreshold > 1) {
            throw new IllegalArgumentException("Gini threshold must be within [0, 1]");
        }
    }

    private String normalizeMode(String thresholdMode) {
        String mode = thresholdMode == null ? "AND" : thresholdMode.trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(mode) && !"OR".equals(mode)) {
            throw new IllegalArgumentException("Threshold mode must be AND or OR");
        }
        return mode;
    }
}
