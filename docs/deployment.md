# Deployment
## Health Camp Registration and Deployment Platform

> **Status:** Planned. This describes the intended deployment approach. No deployment artifacts (Dockerfile, CI workflow) exist yet.

## 1. Build
```bash
mvn clean package
# Produces target/health-camp-registration-platform-0.0.1-SNAPSHOT.jar
```

## 2. Runtime configuration (environment variables)
| Variable | Purpose | Example |
| --- | --- | --- |
| `DB_URL` | JDBC connection string | `jdbc:mysql://...` |
| `DB_USERNAME` | Database user | `app` |
| `DB_PASSWORD` | Database password | (secret, never committed) |
| `SERVER_PORT` | HTTP port | `8080` |

Never commit real values. Provide them through your deployment environment or a secrets manager.

## 3. Running the JAR
```bash
java -jar target/health-camp-registration-platform-0.0.1-SNAPSHOT.jar
```

## 4. Containerization (planned)
- Build a container image from an Eclipse Temurin JDK 17 base image.
- Run with environment variables injected.
- A `Dockerfile` will be added when the application has runnable endpoints.

## 5. Environments (planned)
- **Dev:** local MySQL or Docker Compose.
- **Test:** automated `mvn verify` against H2.
- **Prod:** MySQL managed service + containerized app behind a reverse proxy.

## 6. CI/CD (planned)
- On push / PR: `mvn clean verify`.
- On merge to `main`: build image, push to registry, deploy.
- Rollback: redeploy the previous image tag.

## 7. Database provisioning (planned)
```sql
CREATE DATABASE health_camp_db;
```
Schema migrations (planned, e.g. Flyway) will manage tables once entities are implemented.
