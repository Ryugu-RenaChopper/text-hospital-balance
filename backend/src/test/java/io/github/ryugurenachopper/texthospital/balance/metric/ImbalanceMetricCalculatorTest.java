package io.github.ryugurenachopper.texthospital.balance.metric;

import io.github.ryugurenachopper.texthospital.balance.dto.ImbalanceMetricResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImbalanceMetricCalculatorTest {
    private final ImbalanceMetricCalculator calculator = new ImbalanceMetricCalculator();

    @Test
    void computesImbalanceRatioAndGini() {
        ImbalanceMetricResult result = calculator.calculate(Map.of("major", 10, "minor", 1), 10, 0.4, "AND");
        assertEquals(10.0, result.imbalanceRatio(), 1e-12);
        assertEquals(0.40909090909090917, result.gini(), 1e-12);
        assertTrue(result.significantlyImbalanced());
    }

    @Test
    void supportsAndAndOrThresholdStrategies() {
        Map<String, Integer> counts = Map.of("major", 10, "minor", 1);
        assertFalse(calculator.calculate(counts, 10, 0.5, "AND").significantlyImbalanced());
        assertTrue(calculator.calculate(counts, 10, 0.5, "OR").significantlyImbalanced());
    }

    @Test
    void outputIsSortedByCountThenType() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("z", 2);
        counts.put("b", 1);
        counts.put("a", 1);
        var result = calculator.calculate(counts, 10, 0.4, "AND");
        assertEquals("a", result.typeCountsAscending().get(0).type());
        assertEquals("b", result.typeCountsAscending().get(1).type());
    }

    @Test
    void singleCategoryIsNotApplicable() {
        ImbalanceMetricResult result = calculator.calculate(Map.of("only", 3), 10, 0.4, "AND");
        assertFalse(result.applicable());
        assertNull(result.imbalanceRatio());
        assertNull(result.significantlyImbalanced());
    }

    @Test
    void rejectsInvalidModeAndThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Map.of("a", 1, "b", 2), 10, 0.4, "XOR"));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Map.of("a", 1, "b", 2), 10, 1.1, "AND"));
    }
}
