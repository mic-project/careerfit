# Multi-stage Dockerfile for ECS deployment

# Build stage
FROM gradle:8.8.0-jdk17 AS build
WORKDIR /app

# Copy Gradle files first for better layer caching
COPY backend/build.gradle ./
COPY backend/gradle/ gradle/
COPY backend/gradlew ./

# Download dependencies
RUN gradle build --no-daemon -x test || return 0

# Copy source code and build
COPY backend/src/ src/
RUN gradle bootJar --no-daemon -x test

# Runtime stage with optimized JRE
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install wget for health check
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash -m appuser

# Copy JAR file from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create uploads directory with proper permissions
RUN mkdir -p /app/uploads && \
    chown -R appuser:appgroup /app && \
    chmod -R 755 /app/uploads

USER appuser

# Health check for ECS
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/health || exit 1

# Expose port
EXPOSE 8080

# Environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]