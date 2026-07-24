# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven Wrapper and POM file first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Grant execution permission to Maven wrapper script
RUN chmod +x mvnw

# Download dependencies to leverage Docker layer caching
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Package application jar into standard /app/target output directory
RUN ./mvnw clean package -DskipTests -Dproject.build.directory=/app/target

# Stage 2: Production Lightweight Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for enhanced runtime security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled executable jar artifact from builder stage target directory
COPY --from=builder /tmp/poultry-target/poultry-backend-0.0.1-SNAPSHOT.jar app.jar

# Set non-root ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Render and cloud platforms supply PORT dynamically at runtime
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
