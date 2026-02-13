# Multi-stage Dockerfile for Beema Message Processor

# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with Flink
FROM flink:1.18.1-java17
WORKDIR /opt/flink

# Copy the fat JAR from build stage
COPY --from=build /app/target/beema-message-processor-*.jar /opt/flink/usrlib/beema-message-processor.jar

# Set environment variables
ENV FLINK_JOB_NAME=beema-message-processor
ENV FLINK_PARALLELISM=1
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=beema_kernel

# Expose Flink web UI port
EXPOSE 8081

# Run Flink job
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["standalone-job", "--job-classname", "com.beema.processor.MessageProcessorJob"]
