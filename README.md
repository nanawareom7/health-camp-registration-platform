# Health Camp Registration and Deployment Platform

A Spring Boot platform for registering participants in health camps, reviewing and approving registrations, and tracking participant status.

> **Repository status:** Registration (Feature 1), participant listing, individual status tracking, and approval/rejection (Task 6) are implemented and tested. CI/CD deployment is still **planned** (see the `docs/` folder).

## Status

| Area | Status |
| --- | --- |
| Repository & build setup (Maven, Spring Boot) | Done |
| Application context smoke test (JUnit 5) | Done |
| Participant registration & validation (Feature 1) | Done |
| Participant listing (with status filter) (Task 6) | Done |
| Individual status tracking (get by id) (Task 6) | Done |
| Registration approval & rejection (Task 6) | Done |
| Authentication / authorization on admin endpoints | Not done (demo-only) |
| Camp management / capacity / pagination / confirmation | Planned (see Limitations) |
| CI/CD & deployment | Planned |

Verified by `mvn clean verify`: **19 tests, 0 failures, 0 errors** (see "Verification" below). Planned items are described in the `docs/` folder.

## Implemented MVP features

1. **Participant registration and validation** — `POST /api/v1/registrations` captures full name, email, phone, and preferred camp date; validates fields; creates a record with status `PENDING`.
2. **Participant listing** — `GET /api/v1/registrations[?status=]` lists registrations, optionally filtered by status.
3. **Individual status tracking** — `GET /api/v1/registrations/{id}` returns a single registration or `404`.
4. **Registration approval & rejection** — `PATCH /api/v1/registrations/{id}/status` transitions a `PENDING` registration to `APPROVED` or `REJECTED` and persists the change. Invalid transitions are rejected with `409`.

See `docs/SRS.md` for the full requirements, `docs/api-documentation.md` for the implemented endpoints, and `docs/data-model.md` for the planned entities.

## Tech stack

- **Language:** Java 17 (LTS) — the project targets Java 17; it also builds under newer JDKs (verified with JDK 27 via Maven `--release 17`).
- **Framework:** Spring Boot 3.5.x (Spring Web, Spring Data JPA, Bean Validation)
- **Build:** Maven
- **Database:** MySQL (H2 in-memory for automated tests)
- **Testing:** JUnit 5 (Jupiter), Spring Boot Test, AssertJ, Hamcrest, Mockito (provided by `spring-boot-starter-test`)

## Prerequisites

- JDK 17 or later
- Maven 3.9+
- MySQL 8.x (only required to run the application locally; tests use H2)
- Git

## Project structure

```
health-camp-registration-platform/
├── .github/
│   ├── ISSUE_TEMPLATE/            # Bug report, feature request, issue config
│   └── pull_request_template.md
├── docs/                          # SRS, architecture, data model, API docs, deployment, backlog
├── src/
│   ├── main/
│   │   ├── java/com/healthcamp/   # Application + registration (entity, service, controller, DTOs, status, exceptions) + common handler
│   │   └── resources/             # application.yml (safe placeholders)
│   └── test/
│       ├── java/com/healthcamp/   # JUnit 5 tests (context + registration + workflow)
│       └── resources/             # application-test.yml (H2)
├── CONTRIBUTING.md
├── README.md
└── pom.xml
```

## Setup

1. Clone the repository.
2. Create a MySQL database (only needed to run the app, not to run tests):
   ```sql
   CREATE DATABASE health_camp_db;
   ```
3. Provide database credentials via environment variables (never commit real secrets).

   Linux / macOS:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   export DB_USERNAME="your_user"
   export DB_PASSWORD="your_password"
   ```
   Windows PowerShell:
   ```powershell
   $env:DB_URL="jdbc:mysql://localhost:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:DB_USERNAME="your_user"
   $env:DB_PASSWORD="your_password"
   ```
4. Create the `registrations` table (only needed to run the app; tests use H2 and create the schema automatically). The app uses `ddl-auto: none`, so the table must exist:
   ```sql
   USE health_camp_db;
   CREATE TABLE IF NOT EXISTS registrations (
       id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
       full_name VARCHAR(255) NOT NULL,
       email VARCHAR(255) NOT NULL,
       phone VARCHAR(255) NOT NULL,
       preferred_camp_date DATE NOT NULL,
       status VARCHAR(255) NOT NULL,
       created_at TIMESTAMP NOT NULL
   ) ENGINE=InnoDB;
   ```

## Build and test commands

```bash
# Compile, run tests, package, and run verify checks
mvn clean verify

# Run tests only
mvn test

# Run the application (requires MySQL configured via env vars + the registrations table)
mvn spring-boot:run
```

> Tests run against an in-memory H2 database, so MySQL is **not** required to run `mvn test` or `mvn clean verify`.

## Registration & workflow API (Features 1 & 6)

Base path: `/api/v1`. All endpoints are implemented and tested. The listing, get-by-id, and status-update endpoints are organizer/admin operations — see the **Security / demo-only** note below.

### POST /api/v1/registrations — create
Registers a participant. Creates a registration with status `PENDING`.

Request body:
```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+919876543210",
  "preferredCampDate": "2026-10-28"
}
```

Field rules:

| Field | Rule |
| --- | --- |
| `fullName` | Required, not blank. |
| `email` | Required, valid email format. |
| `phone` | Required, 7-20 digits; hyphens/spaces allowed; optional leading `+`. |
| `preferredCampDate` | Required, ISO date (`yyyy-MM-dd`), must be a future date. |

Success `201 Created` (with `Location: /api/v1/registrations/{id}`):
```json
{
  "id": 1,
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+919876543210",
  "preferredCampDate": "2026-10-28",
  "status": "PENDING",
  "createdAt": "2026-10-20T12:00:00Z"
}
```

### GET /api/v1/registrations — list
Lists all registrations ordered by id, optionally filtered by status.

- `GET /api/v1/registrations` — all registrations.
- `GET /api/v1/registrations?status=PENDING` — only `PENDING` (also `APPROVED`, `REJECTED`).

Success `200 OK`:
```json
[
  { "id": 1, "fullName": "Jane Doe", "email": "jane@example.com", "phone": "+919876543210", "preferredCampDate": "2026-10-28", "status": "PENDING", "createdAt": "2026-10-20T12:00:00Z" }
]
```
An unknown `status` value returns `400 Bad Request`. A known status with no matches returns `200` with an empty array.

### GET /api/v1/registrations/{id} — individual status tracking
Success `200 OK` with the registration, or `404 Not Found` if the id does not exist.

### PATCH /api/v1/registrations/{id}/status — approve / reject
Transitions a `PENDING` registration to `APPROVED` or `REJECTED` and persists the new status.

Request body:
```json
{ "status": "APPROVED" }
```

Success `200 OK` with the updated registration. Status rules:

| Current status | Allowed target |
| --- | --- |
| `PENDING` | `APPROVED` or `REJECTED` |
| `APPROVED` | none (terminal) |
| `REJECTED` | none (terminal) |

- `400 Bad Request` — `status` missing/blank, or not `APPROVED`/`REJECTED` (e.g. `PENDING`).
- `404 Not Found` — registration id does not exist.
- `409 Conflict` — transition not allowed from the current state (e.g. approving an already-`APPROVED` registration).

### Error response shape (all errors)
```json
{
  "timestamp": "2026-10-20T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [ { "field": "email", "message": "Email must be a valid email address" } ]
}
```
The `errors` array is present for Bean Validation failures; `status`/`error`/`message` are always present.

### Try it
```bash
# Register
curl -i -X POST http://localhost:8080/api/v1/registrations \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","phone":"+919876543210","preferredCampDate":"2026-10-28"}'

# List pending
curl -i "http://localhost:8080/api/v1/registrations?status=PENDING"

# Get one
curl -i http://localhost:8080/api/v1/registrations/1

# Approve
curl -i -X PATCH http://localhost:8080/api/v1/registrations/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"APPROVED"}'
```

## Verification

Built and tested with `mvn clean verify` (Maven 3.9.16, JDK 27 compiling to release 17, H2 test profile):

| Test class | Tests | Failures | Errors |
| --- | --- | --- | --- |
| `HealthCampApplicationTests` | 1 | 0 | 0 |
| `RegistrationControllerTest` | 3 | 0 | 0 |
| `RegistrationWorkflowTest` | 15 | 0 | 0 |
| **Total** | **19** | **0** | **0** |

Result: `BUILD SUCCESS`. Reports: `target/surefire-reports/`.

## Security / demo-only endpoints

> The `GET` (list), `GET /{id}`, and `PATCH /{id}/status` endpoints are organizer/admin operations. They are **unprotected**: there is currently **no authentication or authorization**. They are **demo-only** and must not be exposed to untrusted networks without adding auth first. Adding auth (e.g. Spring Security with role-based access) is a planned follow-up.

## Limitations (not yet implemented)

- **No authentication/authorization** — admin endpoints are demo-only (see above).
- **Simplified data model** — a single `Registration` entity (full name, email, phone, preferred camp date, status, created at). The planned `HealthCamp` entity, `campId`, camp-based listing, and capacity enforcement (`docs/data-model.md`, `docs/SRS.md` FR-3.4) are not implemented.
- **No duplicate check** — registering the same email multiple times is allowed (planned: unique registration per email+camp).
- **No pagination** — listing returns the full result set (planned: `page`/`size`).
- **No `CONFIRMED` state or notifications** — only `PENDING`/`APPROVED`/`REJECTED` are implemented; approval->confirmation and email/SMS notifications are planned.
- **No rejection reason storage** — `PATCH` accepts only `status` (planned: optional `reason`).
- **Manual schema management for the app** — `ddl-auto: none` with the provided DDL; no Flyway/Liquibase migrations yet.

## Deployment overview

See `docs/deployment.md` for the full plan. In summary:

- Build a runnable JAR: `mvn clean package`.
- Run the JAR with environment-specific config: `java -jar target/health-camp-registration-platform-0.0.1-SNAPSHOT.jar`.
- Recommended targets: a container image (Docker) deployed to a VM or a container platform; environment variables supply database and server settings.
- CI (planned): run `mvn verify` on pushes and pull requests.

---

## Docker Container Lifecycle (Task 11)

A multi-stage `Dockerfile` is provided at the repository root for containerized deployment using Eclipse Temurin Java 17 and Alpine Linux.

### Building the image

```bash
docker build -t health-camp-registration:task11 .
```

### Running the container

To avoid conflict with Jenkins (port 8080) and Jenkins deployment (port 8081), map host port `8082` to container port `8080`:

```bash
docker run -d \
  --name health-camp-app-task11 \
  -p 8082:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="" \
  health-camp-registration:task11
```

> **Note on Windows Host Networking:** When running MySQL on the Windows host machine, use `host.docker.internal` rather than `localhost` inside the container.

### Container Lifecycle Commands

```bash
# View running container and health status
docker ps --filter "name=health-camp-app-task11"

# View logs
docker logs -f health-camp-app-task11

# Stop container
docker stop health-camp-app-task11

# Start container
docker start health-camp-app-task11

# Stop and remove container
docker stop health-camp-app-task11
docker rm health-camp-app-task11
```

### Application URLs (Container)

- Registration UI: `http://localhost:8082/`
- API Endpoint: `http://localhost:8082/api/v1/registrations`

See `docs/task-11-docker-lifecycle-evidence.md` for full execution evidence and lifecycle outputs.

## Contributing

See `CONTRIBUTING.md` for branch naming, commit message conventions, and pull request and review rules.

## License

Not yet selected. A LICENSE file will be added once the project owner chooses a license.
