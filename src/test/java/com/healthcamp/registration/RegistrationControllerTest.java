package com.healthcamp.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the registration endpoint using the full Spring context
 * with an in-memory H2 database (test profile). Each test runs in a transaction
 * that rolls back, keeping the database clean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationRepository repository;

    @Test
    void registerValid_returnsCreatedAndPersists() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Jane Doe",
                "email", "jane@example.com",
                "phone", "+919876543210",
                "preferredCampDate", LocalDate.now().plusDays(7).toString()
        );

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/registrations/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.phone").value("+919876543210"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists());

        // Persistence is configured (JPA + H2); confirm the row was actually saved.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void registerMissingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*].field",
                        hasItems("fullName", "email", "phone", "preferredCampDate")));
    }

    @Test
    void registerInvalidInput_returns400WithFieldErrors() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Jane Doe",
                "email", "not-an-email",
                "phone", "123",
                "preferredCampDate", LocalDate.now().minusDays(1).toString()
        );

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*].field",
                        hasItems("email", "phone", "preferredCampDate")));
    }
}
