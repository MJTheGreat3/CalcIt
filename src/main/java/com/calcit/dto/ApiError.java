
package com.calcit.dto;

public record ApiError(
    int status,
    String error,
    String message
) {}
