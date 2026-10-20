# Health Camp Registration and Deployment Platform

A Spring Boot platform for registering participants in health camps, reviewing and approving registrations, and tracking participant status.

> **Repository status:** The application skeleton and the participant registration endpoint (Feature 1) are implemented. Registration approval, participant listing, and CI/CD deployment are still **planned** (see the `docs/` folder).

## Status

| Area | Status |
| --- | --- |
| Repository & build setup (Maven, Spring Boot) | Done |
| Application context smoke test (JUnit 5) | Done |
| Participant registration & validation (Feature 1) | Done |
| Registration approval & confirmation | Planned |
| Participant listing & status tracking | Planned |
| CI/CD & deployment | Planned |

Planned items are described in the `docs/` folder and tracked as GitHub issues.

## Planned MVP features

1. **Participant registration and validation** — capture participant details for a health camp and validate required fields. (Implemented as Feature 1.)
2. **Registration approval and confirmation** — organizers review submissions, approve or reject them, and confirm to participants.
3. **Participant listing and status tracking** — list participants per camp and track each registration's status.

See `docs/SRS.md` for the full requirements, `docs/api-documentation.md` for the planned endpoints, and `docs/data-model.md` for the planned entities.

## Tech stack

- **Language:** Java 17 (LTS)
- **Framework:** Spring Boot 3.5.x (Spring Web, Spring Data JPA, Bean Validation)
- **Build:** Maven
- **Database:** MySQL (H2 in-memory for automated tests)
- **Testing:** JUnit 5 (Jupiter), Spring Boot Test, AssertJ, Mockito (provided by `spring-boot-starter-test`)

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
├── docs/                          # SRS, architecture, data model, API docs, deployment
├── src/
│   ├── main/
│   │   ├── java/com/healthcamp/   # Application + registration (entity, service, controller, DTOs, handler)
│   │   └── resources/             # application.yml (safe placeholders)
│   └── test/
│       ├── java/com/healthcamp/   # JUnit 5 tests (context + registration)
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
# Compile and package (runs tests)
mvn clean package

# Run tests only
mvn test

# Run the application (requires MySQL configured via env vars + the registrations table)
mvn spring-boot:run
```

> Tests run against an in-memory H2 database, so MySQL is **not** required to run `mvn test`.

## Registration API (Feature 1)

The registration endpoint is implemented and tested.

### POST /api/v1/registrations

Registers a participant for a health camp. Creates a registration with status `PENDING`.

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

Success response `201 Created`:
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
A `Location` header is set to `/api/v1/registrations/{id}`.

Validation error response `400 Bad Request`:
```json
{
  "timestamp": "2026-10-20T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "Email must be a valid email address" },
    { "field": "phone", "message": "Phone must be 7-20 digits; hyphens and spaces allowed; optional leading '+'" }
  ]
}
```

### Try it

After starting the app (see Setup and Build above):
```bash
curl -i -X POST http://localhost:8080/api/v1/registrations \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","phone":"+919876543210","preferredCampDate":"2026-10-28"}'
```

## Deployment overview

See `docs/deployment.md` for the full plan. In summary:

- Build a runnable JAR: `mvn clean package`.
- Run the JAR with environment-specific config: `java -jar target/health-camp-registration-platform-0.0.1-SNAPSHOT.jar`.
- Recommended targets: a container image (Docker) deployed to a VM or a container platform; environment variables supply database and server settings.
- CI (planned): run `mvn verify` on pushes and pull requests.

## Contributing

See `CONTRIBUTING.md` for branch naming, commit message conventions, and pull request and review rules.

## License

Not yet selected. A `LICENSE` file will be added once the project owner chooses a license.
