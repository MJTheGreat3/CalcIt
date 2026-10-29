
package com.calcit.dto;

public record CalculationResponse(
    String operation,
    double a,
    double b,
    double result
) {}
