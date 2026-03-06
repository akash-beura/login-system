# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Cache dependencies first (re-downloads only when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
