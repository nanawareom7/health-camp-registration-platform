package com.healthcamp.selenium;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * JUnit 5 Extension that automatically captures a browser screenshot upon test failure
 * using Selenium's {@link TakesScreenshot}.
 *
 * <p>Implements {@link AfterTestExecutionCallback} to capture the browser state
 * immediately upon failure, prior to test teardown (@AfterEach).
 *
 * <p>Requirements fulfilled:
 * <ul>
 *   <li>Saves screenshots in {@code target/selenium-failures/}.</li>
 *   <li>Includes test class name, test method name, and timestamp in filename.</li>
 *   <li>Captures screenshots whenever a test fails.</li>
 *   <li>Prints the absolute screenshot path to test stdout.</li>
 *   <li>Ensures any screenshot failure does not suppress or mask the original test exception.</li>
 * </ul>
 */
public class SeleniumScreenshotExtension implements AfterTestExecutionCallback {

    private static final Path SCREENSHOT_DIR = Paths.get("target", "selenium-failures");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Optional<Throwable> exception = context.getExecutionException();
        if (exception.isPresent()) {
            captureScreenshot(context, exception.get());
        }
    }

    private void captureScreenshot(ExtensionContext context, Throwable cause) {
        try {
            Optional<Object> testInstanceOpt = context.getTestInstance();
            if (testInstanceOpt.isEmpty()) {
                System.err.println("[SELENIUM-SCREENSHOT] Test instance unavailable. Cannot capture screenshot.");
                return;
            }

            Object testInstance = testInstanceOpt.get();
            if (!(testInstance instanceof WebDriverProvider provider)) {
                System.err.println("[SELENIUM-SCREENSHOT] Test class does not implement WebDriverProvider. Skipping screenshot.");
                return;
            }

            WebDriver driver = provider.getDriver();
            if (driver == null) {
                System.err.println("[SELENIUM-SCREENSHOT] WebDriver is null. Cannot capture screenshot.");
                return;
            }

            if (driver instanceof TakesScreenshot takesScreenshot) {
                Files.createDirectories(SCREENSHOT_DIR);

                String className = context.getTestClass().map(Class::getSimpleName).orElse("UnknownClass");
                String methodName = context.getTestMethod().map(m -> m.getName()).orElse("unknownMethod");
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                String filename = String.format("%s_%s_%s.png", className, methodName, timestamp);
                Path targetPath = SCREENSHOT_DIR.resolve(filename);

                byte[] screenshotBytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
                Files.write(targetPath, screenshotBytes);

                System.out.println("================================================================================");
                System.out.println("[SELENIUM-SCREENSHOT] TEST FAILED: " + className + "." + methodName);
                System.out.println("[SELENIUM-SCREENSHOT] Failure cause: " + (cause != null ? cause.getMessage() : "Unknown"));
                System.out.println("[SELENIUM-SCREENSHOT] Screenshot captured at: " + targetPath.toAbsolutePath());
                System.out.println("================================================================================");
            } else {
                System.err.println("[SELENIUM-SCREENSHOT] Driver does not implement TakesScreenshot: " + driver.getClass().getName());
            }
        } catch (Exception ex) {
            // Must not suppress or hide the original test failure
            System.err.println("[SELENIUM-SCREENSHOT] Warning: Failed to capture failure screenshot: " + ex.getMessage());
        }
    }
}
