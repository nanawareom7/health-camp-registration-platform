package com.healthcamp.registration;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link Registration}. Uses the existing Spring Data JPA
 * persistence configured in {@code pom.xml}; no new persistence dependencies added.
 */
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
}
