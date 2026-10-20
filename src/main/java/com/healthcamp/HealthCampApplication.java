package com.healthcamp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Health Camp Registration and Deployment Platform.
 *
 * <p>This is a minimal application skeleton. The registration workflow, REST endpoints,
 * persistence entities/repositories, and the approval/confirmation flow are <b>planned</b>
 * (see the {@code docs/} folder) and are <b>not yet implemented</b>.
 */
@SpringBootApplication
public class HealthCampApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthCampApplication.class, args);
    }
}
