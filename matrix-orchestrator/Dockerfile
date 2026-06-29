# Stage 1: Build and compile bytecode
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
# Cache dependencies cleanly
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: High-Performance Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. Create a non-root user and group for Alpine Linux
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Grab compiled jar artifact from the builder phase
COPY --from=builder /build/target/matrix-orchestrator-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# 2. Grant the non-root user ownership of the executable
RUN chown appuser:appgroup app.jar

# 3. Switch to the non-root user before running the application
USER appuser

# Run the bundled Fat JAR
CMD ["java", "-jar", "app.jar"]