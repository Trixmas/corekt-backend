# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom first to leverage Docker cache
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -q clean package -DskipTests


# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Enable production profile and add JVM optimizations
ENTRYPOINT ["java", \
    "-Dspring.profiles.active=prod", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "app.jar"]
