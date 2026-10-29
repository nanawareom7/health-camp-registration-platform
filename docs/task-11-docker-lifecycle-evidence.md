# Task 11 — Docker Image and Container Lifecycle Evidence Report

**Project:** Health Camp Registration and Deployment Platform  
**Target Repository Path:** `C:\Users\Asus\Documents\DEVOPS-project\health-camp-registration-platform`  
**Date of Execution:** 2026-10-29  
**Platform / OS:** Windows 11 (Docker Desktop 4.97.0 / Docker Engine 29.9.2)  
**Image Tag:** `health-camp-registration:task11`  
**Container Name:** `health-camp-app-task11`  
**Host Port Mapping:** `8082:8080` (Preserves Jenkins CI on `8080` and Jenkins Deployment on `8081`)

---

## 1. Executive Summary

Task 11 requires containerizing the Health Camp Registration Spring Boot application using Docker, demonstrating the complete container lifecycle, verifying container health and endpoints, and distinguishing application-level validations from database connectivity failures without breaking any prior tasks.

All requirements were implemented and verified with genuine execution evidence:
- A multi-stage `Dockerfile` targeting Java 17 was created with non-root security (`appuser`).
- Image built and tagged as `health-camp-registration:task11` (167 MB content size, Alpine base).
- Container run on port `8082`, avoiding conflicts with Jenkins (`8080`) and deployed Spring Boot (`8081`).
- Endpoint verification demonstrated:
  - `GET /` returned HTTP 200 serving the static registration UI.
  - `POST /api/v1/registrations` with invalid data returned HTTP 400 Bad Request with field validation errors (application layer).
  - `POST /api/v1/registrations` with valid data returned HTTP 500 due to MySQL connection refusal on `host.docker.internal:3306` (database layer distinction).
- Full container lifecycle demonstrated: `run` → `inspect` → `logs` → `stop` → `start` → `stop` → `rm`, retaining the image.
- Regression testing verified all 5 Selenium journeys continue to pass cleanly (`BUILD SUCCESS`).

---

## 2. Dockerfile Configuration

**File Location:** `health-camp-registration-platform/Dockerfile`

The Dockerfile implements a production-grade multi-stage build:
- **Stage 1 (Builder):** Uses `maven:3.9-eclipse-temurin-17-alpine` to compile source code and package the Spring Boot JAR with `-DskipTests` (tests are verified in Maven/CI).
- **Stage 2 (Runtime):** Uses lightweight `eclipse-temurin:17-jre-alpine`, creates a dedicated unprivileged user/group (`appuser:appgroup`), configures environment defaults, exposes port 8080, sets a Docker `HEALTHCHECK`, and executes the JAR via `java -jar`.

```dockerfile
# ==============================================================================
# Health Camp Registration and Deployment Platform
# Multi-Stage Dockerfile (Task 11)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application with Maven and JDK 17
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /workspace

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source code
COPY src ./src

# Compile and package Spring Boot executable JAR
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Runtime Environment with Temurin JRE 17
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Security: run application under an unprivileged user instead of root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy packaged JAR from the builder stage
COPY --from=builder /workspace/target/health-camp-registration-platform-*.jar /app/app.jar

# Adjust file ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Default runtime configuration
ENV SERVER_PORT=8080 \
    DB_URL="jdbc:mysql://host.docker.internal:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    DB_USERNAME="root" \
    DB_PASSWORD=""

# Expose internal container port
EXPOSE 8080

# Health check probe against Spring Boot HTTP endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Launch Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
```

---

## 3. Docker Image Build and Inspection

### 3.1 Build Command
```powershell
docker build -t health-camp-registration:task11 .
```

### 3.2 Build Output Highlights
```
#14 [builder 6/6] RUN mvn clean package -DskipTests -B
...
#14 16.83 [INFO] Replacing main artifact /workspace/target/health-camp-registration-platform-0.0.1-SNAPSHOT.jar with repackaged archive
#14 16.83 [INFO] BUILD SUCCESS
#14 16.84 [INFO] Total time: 15.226 s
#15 [stage-1 4/5] COPY --from=builder /workspace/target/health-camp-registration-platform-*.jar /app/app.jar
#16 [stage-1 5/5] RUN chown -R appuser:appgroup /app
#17 naming to docker.io/library/health-camp-registration:task11 done
```

### 3.3 Image Inspection Evidence
```powershell
docker images health-camp-registration:task11
docker inspect health-camp-registration:task11
```

**Inspection Values:**
- **Image ID:** `sha256:4179946221672d41a95b52cc4206cf7d985c5e206c9efe59178cc7dabd0dcbbd`
- **Virtual Size / Content Size:** `167 MB`
- **Architecture / OS:** `linux/amd64`
- **Configured User:** `appuser`
- **Entrypoint:** `["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]`

---

## 4. Host Port Strategy and Isolation

To prevent conflicts with existing platform infrastructure:
- **Host Port 8080:** Jenkins Controller Service (`LISTENING`)
- **Host Port 8081:** Jenkins Deploy Stage target deployment port
- **Host Port 8082:** Dedicated mapping for Docker container (`8082:8080`)

Verification before launch:
```powershell
netstat -ano | Select-String ":8082\s"
# Verified: 0 listeners on port 8082
```

---

## 5. Container Execution & Database Configuration

### 5.1 Run Command
```powershell
docker run -d `
  --name health-camp-app-task11 `
  -p 8082:8080 `
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME="root" `
  -e DB_PASSWORD="" `
  health-camp-registration:task11
```

- **Container ID:** `c4ea651d3418b88d9c6f764fd6ab73924381a68770bcbde7535b4ee4e61208c8`
- **Database host parameter:** Uses `host.docker.internal` instead of assuming `localhost` means the Windows host.

### 5.2 Container Status and Health
```powershell
docker ps --filter "name=health-camp-app-task11"
```
```
CONTAINER ID   IMAGE                             COMMAND                  STATUS                    PORTS                                         NAMES
c4ea651d3418   health-camp-registration:task11   "java -Djava.securit…"   Up 18 seconds (healthy)   0.0.0.0:8082->8080/tcp, [::]:8082->8080/tcp   health-camp-app-task11
```

---

## 6. Endpoint Verification & Fault Distinction

### 6.1 UI Endpoint Verification (`GET /`)
```powershell
curl.exe -i http://localhost:8082/
```
**Response:**
```http
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 11863

<!DOCTYPE html>
<html lang="en">
<head>
    <title>Health Camp Registration Platform</title>
...
```
*Result:* Web layer and static resource serving (`index.html`) functions inside the container.

---

### 6.2 Application Validation Layer Verification (`POST /api/v1/registrations` - Bad Request)
```powershell
curl.exe -i -X POST http://localhost:8082/api/v1/registrations `
  -H "Content-Type: application/json" `
  -d '{"fullName":"","email":"not-an-email","phone":"123","preferredCampDate":""}'
```
**Response:**
```http
HTTP/1.1 400 
Content-Type: application/json

{
  "timestamp": "2026-10-29T17:52:04.759187153Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    { "field": "phone", "message": "Phone must be 7-20 digits; hyphens and spaces allowed; optional leading '+'" },
    { "field": "preferredCampDate", "message": "Preferred camp date is required" },
    { "field": "email", "message": "Email must be a valid email address" },
    { "field": "fullName", "message": "Full name is required" }
  ]
}
```
*Result:* Request validation operates entirely in memory at the Spring Boot controller/validation layer before contacting any database.

---

### 6.3 Database Connectivity Failure Distinction (`POST /api/v1/registrations` - Valid Payload)
When submitting a syntactically valid registration:
```powershell
$body = @{
    fullName = "Jane Doe"
    email = "jane.doe@example.com"
    phone = "+919876543210"
    preferredCampDate = "2026-11-15"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8082/api/v1/registrations" -Method POST -Body $body -ContentType "application/json"
```
**Response:**
```http
HTTP/1.1 500 Internal Server Error
{"timestamp":"2026-10-29T17:52:44.758+00:00","status":500,"error":"Internal Server Error","path":"/api/v1/registrations"}
```
**Container Log Trace:**
```
com.zaxxer.hikari.pool.HikariPool: HikariPool-1 - Exception during pool initialization.
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago. The driver has not received any packets from the server.
...
Caused by: java.net.SocketException: Network unreachable
...
at org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl.getConnection
at org.hibernate.resource.jdbc.internal.LogicalConnectionManagedImpl.getPhysicalConnection
```
*Result:* Conclusively distinguishes application logic (HTTP 400) from downstream infrastructure/database connectivity unavailability (HTTP 500 Hikari connection exception).

---

## 7. Container Lifecycle Demonstration

The full container lifecycle was executed and verified sequentially:

| Stage | Command Executed | Observed Result | Status Verified |
| --- | --- | --- | --- |
| **1. Create / Run** | `docker run -d --name health-camp-app-task11 -p 8082:8080 ... health-camp-registration:task11` | Returns container ID `c4ea651d3418` | `Up (healthy)` |
| **2. Inspect** | `docker inspect --format "..." health-camp-app-task11` | `Name: /health-camp-app-task11`, `Ports: 8082->8080` | `Running: true` |
| **3. Logs** | `docker logs health-camp-app-task11` | Tomcat started on 8080; welcome page mapped | Verified |
| **4. Stop** | `docker stop health-camp-app-task11` | Returns `health-camp-app-task11` | `Exited (143)` |
| **5. Start** | `docker start health-camp-app-task11` | Container restarted | `Up (health: starting)` |
| **6. Stop** | `docker stop health-camp-app-task11` | Returns `health-camp-app-task11` | `Exited (143)` |
| **7. Remove** | `docker rm health-camp-app-task11` | Returns `health-camp-app-task11` | Container removed |
| **8. Image Retention** | `docker images health-camp-registration:task11` | Image preserved (ID `417994622167`, 167MB) | Image intact |

---

## 8. Regression Verification

To confirm that Docker configuration did not affect existing Maven, Spring Boot, or Selenium tests:
```powershell
mvn test -Dtest=RegistrationSeleniumTest
```
**Output:**
```
[INFO] Running com.healthcamp.selenium.RegistrationSeleniumTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 86.45 s -- in com.healthcamp.selenium.RegistrationSeleniumTest
[INFO] Results:
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
No regressions were introduced. All prior coursework tasks remain functional.

