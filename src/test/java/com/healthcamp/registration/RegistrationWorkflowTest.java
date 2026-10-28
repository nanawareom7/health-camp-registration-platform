package com.healthcamp.registration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for listing, individual status tracking, and approval/rejection
 * on top of the registration endpoint. Uses the full Spring context with an in-memory
 * H2 database (test profile). Each test runs in a transaction that rolls back.
 *
 * <p>The listing/get/update endpoints are unprotected (demo-only) organizer/admin
 * endpoints; these tests exercise their HTTP contract only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationRepository repository;

    /** Seeds a registration directly in the repository and returns its generated id. */
    private Long seed(String status) {
        Registration reg = new Registration();
        reg.setFullName("Jane Doe");
        reg.setEmail("jane@example.com");
        reg.setPhone("+919876543210");
        reg.setPreferredCampDate(LocalDate.now().plusDays(7));
        reg.setStatus(status);
        reg.setCreatedAt(Instant.now());
        return repository.save(reg).getId();
    }

    // ---------------- Listing ----------------

    @Test
    void listAll_returnsAllRegistrationsOrdered() throws Exception {
        seed("PENDING");
        seed("APPROVED");
        seed("REJECTED");

        mockMvc.perform(get("/api/v1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", hasItems("PENDING", "APPROVED", "REJECTED")));
    }

    @Test
    void listByStatus_filtersByStatus() throws Exception {
        seed("PENDING");
        seed("APPROVED");
        seed("APPROVED");

        mockMvc.perform(get("/api/v1/registrations").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[1].status").value("APPROVED"));
    }

    @Test
    void listByStatus_emptyResult_whenNoMatch() throws Exception {
        seed("PENDING");

        mockMvc.perform(get("/api/v1/registrations").param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listByInvalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/registrations").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------------- Individual status tracking ----------------

    @Test
    void getById_returnsRegistration() throws Exception {
        Long id = seed("PENDING");

        mockMvc.perform(get("/api/v1/registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/registrations/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------------- Approval / rejection ----------------

    @Test
    void approvePending_returns200AndPersistsApproved() throws Exception {
        Long id = seed("PENDING");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Persistent status update verified directly against the repository.
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void rejectPending_returns200AndPersistsRejected() throws Exception {
        Long id = seed("PENDING");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void approveAlreadyApproved_returns409() throws Exception {
        Long id = seed("APPROVED");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rejectAlreadyRejected_returns409() throws Exception {
        Long id = seed("REJECTED");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void approveAlreadyRejected_returns409() throws Exception {
        Long id = seed("REJECTED");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_invalidValue_returns400() throws Exception {
        Long id = seed("PENDING");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatus_toPending_returns400() throws Exception {
        Long id = seed("PENDING");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_missingField_returns400() throws Exception {
        Long id = seed("PENDING");

        mockMvc.perform(patch("/api/v1/registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("status")));
    }

    @Test
    void updateStatus_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/registrations/{id}/status", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }
}
