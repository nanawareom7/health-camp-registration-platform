package com.healthcamp.registration;

/**
 * Lifecycle states for a registration.
 *
 * <p>Transitions implemented in this MVP:
 * <ul>
 *   <li>{@code PENDING} (initial, set on registration) -> {@code APPROVED}</li>
 *   <li>{@code PENDING} -> {@code REJECTED}</li>
 * </ul>
 * {@code APPROVED} and {@code REJECTED} are terminal. A {@code CONFIRMED} state
 * (approval -> confirmation) is planned but not implemented.
 */
public enum RegistrationStatus {
    PENDING,
    APPROVED,
    REJECTED;

    /**
     * Parses a status string case-insensitively.
     *
     * @param value the status string (must not be {@code null} or blank)
     * @return the matching status
     * @throws IllegalArgumentException if the value is not a known status
     */
    public static RegistrationStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status must not be null or blank");
        }
        return RegistrationStatus.valueOf(value.trim().toUpperCase());
    }

    /**
     * @return {@code true} if this status is a valid target for a status update
     *         (an organizer decision: {@code APPROVED} or {@code REJECTED});
     *         {@code false} otherwise (e.g. {@code PENDING}).
     */
    public boolean isDecisionTarget() {
        return this == APPROVED || this == REJECTED;
    }
}
