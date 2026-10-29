package com.healthcamp.selenium;

import org.openqa.selenium.WebDriver;

/**
 * Interface implemented by test classes that manage a Selenium {@link WebDriver},
 * allowing JUnit 5 extensions (like {@link SeleniumScreenshotExtension}) to access
 * the active browser session.
 */
public interface WebDriverProvider {
    WebDriver getDriver();
}

