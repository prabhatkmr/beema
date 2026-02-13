package com.beema.streaming.config;

import java.io.Serializable;

/**
 * Unified configuration for the Policy Data Stream job.
 * Reads from environment variables with sensible defaults.
 */
public class StreamingConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    // Kafka
    private final String kafkaBootstrapServers;
    private final String kafkaGroupId;
    private final String kafkaSourceTopic;

    // S3 / MinIO
    private final String s3Endpoint;
    private final String s3AccessKey;
    private final String s3SecretKey;
    private final boolean s3PathStyleAccess;
    private final String s3OutputPath;

    // Flink job
    private final String jobName;
    private final int parallelism;
    private final long checkpointIntervalMs;

    // Parquet rolling policy
    private final long maxPartSizeBytes;
    private final long rollingIntervalMs;
    private final long inactivityIntervalMs;

    private StreamingConfig(Builder builder) {
        this.kafkaBootstrapServers = builder.kafkaBootstrapServers;
        this.kafkaGroupId = builder.kafkaGroupId;
        this.kafkaSourceTopic = builder.kafkaSourceTopic;
        this.s3Endpoint = builder.s3Endpoint;
        this.s3AccessKey = builder.s3AccessKey;
        this.s3SecretKey = builder.s3SecretKey;
        this.s3PathStyleAccess = builder.s3PathStyleAccess;
        this.s3OutputPath = builder.s3OutputPath;
        this.jobName = builder.jobName;
        this.parallelism = builder.parallelism;
        this.checkpointIntervalMs = builder.checkpointIntervalMs;
        this.maxPartSizeBytes = builder.maxPartSizeBytes;
        this.rollingIntervalMs = builder.rollingIntervalMs;
        this.inactivityIntervalMs = builder.inactivityIntervalMs;
    }

    public static StreamingConfig fromEnv() {
        return new Builder()
                .kafkaBootstrapServers(env("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092"))
                .kafkaGroupId(env("KAFKA_GROUP_ID", "beema-policy-streaming"))
                .kafkaSourceTopic(env("KAFKA_SOURCE_TOPIC", "beema.events.policy_change"))
                .s3Endpoint(env("S3_ENDPOINT", "http://minio:9000"))
                .s3AccessKey(env("S3_ACCESS_KEY", "admin"))
                .s3SecretKey(env("S3_SECRET_KEY", "password123"))
                .s3PathStyleAccess(Boolean.parseBoolean(env("S3_PATH_STYLE_ACCESS", "true")))
                .s3OutputPath(env("S3_OUTPUT_PATH", "s3a://beema-datalake/speed/policy/"))
                .jobName(env("FLINK_JOB_NAME", "beema-policy-data-stream"))
                .parallelism(Integer.parseInt(env("FLINK_PARALLELISM", "2")))
                .checkpointIntervalMs(Long.parseLong(env("FLINK_CHECKPOINT_INTERVAL", "300000")))
                .maxPartSizeBytes(Long.parseLong(env("PARQUET_MAX_PART_SIZE_MB", "128")) * 1024 * 1024)
                .rollingIntervalMs(Long.parseLong(env("PARQUET_ROLLING_INTERVAL", "300000")))
                .inactivityIntervalMs(Long.parseLong(env("PARQUET_INACTIVITY_INTERVAL", "60000")))
                .build();
    }

    private static String env(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    // --- Getters ---

    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public String getKafkaGroupId() { return kafkaGroupId; }
    public String getKafkaSourceTopic() { return kafkaSourceTopic; }
    public String getS3Endpoint() { return s3Endpoint; }
    public String getS3AccessKey() { return s3AccessKey; }
    public String getS3SecretKey() { return s3SecretKey; }
    public boolean isS3PathStyleAccess() { return s3PathStyleAccess; }
    public String getS3OutputPath() { return s3OutputPath; }
    public String getJobName() { return jobName; }
    public int getParallelism() { return parallelism; }
    public long getCheckpointIntervalMs() { return checkpointIntervalMs; }
    public long getMaxPartSizeBytes() { return maxPartSizeBytes; }
    public long getRollingIntervalMs() { return rollingIntervalMs; }
    public long getInactivityIntervalMs() { return inactivityIntervalMs; }

    @Override
    public String toString() {
        return "StreamingConfig{" +
                "kafkaBootstrapServers='" + kafkaBootstrapServers + '\'' +
                ", kafkaSourceTopic='" + kafkaSourceTopic + '\'' +
                ", s3OutputPath='" + s3OutputPath + '\'' +
                ", jobName='" + jobName + '\'' +
                ", parallelism=" + parallelism +
                ", checkpointIntervalMs=" + checkpointIntervalMs +
                '}';
    }

    public static class Builder {
        private String kafkaBootstrapServers;
        private String kafkaGroupId;
        private String kafkaSourceTopic;
        private String s3Endpoint;
        private String s3AccessKey;
        private String s3SecretKey;
        private boolean s3PathStyleAccess;
        private String s3OutputPath;
        private String jobName;
        private int parallelism;
        private long checkpointIntervalMs;
        private long maxPartSizeBytes;
        private long rollingIntervalMs;
        private long inactivityIntervalMs;

        public Builder kafkaBootstrapServers(String v) { this.kafkaBootstrapServers = v; return this; }
        public Builder kafkaGroupId(String v) { this.kafkaGroupId = v; return this; }
        public Builder kafkaSourceTopic(String v) { this.kafkaSourceTopic = v; return this; }
        public Builder s3Endpoint(String v) { this.s3Endpoint = v; return this; }
        public Builder s3AccessKey(String v) { this.s3AccessKey = v; return this; }
        public Builder s3SecretKey(String v) { this.s3SecretKey = v; return this; }
        public Builder s3PathStyleAccess(boolean v) { this.s3PathStyleAccess = v; return this; }
        public Builder s3OutputPath(String v) { this.s3OutputPath = v; return this; }
        public Builder jobName(String v) { this.jobName = v; return this; }
        public Builder parallelism(int v) { this.parallelism = v; return this; }
        public Builder checkpointIntervalMs(long v) { this.checkpointIntervalMs = v; return this; }
        public Builder maxPartSizeBytes(long v) { this.maxPartSizeBytes = v; return this; }
        public Builder rollingIntervalMs(long v) { this.rollingIntervalMs = v; return this; }
        public Builder inactivityIntervalMs(long v) { this.inactivityIntervalMs = v; return this; }

        public StreamingConfig build() {
            return new StreamingConfig(this);
        }
    }
}
