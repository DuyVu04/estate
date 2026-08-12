# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Build profile parameter (defaults to 'dev', can be overridden via --build-arg MAVEN_PROFILE=prod)
ARG MAVEN_PROFILE=dev

# Copy only pom.xml first so dependency layer is cached
# and only re-downloaded when pom.xml actually changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build — using the flexible MAVEN_PROFILE build argument
COPY src ./src
RUN mvn clean package -P ${MAVEN_PROFILE} -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=builder /app/target/*.jar api-service.jar

# Optional: install wget for the docker-compose healthcheck (Alpine doesn't ship it by default)
RUN apk add --no-cache wget

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Duser.timezone=UTC", "-jar", "api-service.jar"]