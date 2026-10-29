package com.healthcamp.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.io.File;
import java.time.Duration;

/**
 * Factory for creating configured Selenium WebDriver instances.
 *
 * <p>Features:
 * <ul>
 *   <li>Runs in headless mode ({@code --headless=new}) for seamless CI/CD and local Maven execution.</li>
 *   <li>Uses Selenium Manager (bundled in Selenium 4.31+) to resolve driver binaries automatically.</li>
 *   <li>Checks standard Windows installation paths for Chrome and Edge binaries.</li>
 *   <li>Falls back gracefully from Chrome to Edge if Chrome is unavailable.</li>
 *   <li>Configures timeouts and common options to avoid resource issues.</li>
 * </ul>
 */
public class WebDriverFactory {

    public static WebDriver createDriver() {
        String browser = System.getProperty("selenium.browser", "chrome").toLowerCase();

        WebDriver driver;
        if ("edge".equals(browser)) {
            driver = createEdgeDriver();
        } else {
            try {
                driver = createChromeDriver();
            } catch (Exception ex) {
                System.out.println("[SELENIUM] Failed to initialize ChromeDriver (" + ex.getMessage() + "). Falling back to EdgeDriver...");
                driver = createEdgeDriver();
            }
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        return driver;
    }

    public static ChromeDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");

        File chrome64 = new File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        File chrome32 = new File("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
        if (chrome64.exists()) {
            options.setBinary(chrome64);
        } else if (chrome32.exists()) {
            options.setBinary(chrome32);
        }

        return new ChromeDriver(options);
    }

    public static EdgeDriver createEdgeDriver() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");

        File edge32 = new File("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        File edge64 = new File("C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe");
        if (edge32.exists()) {
            options.setBinary(edge32);
        } else if (edge64.exists()) {
            options.setBinary(edge64);
        }

        return new EdgeDriver(options);
    }
}

