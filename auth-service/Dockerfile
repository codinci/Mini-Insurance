# Build from the repository root:
#   docker build -f auth-service/Dockerfile -t mini-insurance/auth-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
# Resolve dependencies first so code changes do not re-download everything.
RUN mvn -B -q -pl auth-service -am dependency:go-offline
COPY auth-service/src ./auth-service/src
RUN mvn -B -q -pl auth-service -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
USER app
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8081/api/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
