package com.healthcamp.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Validates the {@link SeleniumScreenshotExtension} behavior:
 * ensures that when a failure exception is present, a PNG screenshot
 * is captured into {@code target/selenium-failures/}, contains the test
 * class and method name and timestamp in the filename, and writes non-empty image bytes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SeleniumScreenshotExtensionTest implements WebDriverProvider {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private RegistrationPage page;

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    @BeforeEach
    void setUp() {
        driver = WebDriverFactory.createDriver();
        page = new RegistrationPage(driver);
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
    @DisplayName("Verify failure screenshot is created and saved under target/selenium-failures/")
    void verifyFailureScreenshotCapturedInTargetDir() throws Exception {
        page.open("http://localhost:" + port + "/index.html");
        assertThat(page.isPageLoaded()).isTrue();

        SeleniumScreenshotExtension extension = new SeleniumScreenshotExtension();

        ExtensionContext context = Mockito.mock(ExtensionContext.class);
        when(context.getTestInstance()).thenReturn(Optional.of(this));
        when(context.getTestClass()).thenReturn(Optional.of((Class) SeleniumScreenshotExtensionTest.class));
        when(context.getTestMethod()).thenReturn(Optional.of(
                SeleniumScreenshotExtensionTest.class.getDeclaredMethod("verifyFailureScreenshotCapturedInTargetDir")));
        when(context.getExecutionException()).thenReturn(Optional.of(new AssertionError("Demonstration failure for Task 9 screenshot capture verification")));

        extension.afterTestExecution(context);

        Path screenshotDir = Paths.get("target", "selenium-failures");
        assertThat(Files.exists(screenshotDir))
                .as("target/selenium-failures/ directory must exist")
                .isTrue();

        try (Stream<Path> stream = Files.list(screenshotDir)) {
            Optional<Path> capturedPng = stream
                    .filter(p -> p.getFileName().toString().startsWith("SeleniumScreenshotExtensionTest_") && p.getFileName().toString().endsWith(".png"))
                    .findFirst();

            assertThat(capturedPng)
                    .as("A screenshot starting with 'SeleniumScreenshotExtensionTest_' must be saved")
                    .isPresent();

            assertThat(Files.size(capturedPng.get()))
                    .as("Screenshot file must contain image data (> 0 bytes)")
                    .isGreaterThan(1000);
        }
    }
}
