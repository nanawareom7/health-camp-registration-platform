package com.healthcamp.registration;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for health camp registration.
 *
 * <ul>
 *   <li>{@code POST   /api/v1/registrations} — create a registration (status PENDING).</li>
 *   <li>{@code GET    /api/v1/registrations[?status=]} — list registrations, optionally
 *       filtered by status.</li>
 *   <li>{@code GET    /api/v1/registrations/{id}} — get a single registration (status tracking).</li>
 *   <li>{@code PATCH  /api/v1/registrations/{id}/status} — approve or reject a registration.</li>
 * </ul>
 *
 * <p>The listing, get-by-id and status-update endpoints are organizer/admin operations.
 * They are currently <b>unprotected</b> (no authentication/authorization) and are
 * <b>demo-only</b> — see the README and {@code docs/api-documentation.md}.
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

    @GetMapping
    public List<RegistrationResponse> list(@RequestParam(name = "status", required = false) String status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public RegistrationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public RegistrationResponse updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(id, request);
    }
}
