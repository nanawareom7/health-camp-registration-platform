package com.healthcamp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic smoke test that verifies the Spring application context loads successfully.
 *
 * <p>Runs against an in-memory H2 database (see
 * {@code src/test/resources/application-test.yml}) so it does not require a running
 * MySQL instance. Uses JUnit 5 ({@code org.junit.jupiter}).
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthCampApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the application context starts without errors.
    }
}
