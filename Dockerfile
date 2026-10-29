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

# Compile and package Spring Boot executable JAR (skipping unit/Selenium tests during container build)
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
# Host-to-container DB connectivity uses host.docker.internal for reaching Windows host services
ENV SERVER_PORT=8080 \
    DB_URL="jdbc:mysql://host.docker.internal:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    DB_USERNAME="root"

# Expose internal container port
EXPOSE 8080

# Health check probe against Spring Boot HTTP endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Launch Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

