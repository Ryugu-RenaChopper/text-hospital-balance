package io.github.ryugurenachopper.texthospital.balance.metric;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizedEntropyCalculatorTest {
    private final NormalizedEntropyCalculator calculator = new NormalizedEntropyCalculator();

    @Test
    void equalCountsHaveMaximumNormalizedEntropy() {
        assertEquals(Math.log(3) / Math.log(2), calculator.shannonEntropy(List.of(2, 2, 2)), 1e-12);
        assertEquals(1.0, calculator.calculate(List.of(2, 2, 2)), 1e-12);
    }

    @Test
    void skewedCountsHaveLowerNormalizedEntropy() {
        Double result = calculator.calculate(List.of(10, 1, 1));
        assertTrue(result != null && result > 0.0 && result < 1.0);
    }

    @Test
    void fewerThanTwoPositiveCategoriesIsNotApplicable() {
        assertNull(calculator.calculate(List.of(0, 4, 0)));
        assertNull(calculator.calculate(List.of()));
    }

    @Test
    void negativeCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(List.of(2, -1)));
    }
}
