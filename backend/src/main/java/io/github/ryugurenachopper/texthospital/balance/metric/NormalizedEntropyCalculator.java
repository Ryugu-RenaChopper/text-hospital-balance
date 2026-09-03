package io.github.ryugurenachopper.texthospital.balance.metric;

import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class NormalizedEntropyCalculator {
    public double shannonEntropy(Collection<Integer> counts) {
        if (counts == null) {
            throw new IllegalArgumentException("Category counts must not be null");
        }
        long total = validateAndTotal(counts);
        if (total == 0) {
            return 0.0;
        }
        double entropy = 0.0;
        for (Integer count : counts) {
            if (count == null || count == 0) {
                continue;
            }
            double probability = (double) count / total;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }

    public Double calculate(Collection<Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return null;
        }
        validateAndTotal(counts);
        int positiveCategories = (int) counts.stream()
                .filter(count -> count != null && count > 0)
                .count();
        if (positiveCategories <= 1) {
            return null;
        }
        double normalized = shannonEntropy(counts) / (Math.log(positiveCategories) / Math.log(2));
        return Math.max(0.0, Math.min(1.0, normalized));
    }

    private long validateAndTotal(Collection<Integer> counts) {
        long total = 0;
        for (Integer count : counts) {
            if (count == null) {
                continue;
            }
            if (count < 0) {
                throw new IllegalArgumentException("Category count must not be negative");
            }
            total += count;
        }
        return total;
    }
}
