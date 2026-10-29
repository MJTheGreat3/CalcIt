
package com.calcit.service;

import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    public double add(double a, double b) {
        requireFinite(a, b);
        return a + b;
    }

    public double subtract(double a, double b) {
        requireFinite(a, b);
        return a - b;
    }

    public double multiply(double a, double b) {
        requireFinite(a, b);
        return a * b;
    }

    public double divide(double a, double b) {
        requireFinite(a, b);
        if (b == 0.0) {
            throw new IllegalArgumentException(
                "Division by zero is not allowed"
            );
        }

        return a / b;
    }

    private void requireFinite(double a, double b) {
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new IllegalArgumentException(
                "Parameters must be finite numbers"
            );
        }
    }
}
