# Task 12 — Jenkins–Docker Continuous Deployment Evidence Report

**Project:** Health Camp Registration and Deployment Platform  
**Target Repository Path:** `C:\Users\Asus\Documents\DEVOPS-project\health-camp-registration-platform`  
**Date of Report:** 2026-10-30  
**Operating System:** Windows 11  
**Jenkins Controller:** `http://localhost:8080` (Windows Service, `LocalSystem` account)  
**Jenkins Pipeline Job:** `Health-Camp-Pipeline`  
**Configured SCM Branch:** `*/development`  
**Docker Hub Target:** `nanawareom7/health-camp-registration`  
**Standalone Deployment Target:** `http://localhost:8081/` (Task 8 JAR deployment preserved)  
**Docker Deployment Target:** `http://localhost:8082/` (Task 12 container deployment: `health-camp-app-cd`)  
**MySQL Database Target:** `host.docker.internal:3306/health_camp_db`

---

## 1. Evidence Tier Classification

To maintain absolute academic and engineering integrity, all evidence is strictly divided into three tiers:
- **Tier 1 — Planned Architecture & Pipeline Design:** The declarative structure, parameters, stage definitions, and credential bindings.
- **Tier 2 — Locally Verified Live Diagnostic & Persistence Evidence:** Executed directly on the Windows 11 host and Docker engine to validate test suites, database schema, container JDBC connectivity, and end-to-end database persistence.
- **Tier 3 — Actual Jenkins Pipeline Execution Evidence:** Live console logs, build number, and test trend graphs captured during/after a Jenkins build run.

---

## 2. Tier 2: Locally Verified Evidence & Test Execution

### 2.1 Automated Test Suite Verification (Maven Clean Verify)
Executed `mvn clean verify --batch-mode --no-transfer-progress` on the project:
- **Total Tests Run:** 10
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Status:** `BUILD SUCCESS` (Execution time: 34.38 s)
- **Executable JAR Repackaged:** `target/health-camp-registration-platform-0.0.1-SNAPSHOT.jar` (55.5 MB)

**Test Classes Breakdown:**
1. `HealthCampApplicationTests`: 1 smoke test (`contextLoads()`) — PASSED
2. `RegistrationControllerTest`: 3 Web API tests (`registerValid`, `registerMissingFields`, `registerInvalidInput`) — PASSED
3. `SeleniumScreenshotExtensionTest`: 1 extension verification test (`verifyFailureScreenshotCapturedInTargetDir`) — PASSED
4. `RegistrationSeleniumTest`: 5 browser UI automation journeys in headless Chrome — PASSED:
   - `journey1_registrationPageLoads_requiredFieldsVisible`
   - `journey2_validRegistration_submitted_confirmationAppears`
   - `journey3_invalidEmailAndPhone_rejectedWithValidationErrors`
   - `journey4_missingRequiredFields_rejectedWithValidationErrors`
   - `journey5_registrationStatusTracking_andPastDateRejection`

**Surefire Reports Generated:**
- `TEST-com.healthcamp.HealthCampApplicationTests.xml` (39.1 KB)
- `TEST-com.healthcamp.registration.RegistrationControllerTest.xml` (39.4 KB)
- `TEST-com.healthcamp.selenium.RegistrationSeleniumTest.xml` (45.0 KB)
- `TEST-com.healthcamp.selenium.SeleniumScreenshotExtensionTest.xml` (35.1 KB)

---

### 2.2 Live Database Persistence Verification

1. **Schema Initialization:**  
   The `registrations` table was created in `health_camp_db` using the exact approved DDL:
   ```sql
   CREATE TABLE `registrations` (
     `id` bigint NOT NULL AUTO_INCREMENT,
     `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
     `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
     `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
     `preferred_camp_date` date NOT NULL,
     `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
     `created_at` datetime(6) NOT NULL,
     PRIMARY KEY (`id`)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
   ```

2. **Container-to-Host Persistence Test:**  
   A test container was launched with runtime credentials connecting to `jdbc:mysql://host.docker.internal:3306/health_camp_db`.

3. **Inbound HTTP Request:**
   ```json
   POST /api/v1/registrations
   {
       "fullName": "Task 12 Verification Participant",
       "email": "task12.verified@example.com",
       "phone": "+919876543210",
       "preferredCampDate": "2026-11-25"
   }
   ```

4. **API Response (HTTP 201 Created):**
   ```json
   {
       "id": 1,
       "fullName": "Task 12 Verification Participant",
       "email": "task12.verified@example.com",
       "phone": "+919876543210",
       "preferredCampDate": "2026-11-25",
       "status": "PENDING",
       "createdAt": "2026-10-29T20:31:50.565419921Z"
   }
   ```

5. **Direct MySQL Database Query Result:**
   ```sql
   SELECT id, full_name, email, phone, preferred_camp_date, status, created_at FROM health_camp_db.registrations;
   ```
   ```
   +----+-----------------------------------+-----------------------------+---------------+---------------------+---------+----------------------------+
   | id | full_name                         | email                       | phone         | preferred_camp_date | status  | created_at                 |
   +----+-----------------------------------+-----------------------------+---------------+---------------------+---------+----------------------------+
   |  1 | Task 12 Verification Participant  | task12.verified@example.com | +919876543210 | 2026-11-25          | PENDING | 2026-10-29 20:31:50.565420 |
   +----+-----------------------------------+-----------------------------+---------------+---------------------+---------+----------------------------+
   ```
   *Result:* Proves conclusively that registrations are persisted into the physical MySQL database with full microsecond precision (`DATETIME(6)`).

---

### 2.3 Jenkins Credentials Status
- **`dockerhub-creds`:** Verified in `C:\ProgramData\Jenkins\.jenkins\credentials.xml`. (Type: `UsernamePasswordCredentialsImpl`, Username: `nanawareom7`).
- **`mysql-password`:** Verified in `C:\ProgramData\Jenkins\.jenkins\credentials.xml`. (Type: `StringCredentialsImpl` / Secret text).

---

## 3. Tier 1: Continuous Deployment Pipeline Architecture

### 3.1 Stage Execution Order and Deployment Protection
The pipeline enforces strict ordering: **Zero deployments occur until all tests pass, the image is built, and it is published to Docker Hub**:

```
Stage 1: Checkout (*/development)
   │
   ▼
Stage 2: Docker Diagnostic (where docker, docker version, docker info)
   │
   ▼
Stage 3: Test & Build (mvn clean verify + junit allowEmptyResults: false)
   │  [HALT ON TEST FAILURE: No build, no push, no deploy]
   ▼
Stage 4: Package Verification (verify target/*.jar exists)
   │
   ▼
Stage 5: Docker Build (nanawareom7/health-camp-registration:build-${BUILD_NUMBER})
   │
   ▼
Stage 6: Docker Push (docker login via dockerhub-creds -> push build tag + latest)
   │
   ▼
Stage 7: Deploy Standalone (Port 8081 preserved from Task 8; launch.bat + HTTP 200 check)
   │
   ▼
Stage 8: Docker Deploy (Port 8082 with automated rollback)
```

### 3.2 Automated Rollback Logic
In Stage 8:
1. Jenkins inspects whether an active container `health-camp-app-cd` is running and records its exact image tag:
   ```powershell
   $prevImage = docker ps --filter "name=^health-camp-app-cd$" --format "{{.Image}}"
   ```
2. The candidate image (`build-${BUILD_NUMBER}`) is launched on port 8082 with runtime secret injection.
3. A bounded readiness probe checks `http://localhost:8082/` every 3s for up to 60s.
4. **If the candidate fails readiness:**
   - Jenkins prints candidate container logs.
   - Stops and removes the failed candidate container.
   - Restores the previous working container using `$prevImage`.
   - Fails the build.

---

## 4. Tier 3: Actual Jenkins Execution Evidence

*(Pending manual trigger in Jenkins UI by user.)*

- **Job Name:** `Health-Camp-Pipeline`
- **Target Branch:** `*/development`
- **Build Number:** `[Pending trigger]`
- **Docker Hub Published Tags:** `[Pending push]`
- **Active Container Status:** `[Pending pipeline run]`

---

## 5. Submission Evidence Checklist

- [x] Pre-deployment local Maven test verification (10/10 passed).
- [x] Surefire XML test reports generated in `target/surefire-reports/`.
- [x] Database schema initialized and verified in MySQL 8.0.
- [x] Real API registration persisted and confirmed in MySQL table `health_camp_db.registrations`.
- [x] `dockerhub-creds` and `mysql-password` verified in Jenkins.
- [x] Jenkinsfile updated with strict quality gate, dual deployment, and automated rollback.
- [ ] Final Jenkins pipeline execution stage view showing all 8 stages green.
- [ ] Published Docker Hub repository view for `nanawareom7/health-camp-registration`.
