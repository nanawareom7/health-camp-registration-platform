package com.healthcamp.registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for {@link Registration}. Uses the existing Spring Data JPA
 * persistence configured in {@code pom.xml}; no new persistence dependencies added.
 */
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /** All registrations ordered by ascending id (stable listing order). */
    List<Registration> findAllByOrderByIdAsc();

    /** Registrations with a given status, ordered by ascending id. */
    List<Registration> findByStatusOrderByIdAsc(String status);
}
