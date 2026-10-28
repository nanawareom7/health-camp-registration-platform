package com.healthcamp.registration;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration operations: create (status {@code PENDING}), list (with an optional
 * status filter), get-by-id, and approve/reject with validated status transitions.
 *
 * <p>Only a {@code PENDING} registration can be transitioned; {@code APPROVED} and
 * {@code REJECTED} are terminal. Unknown status values are rejected as invalid input.
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
        registration.setStatus(RegistrationStatus.PENDING.name());
        registration.setCreatedAt(Instant.now());

        Registration saved = repository.save(registration);
        return RegistrationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> list(String status) {
        List<Registration> registrations;
        if (status == null || status.isBlank()) {
            registrations = repository.findAllByOrderByIdAsc();
        } else {
            RegistrationStatus filter = parseStatus(status);
            registrations = repository.findByStatusOrderByIdAsc(filter.name());
        }
        return registrations.stream().map(RegistrationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationResponse get(Long id) {
        Registration registration = repository.findById(id)
                .orElseThrow(() -> new RegistrationNotFoundException(id));
        return RegistrationResponse.from(registration);
    }

    @Transactional
    public RegistrationResponse updateStatus(Long id, StatusUpdateRequest request) {
        Registration registration = repository.findById(id)
                .orElseThrow(() -> new RegistrationNotFoundException(id));

        RegistrationStatus target = parseStatus(request.getStatus());
        if (!target.isDecisionTarget()) {
            // Only APPROVED/REJECTED are valid update targets (PENDING is not).
            throw new InvalidStatusException(request.getStatus());
        }
        if (!RegistrationStatus.PENDING.name().equals(registration.getStatus())) {
            // Only a PENDING registration can be approved/rejected; terminal states are final.
            throw new IllegalStatusTransitionException(id, registration.getStatus(), target.name());
        }

        registration.setStatus(target.name());
        Registration saved = repository.save(registration);
        return RegistrationResponse.from(saved);
    }

    private RegistrationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidStatusException(value);
        }
        try {
            return RegistrationStatus.from(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException(value);
        }
    }
}
