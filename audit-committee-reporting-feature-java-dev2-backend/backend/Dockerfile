# ── Stage 1: Build the JAR file ────────────────────────────────────────────
# Uses Maven to compile Java code and package it into a .jar file
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first — Docker caches dependencies if pom.xml hasn't changed
# This makes rebuilds MUCH faster
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run the JAR ───────────────────────────────────────────────────
# Uses a smaller image — no Maven needed at runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080 for Docker Compose
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]