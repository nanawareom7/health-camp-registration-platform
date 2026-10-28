package com.healthcamp.registration;

/**
 * Thrown when a status value is not a recognized/allowed status (e.g. an unknown
 * string, or {@code PENDING} used as an update target). Mapped to HTTP
 * {@code 400 Bad Request} by {@code GlobalExceptionHandler}.
 */
public class InvalidStatusException extends RuntimeException {

    public InvalidStatusException(String value) {
        super("Invalid status: '" + value + "'. Allowed values: APPROVED, REJECTED");
    }
}
