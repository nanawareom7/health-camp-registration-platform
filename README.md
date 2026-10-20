# Health Camp Registration and Deployment Platform

A Spring Boot platform for registering participants in health camps, reviewing and approving registrations, and tracking participant status.

> **This repository is at the initialization stage (Task 4).** It contains a minimal, buildable application skeleton plus project documentation, GitHub templates, and contribution guidelines. The full registration workflow is **planned** and is **not yet implemented**.

## Status

| Area | Status |
| --- | --- |
| Repository & build setup (Maven, Spring Boot) | Done (skeleton) |
| Application context smoke test (JUnit 5) | Done |
| Participant registration workflow | Planned |
| Registration approval & confirmation | Planned |
| Participant listing & status tracking | Planned |
| REST API | Planned |
| CI/CD & deployment | Planned |

All "Planned" items are described in the `docs/` folder and tracked as GitHub issues. They are intentionally not implemented in this task.

## Planned MVP features

1. **Participant registration and validation** — capture participant details for a health camp and validate required fields.
2. **Registration approval and confirmation** — organizers review submissions, approve or reject them, and confirm to participants.
3. **Participant listing and status tracking** — list participants per camp and track each registration's status.

See `docs/SRS.md` for the full requirements, `docs/api-documentation.md` for the planned endpoints, and `docs/data-model.md` for the planned entities.

## Tech stack

- **Language:** Java 17 (LTS)
- **Framework:** Spring Boot 3.5.x (Spring Web, Spring Data JPA)
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
│   │   ├── java/com/healthcamp/   # Application source (main class only for now)
│   │   └── resources/             # application.yml (safe placeholders)
│   └── test/
│       ├── java/com/healthcamp/   # JUnit 5 tests
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

## Build and test commands

```bash
# Compile and package (runs tests)
mvn clean package

# Run tests only
mvn test

# Run the application (requires MySQL configured via env vars)
mvn spring-boot:run
```

> Tests run against an in-memory H2 database, so MySQL is **not** required to run `mvn test`.

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
