# ==============================================================================
# Stage 1: Build
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy dependency descriptor first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ==============================================================================
# Stage 2: Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S beema && adduser -S beema -G beema

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R beema:beema /app

USER beema

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
