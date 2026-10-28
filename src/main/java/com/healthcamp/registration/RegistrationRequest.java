package com.healthcamp.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Inbound registration form: full name, email, phone, and preferred camp date.
 *
 * <p>Validated with Bean Validation before reaching the service. Jackson deserializes
 * the JSON request using the default constructor and setters.
 */
public class RegistrationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9][0-9\\- ]{6,19}$",
            message = "Phone must be 7-20 digits; hyphens and spaces allowed; optional leading '+'")
    private String phone;

    @NotNull(message = "Preferred camp date is required")
    @Future(message = "Preferred camp date must be a future date")
    private LocalDate preferredCampDate;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getPreferredCampDate() {
        return preferredCampDate;
    }

    public void setPreferredCampDate(LocalDate preferredCampDate) {
        this.preferredCampDate = preferredCampDate;
    }
}
