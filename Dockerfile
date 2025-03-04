# Use JDK 21 as the base image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy gradle files for dependency resolution
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy source code
COPY src ./src
COPY gradle.properties ./gradle.properties

# Build the application
RUN ./gradlew bootJar --no-daemon

# Create directory for file storage
RUN mkdir -p files

# Runtime configuration
EXPOSE 8080

# Set Railway-specific environment variable to use the assigned port
ENV PORT=8080

# Entry point to run the application
ENTRYPOINT ["java", "-jar", "/app/build/libs/*.jar"]
