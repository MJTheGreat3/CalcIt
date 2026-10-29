
package com.calcit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private CalculatorService calculatorService;

    @BeforeEach
    void setUp() {
        calculatorService = new CalculatorService();
    }

    @Test
    void shouldAddTwoNumbers() {
        assertEquals(5.0, calculatorService.add(2.0, 3.0), 1e-9);
    }

    @Test
    void shouldSubtractTwoNumbers() {
        assertEquals(5.0, calculatorService.subtract(7.0, 2.0), 1e-9);
    }

    @Test
    void shouldMultiplyTwoNumbers() {
        assertEquals(12.0, calculatorService.multiply(4.0, 3.0), 1e-9);
    }

    @Test
    void shouldDivideTwoNumbers() {
        assertEquals(5.0, calculatorService.divide(10.0, 2.0), 1e-9);
    }

    @Test
    void shouldSupportFractionalResults() {
        assertEquals(2.5, calculatorService.divide(10.0, 4.0), 1e-9);
    }

    @Test
    void shouldHandleNegativeNumbers() {
        assertEquals(-5.0, calculatorService.add(-2.0, -3.0), 1e-9);
    }

    @Test
    void shouldRejectDivisionByZero() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculatorService.divide(10.0, 0.0)
        );

        assertEquals(
            "Division by zero is not allowed",
            exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonFiniteInputs() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculatorService.add(Double.NaN, 1.0)
        );

        assertEquals(
            "Parameters must be finite numbers",
            exception.getMessage()
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> calculatorService.multiply(1.0, Double.POSITIVE_INFINITY)
        );
    }
}
