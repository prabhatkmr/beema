# Multi-stage Dockerfile for Beema Streaming (Flink Policy Data Stream Job)

# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Prepare optional Flink libs (hadoop-aws, aws-sdk may be needed in Flink's lib/)
RUN mkdir -p /app/flink-libs && \
    cp /app/target/dependency/hadoop-aws-*.jar /app/flink-libs/ 2>/dev/null; \
    cp /app/target/dependency/aws-java-sdk-bundle-*.jar /app/flink-libs/ 2>/dev/null; \
    true

# Stage 2: Runtime with Flink
FROM flink:1.18.1
WORKDIR /opt/flink

# Copy the fat JAR from build stage
COPY --from=build /app/target/beema-streaming-*.jar /opt/flink/usrlib/beema-streaming.jar

# Copy Hadoop/S3 libs if available
COPY --from=build /app/flink-libs/ /opt/flink/lib/

# Set environment defaults
ENV FLINK_JOB_NAME=beema-policy-data-stream
ENV FLINK_PARALLELISM=2
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:29092
ENV KAFKA_SOURCE_TOPIC=beema.events.policy_change
ENV S3_ENDPOINT=http://minio:9000
ENV S3_ACCESS_KEY=admin
ENV S3_SECRET_KEY=password123
ENV S3_OUTPUT_PATH=s3a://beema-datalake/speed/policy/

# Expose Flink web UI port
EXPOSE 8081

# Run Flink job
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["standalone-job", "--job-classname", "com.beema.streaming.job.PolicyDataStreamJob"]
