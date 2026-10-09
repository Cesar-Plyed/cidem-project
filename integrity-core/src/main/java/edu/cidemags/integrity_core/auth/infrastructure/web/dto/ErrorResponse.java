package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, null);
    }

    public static ErrorResponse ofValidation(int status, String error, Map<String, String> fieldErrors) {
        String message = fieldErrors == null || fieldErrors.isEmpty()
                ? "Error de validacion"
                : String.join("; ", fieldErrors.values().stream().distinct().toList());
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, fieldErrors);
    }
}