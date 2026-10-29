package com.healthcamp.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Task 9: Selenium WebDriver Automated Test Suite.
 *
 * <p>Executes 5 critical user journeys against the Spring Boot Health Camp Registration
 * Platform using headless Chrome / Edge browser and JUnit 5.
 *
 * <p>Features:
 * <ul>
 *   <li>Runs locally via {@code mvn clean test}.</li>
 *   <li>Uses in-memory H2 database under {@code test} profile (no external MySQL required).</li>
 *   <li>Uses {@link SeleniumScreenshotExtension} to capture screenshots into {@code target/selenium-failures/} on any test failure.</li>
 *   <li>Follows Page Object Model (POM) via {@link RegistrationPage}.</li>
 *   <li>Uses explicit waits to avoid race conditions.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SeleniumScreenshotExtension.class)
public class RegistrationSeleniumTest implements WebDriverProvider {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private RegistrationPage registrationPage;
    private String baseUrl;

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    @BeforeEach
    void setUp() {
        driver = WebDriverFactory.createDriver();
        registrationPage = new RegistrationPage(driver);
        baseUrl = "http://localhost:" + port + "/index.html";
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("Journey 1: Registration page loads and required fields are visible")
    void journey1_registrationPageLoads_requiredFieldsVisible() {
        registrationPage.open(baseUrl);

        assertAll("Verify page load and form visibility",
                () -> assertThat(registrationPage.isPageLoaded())
                        .as("Page title should be loaded")
                        .isTrue(),
                () -> assertThat(registrationPage.getPageTitleText())
                        .as("Page title text should match")
                        .contains("Health Camp Participant Registration"),
                () -> assertThat(registrationPage.areRequiredFieldsVisible())
                        .as("All 4 input fields and buttons must be visible")
                        .isTrue(),
                () -> assertThat(registrationPage.isSuccessBannerVisible())
                        .as("Success banner should be initially hidden")
                        .isFalse(),
                () -> assertThat(registrationPage.isErrorBannerVisible())
                        .as("Error banner should be initially hidden")
                        .isFalse()
        );
    }

    @Test
    @DisplayName("Journey 2: Valid registration is submitted and expected confirmation appears")
    void journey2_validRegistration_submitted_confirmationAppears() {
        registrationPage.open(baseUrl);

        String uniqueEmail = "participant_" + UUID.randomUUID().toString().substring(0, 8) + "@example.org";
        String fullName = "Aarav Sharma";
        String phone = "+919876543210";
        String futureDate = LocalDate.now().plusDays(14).toString();

        registrationPage.fillForm(fullName, uniqueEmail, phone, futureDate);
        registrationPage.clickSubmit();

        registrationPage.waitForSuccessBanner();

        assertAll("Verify successful registration confirmation",
                () -> assertThat(registrationPage.isSuccessBannerVisible())
                        .as("Success banner must be visible")
                        .isTrue(),
                () -> assertThat(registrationPage.getSuccessHeadingText())
                        .as("Success heading text")
                        .contains("Registration Submitted Successfully!"),
                () -> assertThat(registrationPage.getConfirmedIdText())
                        .as("Confirmed registration ID should have prefix '#'")
                        .startsWith("#"),
                () -> assertThat(registrationPage.getConfirmedStatusText())
                        .as("Status must be PENDING upon initial registration")
                        .isEqualTo("PENDING"),
                () -> assertThat(registrationPage.getConfirmedNameText())
                        .as("Confirmed full name")
                        .isEqualTo(fullName),
                () -> assertThat(registrationPage.getConfirmedEmailText())
                        .as("Confirmed email")
                        .isEqualTo(uniqueEmail),
                () -> assertThat(registrationPage.getConfirmedPhoneText())
                        .as("Confirmed phone")
                        .isEqualTo(phone),
                () -> assertThat(registrationPage.getConfirmedDateText())
                        .as("Confirmed preferred camp date")
                        .isEqualTo(futureDate)
        );
    }

    @Test
    @DisplayName("Journey 3: Invalid email and phone format are rejected with validation errors")
    void journey3_invalidEmailAndPhone_rejectedWithValidationErrors() {
        registrationPage.open(baseUrl);

        String invalidEmail = "not-a-valid-email";
        String invalidPhone = "123"; // Too short (min 7 digits per API regex)
        String futureDate = LocalDate.now().plusDays(7).toString();

        registrationPage.fillForm("Vikram Malhotra", invalidEmail, invalidPhone, futureDate);
        registrationPage.clickSubmit();

        registrationPage.waitForErrorBanner();

        assertAll("Verify validation error messages for invalid format",
                () -> assertThat(registrationPage.isErrorBannerVisible())
                        .as("Error banner should be visible")
                .isTrue(),
                () -> assertThat(registrationPage.getErrorMessageText())
                        .as("Error banner message")
                        .contains("Validation failed"),
                () -> assertThat(registrationPage.isEmailErrorVisible())
                        .as("Email field error should be visible")
                        .isTrue(),
                () -> assertThat(registrationPage.getEmailErrorText())
                        .as("Email field error message")
                        .contains("Email must be a valid email address"),
                () -> assertThat(registrationPage.isPhoneErrorVisible())
                        .as("Phone field error should be visible")
                        .isTrue(),
                () -> assertThat(registrationPage.getPhoneErrorText())
                        .as("Phone field error message")
                        .contains("Phone must be 7-20 digits")
        );
    }

    @Test
    @DisplayName("Journey 4: Missing required fields are rejected with validation errors")
    void journey4_missingRequiredFields_rejectedWithValidationErrors() {
        registrationPage.open(baseUrl);

        // Submit form without filling any input fields
        registrationPage.clickSubmit();

        registrationPage.waitForErrorBanner();

        List<String> errors = registrationPage.getErrorListTexts();

        assertAll("Verify all required fields produce validation errors",
                () -> assertThat(registrationPage.isErrorBannerVisible())
                        .as("Error banner must be visible")
                        .isTrue(),
                () -> assertThat(registrationPage.getErrorMessageText())
                        .as("Main error banner text")
                        .contains("Validation failed"),
                () -> assertThat(errors)
                        .as("Error list should contain validation errors for all 4 required fields")
                        .anyMatch(err -> err.contains("Full name is required"))
                        .anyMatch(err -> err.contains("Email is required"))
                        .anyMatch(err -> err.contains("Phone is required"))
                        .anyMatch(err -> err.contains("Preferred camp date is required")),
                () -> assertThat(registrationPage.isFullNameErrorVisible()).isTrue(),
                () -> assertThat(registrationPage.isEmailErrorVisible()).isTrue(),
                () -> assertThat(registrationPage.isPhoneErrorVisible()).isTrue(),
                () -> assertThat(registrationPage.isPreferredCampDateErrorVisible()).isTrue()
        );
    }

    @Test
    @DisplayName("Journey 5: Registration status tracking and past camp date rejection")
    void journey5_registrationStatusTracking_andPastDateRejection() {
        registrationPage.open(baseUrl);

        // Step 1: Submit a registration with a past date to verify business date validation
        String pastDate = LocalDate.now().minusDays(2).toString();
        registrationPage.fillForm("Sunita Rao", "sunita.rao@example.org", "+919123456780", pastDate);
        registrationPage.clickSubmit();

        registrationPage.waitForErrorBanner();

        assertThat(registrationPage.isPreferredCampDateErrorVisible())
                .as("Past camp date must be rejected")
                .isTrue();
        assertThat(registrationPage.getPreferredCampDateErrorText())
                .contains("Preferred camp date must be a future date");

        // Step 2: Correct to a valid future date and submit
        String futureDate = LocalDate.now().plusMonths(1).toString();
        registrationPage.enterPreferredCampDate(futureDate);
        registrationPage.clickSubmit();

        registrationPage.waitForSuccessBanner();

        // Step 3: Verify the initial registration status is assigned and tracked as PENDING
        assertAll("Verify initial status tracking",
                () -> assertThat(registrationPage.getConfirmedStatusText())
                        .as("Initial registration status is tracked as PENDING")
                        .isEqualTo("PENDING"),
                () -> assertThat(registrationPage.getConfirmedIdText())
                        .as("Assigned registration ID should be positive integer")
                        .matches("#\\d+"),
                () -> assertThat(registrationPage.getConfirmedCreatedAtText())
                        .as("Creation timestamp must be populated")
                        .isNotBlank()
        );
    }
}

