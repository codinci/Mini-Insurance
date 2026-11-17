package com.example.miniinsurance.common;

import java.time.Instant;
import java.util.List;

/** Consistent error body returned by {@link ApiExceptionHandler}. */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> fieldErrors
) {

    public record FieldViolation(String field, String message) {
    }
}
