package com.healthcamp.registration;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST endpoint for health camp registration.
 *
 * <p>POST {@code /api/v1/registrations} accepts the registration form, validates it,
 * persists it, and returns {@code 201 Created} with the saved registration.
 */
@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = service.register(request);
        URI location = URI.create("/api/v1/registrations/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }
}
