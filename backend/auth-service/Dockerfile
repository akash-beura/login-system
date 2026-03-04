# Uses the JAR built by 'mvn clean install' locally.
# Run 'mvn clean install -DskipTests' before 'docker compose up --build'.
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
