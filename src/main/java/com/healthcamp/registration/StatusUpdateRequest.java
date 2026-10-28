package com.healthcamp.registration;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for approving/rejecting a registration.
 *
 * <p>{@code status} must be {@code APPROVED} or {@code REJECTED}. An optional
 * rejection reason is planned but not yet supported.
 */
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
