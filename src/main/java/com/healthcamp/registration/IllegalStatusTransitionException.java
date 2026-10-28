package com.healthcamp.registration;

/**
 * Thrown when a requested status transition is not allowed from the current state
 * (e.g. approving a registration that is already {@code APPROVED} or {@code REJECTED}).
 * Mapped to HTTP {@code 409 Conflict} by {@code GlobalExceptionHandler}.
 */
public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(Long id, String current, String target) {
        super("Cannot transition registration " + id + " from " + current + " to " + target);
    }
}
