package com.healthcamp.registration;

/**
 * Thrown when a registration lookup or update targets a non-existent registration.
 * Mapped to HTTP {@code 404 Not Found} by {@code GlobalExceptionHandler}.
 */
public class RegistrationNotFoundException extends RuntimeException {

    public RegistrationNotFoundException(Long id) {
        super("Registration not found: " + id);
    }
}
