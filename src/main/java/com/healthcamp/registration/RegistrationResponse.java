package com.healthcamp.registration;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Registration confirmation returned after a successful submission. Immutable.
 */
public class RegistrationResponse {

    private final Long id;
    private final String fullName;
    private final String email;
    private final String phone;
    private final LocalDate preferredCampDate;
    private final String status;
    private final Instant createdAt;

    public RegistrationResponse(Long id, String fullName, String email, String phone,
                                LocalDate preferredCampDate, String status, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.preferredCampDate = preferredCampDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static RegistrationResponse from(Registration reg) {
        return new RegistrationResponse(
                reg.getId(),
                reg.getFullName(),
                reg.getEmail(),
                reg.getPhone(),
                reg.getPreferredCampDate(),
                reg.getStatus(),
                reg.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getPreferredCampDate() {
        return preferredCampDate;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
