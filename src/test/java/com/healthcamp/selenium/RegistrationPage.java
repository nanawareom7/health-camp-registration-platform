package com.healthcamp.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object Model (POM) representing the Health Camp Registration web page.
 *
 * <p>Encapsulates DOM locators, interactions, and explicit waits for robust,
 * maintainable Selenium WebDriver tests.
 */
public class RegistrationPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Locators
    private final By pageTitle = By.id("pageTitle");
    private final By fullNameInput = By.id("fullName");
    private final By emailInput = By.id("email");
    private final By phoneInput = By.id("phone");
    private final By preferredCampDateInput = By.id("preferredCampDate");
    private final By submitButton = By.id("submitBtn");
    private final By resetButton = By.id("resetBtn");

    // Success banner & confirmation elements
    private final By successBanner = By.id("successBanner");
    private final By successHeading = By.id("successHeading");
    private final By confirmedId = By.id("confirmedId");
    private final By confirmedStatus = By.id("confirmedStatus");
    private final By confirmedName = By.id("confirmedName");
    private final By confirmedEmail = By.id("confirmedEmail");
    private final By confirmedPhone = By.id("confirmedPhone");
    private final By confirmedDate = By.id("confirmedDate");
    private final By confirmedCreatedAt = By.id("confirmedCreatedAt");

    // Error banner & validation elements
    private final By errorBanner = By.id("errorBanner");
    private final By errorMessage = By.id("errorMessage");
    private final By errorListItems = By.cssSelector("#errorList li");
    private final By fullNameError = By.id("fullNameError");
    private final By emailError = By.id("emailError");
    private final By phoneError = By.id("phoneError");
    private final By preferredCampDateError = By.id("preferredCampDateError");

    public RegistrationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void open(String url) {
        driver.get(url);
        wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle));
    }

    public boolean isPageLoaded() {
        try {
            return driver.findElement(pageTitle).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPageTitleText() {
        return driver.findElement(pageTitle).getText();
    }

    public boolean areRequiredFieldsVisible() {
        return driver.findElement(fullNameInput).isDisplayed()
                && driver.findElement(emailInput).isDisplayed()
                && driver.findElement(phoneInput).isDisplayed()
                && driver.findElement(preferredCampDateInput).isDisplayed()
                && driver.findElement(submitButton).isDisplayed()
                && driver.findElement(resetButton).isDisplayed();
    }

    public void enterFullName(String name) {
        WebElement el = driver.findElement(fullNameInput);
        el.clear();
        if (name != null) {
            el.sendKeys(name);
        }
    }

    public void enterEmail(String email) {
        WebElement el = driver.findElement(emailInput);
        el.clear();
        if (email != null) {
            el.sendKeys(email);
        }
    }

    public void enterPhone(String phone) {
        WebElement el = driver.findElement(phoneInput);
        el.clear();
        if (phone != null) {
            el.sendKeys(phone);
        }
    }

    public void enterPreferredCampDate(String date) {
        WebElement el = driver.findElement(preferredCampDateInput);
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "arguments[0].value = arguments[1]; " +
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true })); " +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    el, date != null ? date : "");
        } else {
            el.clear();
            if (date != null) {
                el.sendKeys(date);
            }
        }
    }

    public void fillForm(String fullName, String email, String phone, String campDate) {
        enterFullName(fullName);
        enterEmail(email);
        enterPhone(phone);
        enterPreferredCampDate(campDate);
    }

    public void clickSubmit() {
        driver.findElement(submitButton).click();
    }

    public void clickReset() {
        driver.findElement(resetButton).click();
    }

    public void waitForSuccessBanner() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(successBanner));
    }

    public void waitForErrorBanner() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(errorBanner));
    }

    public boolean isSuccessBannerVisible() {
        try {
            return driver.findElement(successBanner).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isErrorBannerVisible() {
        try {
            return driver.findElement(errorBanner).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getSuccessHeadingText() {
        return driver.findElement(successHeading).getText();
    }

    public String getConfirmedIdText() {
        return driver.findElement(confirmedId).getText();
    }

    public String getConfirmedStatusText() {
        return driver.findElement(confirmedStatus).getText();
    }

    public String getConfirmedNameText() {
        return driver.findElement(confirmedName).getText();
    }

    public String getConfirmedEmailText() {
        return driver.findElement(confirmedEmail).getText();
    }

    public String getConfirmedPhoneText() {
        return driver.findElement(confirmedPhone).getText();
    }

    public String getConfirmedDateText() {
        return driver.findElement(confirmedDate).getText();
    }

    public String getConfirmedCreatedAtText() {
        return driver.findElement(confirmedCreatedAt).getText();
    }

    public String getErrorMessageText() {
        return driver.findElement(errorMessage).getText();
    }

    public List<String> getErrorListTexts() {
        return driver.findElements(errorListItems)
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public boolean isEmailErrorVisible() {
        try {
            return driver.findElement(emailError).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailErrorText() {
        return driver.findElement(emailError).getText();
    }

    public boolean isPhoneErrorVisible() {
        try {
            return driver.findElement(phoneError).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPhoneErrorText() {
        return driver.findElement(phoneError).getText();
    }

    public boolean isFullNameErrorVisible() {
        try {
            return driver.findElement(fullNameError).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getFullNameErrorText() {
        return driver.findElement(fullNameError).getText();
    }

    public boolean isPreferredCampDateErrorVisible() {
        try {
            return driver.findElement(preferredCampDateError).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPreferredCampDateErrorText() {
        return driver.findElement(preferredCampDateError).getText();
    }
}
