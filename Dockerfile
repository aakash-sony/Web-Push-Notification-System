# ==========================================
# Stage 1: Build the application JAR
# ==========================================
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Grant execute permissions to Maven wrapper
RUN chmod +x ./mvnw

# Pre-download dependencies to leverage Docker layer caching
RUN ./mvnw dependency:go-offline -B

# Copy source code and build production JAR
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal, secure runtime container
# ==========================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Render dynamically passes PORT (fallback to 8080)
ENV PORT=8080
EXPOSE 8080

# Health check to ensure the container is responsive
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1

# JVM container memory optimizations for Render's 512MB Free Tier:
# 1. -XX:+UseContainerSupport: Detects container cgroup limits
# 2. -XX:MaxRAMPercentage=75.0: Limits heap to ~384MB, leaving headroom for Metaspace/OS to prevent OOM (Exit 137)
# 3. -Dserver.port=${PORT}: Binds dynamically to Render's exposed port
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Dserver.port=${PORT}", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
