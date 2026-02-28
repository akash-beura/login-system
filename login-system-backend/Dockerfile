# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom first so dependency layer is cached
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and package (skip tests — run them in CI separately)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

# Non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
