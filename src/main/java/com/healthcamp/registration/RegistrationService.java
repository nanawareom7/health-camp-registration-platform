package com.healthcamp.registration;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and persists a registration with status {@code PENDING}.
 */
@Service
public class RegistrationService {

    private final RegistrationRepository repository;

    public RegistrationService(RegistrationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        Registration registration = new Registration();
        registration.setFullName(request.getFullName());
        registration.setEmail(request.getEmail());
        registration.setPhone(request.getPhone());
        registration.setPreferredCampDate(request.getPreferredCampDate());
        registration.setStatus("PENDING");
        registration.setCreatedAt(Instant.now());

        Registration saved = repository.save(registration);
        return RegistrationResponse.from(saved);
    }
}
