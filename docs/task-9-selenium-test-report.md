# Task 8 and Task 9: Debug, Fix, and Execution Report

**Project:** Health Camp Registration and Deployment Platform  
**Execution Date:** 2026-10-29  
**Branch:** `development`  
**Environment:** Windows 11, JDK 27 (compiling to Java 17 bytecode target), Maven 3.10.0, Google Chrome 156.0.8078.49 (Headless), H2 In-Memory Database (MySQL compatibility mode for automated tests), Jenkins 2.x on Windows Service.

---

## 1. Executive Summary

This report documents the forensic investigation, root cause diagnosis, and validated fixes for:
1. **Task 9 (Whitelabel 404 & Screenshot Confusion):** Diagnosis of why port `8081` previously returned HTTP 404, proof of static UI packaging in the Spring Boot JAR, resolution of reflection-based screenshot naming in `SeleniumScreenshotExtensionTest`, and live verification that `http://localhost:8081/` actively serves the registration UI with HTTP 200.
2. **Task 8 (Jenkins Pipeline Deployment Hang):** Diagnosis of why Jenkins Build 10 was stuck for 20 minutes on the Deploy stage, identification of PowerShell's internal stream-reader deadlock on `Start-Process -RedirectStandardOutput`, implementation of detached execution via `launch.bat`, and addition of a bounded, deterministic readiness check verifying HTTP 200 on `http://localhost:<port>/`.

---

## 2. Root Cause Analysis

### Issue A: Task 9 Whitelabel 404 on Port 8081
- **Root Cause:** The process running on port 8081 was executing `C:\Jenkins\deployments\health-camp\health-camp-registration-platform-0.0.1-SNAPSHOT.jar`, which was built by Jenkins Build 10 at 21:13:56 from remote GitHub branch `development` (commit `f913146`). Because the newly created `src/main/resources/static/index.html` was an untracked local file not yet committed/pushed to GitHub, the deployed JAR in `C:\Jenkins\deployments\health-camp` contained *no* static resources (`BOOT-INF/classes/static/index.html` was absent). Consequently, Spring Boot's embedded Tomcat had no welcome page mapping and returned the Whitelabel HTTP 404 page.
- **Verification & Fix:** When packaged locally with `mvn package`, `src/main/resources/static/index.html` is placed into `BOOT-INF/classes/static/index.html`. Deploying this updated JAR to `C:\Jenkins\deployments\health-camp` and querying `http://localhost:8081/` immediately returns `HTTP 200 OK` with `Content-Type: text/html;charset=UTF-8` and the full registration page.

### Issue B: Misleading Screenshot `journey3_invalidEmailAndPhone_rejectedWithValidationErrors`
- **Root Cause:** In `SeleniumScreenshotExtensionTest.java`, reflection was used to obtain a method for the mock `ExtensionContext`:
  `RegistrationSeleniumTest.class.getDeclaredMethods()[0]`
  On JVM 27, reflection method ordering placed `journey3_invalidEmailAndPhone_rejectedWithValidationErrors` at index 0. When `SeleniumScreenshotExtensionTest` verified that screenshot capture worked, it passed that method name to the extension, generating a PNG named `RegistrationSeleniumTest_journey3_...png`. Journey 3 had actually passed in the test suite, but the filename created the illusion that Journey 3 failed.
- **Fix:** `SeleniumScreenshotExtensionTest.java` was updated to explicitly reference its own method `SeleniumScreenshotExtensionTest.class.getDeclaredMethod("verifyFailureScreenshotCapturedInTargetDir")`. The generated demonstration screenshot is now named `SeleniumScreenshotExtensionTest_verifyFailureScreenshotCapturedInTargetDir_<timestamp>.png`.

### Issue C: Task 8 Jenkins Deploy Stage Hanging for 20 Minutes
- **Root Cause:** In `Jenkinsfile` commit `f913146`, the launch step was rewritten as:
  `$proc = Start-Process -FilePath "java" -ArgumentList $javaArgs -WorkingDirectory $deployDir -RedirectStandardOutput $logFile -RedirectStandardError $logFileErr -PassThru`
  Inside Jenkins' `durable-task` plugin on Windows, scripts are wrapped in `& { & $MainScript } *>&1 | Out-FileNoBom`. In Windows PowerShell, passing `-RedirectStandardOutput` and `-RedirectStandardError` to `Start-Process` attaches internal .NET stream readers that remain active as long as the spawned child process is alive. Because `java.exe` is a standing background service, PowerShell waited indefinitely for EOF on those streams. The PowerShell step never returned to Jenkins, causing the job to hang until Jenkins' 20-minute pipeline timeout aborted the build (`Pipeline result: ABORTED`).
- **Fix:** Modified `Jenkinsfile` to write a dedicated `launch.bat` script that handles file redirection (`java ... > app.log 2> app-err.log`) at the OS shell level. PowerShell launches `cmd.exe /c launch.bat` detached without stream redirection parameters, returning in under 200 ms. The subsequent health-check step polls `http://localhost:<port>/` in a bounded loop (max 60 seconds), verifying `HTTP 200 OK` and extracting the real listening PID into `app.pid`.

---

## 3. Test Execution Results (Local Suite)

* **Command:** `mvn clean test`
* **Build Status:** `BUILD SUCCESS` (Execution time: 23.2 s)
* **Total Tests Run:** 10
* **Failures:** 0
* **Errors:** 0
* **Skipped:** 0

### Test Breakdown

| Test Class | Category | Tests Run | Passed | Failed |
| --- | --- | --- | --- | --- |
| `HealthCampApplicationTests` | Context Smoke Test | 1 | 1 | 0 |
| `RegistrationControllerTest` | MockMvc Controller Test | 3 | 3 | 0 |
| `RegistrationSeleniumTest` | Selenium WebDriver UI E2E (5 Journeys) | 5 | 5 | 0 |
| `SeleniumScreenshotExtensionTest` | Screenshot Extension Verification | 1 | 1 | 0 |
| **Total** | | **10** | **10** | **0** |

### Critical User Journeys (Selenium)

1. **Journey 1 (`journey1_registrationPageLoads_requiredFieldsVisible`):** Verified page title, input fields (`fullName`, `email`, `phone`, `preferredCampDate`), submit/reset buttons, and initial hidden status of alert banners.
2. **Journey 2 (`journey2_validRegistration_submitted_confirmationAppears`):** Verified valid form submission with unique email, explicit wait for `#successBanner`, confirmation ID prefixed with `#`, status `PENDING`, and Hibernate insertion into in-memory H2 database.
3. **Journey 3 (`journey3_invalidEmailAndPhone_rejectedWithValidationErrors`):** Verified rejection of invalid email and short phone with HTTP 400 Bean Validation messages (`Email must be a valid email address`, `Phone must be 7-20 digits`).
4. **Journey 4 (`journey4_missingRequiredFields_rejectedWithValidationErrors`):** Verified rejection of blank submission with field-level errors for all 4 required fields.
5. **Journey 5 (`journey5_registrationStatusTracking_andPastDateRejection`):** Verified business rule rejection for past camp date, subsequent valid submission, and status tracking as `PENDING`.

---

## 4. Packaging and Live Deployment Verification

1. **JAR Content Verification:**
   ```powershell
   jar tf "target\health-camp-registration-platform-0.0.1-SNAPSHOT.jar" | Select-String "index.html"
   # Output: BOOT-INF/classes/static/index.html
   ```
2. **Port 8081 Live Verification:**
   ```powershell
   curl.exe -i http://localhost:8081/
   ```
   **Response:**
   ```http
   HTTP/1.1 200 
   Content-Type: text/html;charset=UTF-8
   Content-Length: 11863
   <!DOCTYPE html>
   <html lang="en">
   ...
   ```
3. **Failure Screenshot Generation:**
   Captured screenshot exists at `target/selenium-failures/SeleniumScreenshotExtensionTest_verifyFailureScreenshotCapturedInTargetDir_20261029_214213_721.png` (29,443 bytes).

---

## 5. Artifact and Configuration Summary

- Web UI: [`src/main/resources/static/index.html`](file:///C:/Users/Asus/Documents/DEVOPS-project/health-camp-registration-platform/src/main/resources/static/index.html)
- Selenium Suite: [`src/test/java/com/healthcamp/selenium/RegistrationSeleniumTest.java`](file:///C:/Users/Asus/Documents/DEVOPS-project/health-camp-registration-platform/src/test/java/com/healthcamp/selenium/RegistrationSeleniumTest.java)
- Screenshot Extension: [`src/test/java/com/healthcamp/selenium/SeleniumScreenshotExtension.java`](file:///C:/Users/Asus/Documents/DEVOPS-project/health-camp-registration-platform/src/test/java/com/healthcamp/selenium/SeleniumScreenshotExtension.java)
- Jenkins Pipeline: [`Jenkinsfile`](file:///C:/Users/Asus/Documents/DEVOPS-project/health-camp-registration-platform/Jenkinsfile)
- Surefire Reports: `target/surefire-reports/`
- Failure Screenshots: `target/selenium-failures/`
