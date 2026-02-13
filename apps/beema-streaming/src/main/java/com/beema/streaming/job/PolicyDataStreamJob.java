package com.beema.streaming.job;

import com.beema.streaming.config.StreamingConfig;
import com.beema.streaming.mapper.PolicyEventMapper;
import com.beema.streaming.model.PolicyEvent;
import com.beema.streaming.model.PolicyFlatRecord;
import com.beema.streaming.serde.PolicyEventDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetWriters;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.connector.file.sink.FileSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Beema Policy Data Stream Job
 *
 * Reads policy change events from Kafka, transforms them into flat Avro records,
 * and writes Parquet files to MinIO (s3a://) with date/hour partitioning.
 *
 * Architecture:
 * 1. Kafka Source: beema.events.policy_change topic
 * 2. Deserialize: JSON -> PolicyEvent
 * 3. Map: PolicyEvent -> PolicyFlatRecord (flat Avro-compatible POJO)
 * 4. Convert: PolicyFlatRecord -> GenericRecord (Avro)
 * 5. Parquet Sink: GenericRecord -> s3a://beema-datalake/speed/policy/{date}/{hour}/
 *
 * Rolling Policy: OnCheckpointRollingPolicy (every 5 minutes or 128 MB)
 * Partitioning: yyyy-MM-dd/HH (UTC)
 */
public class PolicyDataStreamJob {
    private static final Logger log = LoggerFactory.getLogger(PolicyDataStreamJob.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Beema Policy Data Stream Job...");

        // Load configuration from environment
        StreamingConfig config = StreamingConfig.fromEnv();
        log.info("Configuration loaded: {}", config);

        // Configure Hadoop for S3/MinIO access
        configureS3(config);

        // Set up Flink execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        configureEnvironment(env, config);

        // Build Kafka source
        KafkaSource<PolicyEvent> kafkaSource = buildKafkaSource(config);

        // Read from Kafka
        DataStream<PolicyEvent> eventStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka Policy Events Source"
        );

        // Filter null events (failed deserialization)
        DataStream<PolicyEvent> validEvents = eventStream
                .filter(event -> event != null)
                .name("Filter Valid Events");

        // Map PolicyEvent -> PolicyFlatRecord
        DataStream<PolicyFlatRecord> flatRecords = validEvents
                .map(new PolicyEventMapper())
                .name("Map to Flat Record");

        // Convert to Avro GenericRecord for Parquet writing
        DataStream<GenericRecord> avroRecords = flatRecords
                .map(PolicyFlatRecord::toGenericRecord)
                .returns(GenericRecord.class)
                .name("Convert to Avro GenericRecord");

        // Build Parquet file sink
        FileSink<GenericRecord> parquetSink = buildParquetSink(config);

        // Write to MinIO
        avroRecords.sinkTo(parquetSink).name("Parquet Sink to MinIO");

        // Execute
        log.info("Executing Flink job: '{}'", config.getJobName());
        env.execute(config.getJobName());
    }

    private static void configureS3(StreamingConfig config) {
        // Configure Hadoop filesystem for s3a:// with MinIO
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        hadoopConf.set("fs.s3a.endpoint", config.getS3Endpoint());
        hadoopConf.set("fs.s3a.access.key", config.getS3AccessKey());
        hadoopConf.set("fs.s3a.secret.key", config.getS3SecretKey());
        hadoopConf.set("fs.s3a.path.style.access", String.valueOf(config.isS3PathStyleAccess()));
        hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        hadoopConf.set("fs.s3a.connection.ssl.enabled", "false");

        // Set as system properties for Flink's file system to pick up
        System.setProperty("fs.s3a.endpoint", config.getS3Endpoint());
        System.setProperty("fs.s3a.access.key", config.getS3AccessKey());
        System.setProperty("fs.s3a.secret.key", config.getS3SecretKey());
        System.setProperty("fs.s3a.path.style.access", String.valueOf(config.isS3PathStyleAccess()));
        System.setProperty("fs.s3a.connection.ssl.enabled", "false");

        log.info("S3/MinIO configured: endpoint={}, pathStyleAccess={}",
                config.getS3Endpoint(), config.isS3PathStyleAccess());
    }

    private static void configureEnvironment(StreamExecutionEnvironment env, StreamingConfig config) {
        env.setParallelism(config.getParallelism());

        // Checkpointing: exactly-once, every 5 minutes
        env.enableCheckpointing(config.getCheckpointIntervalMs(), CheckpointingMode.EXACTLY_ONCE);
        CheckpointConfig cpConfig = env.getCheckpointConfig();
        cpConfig.setMinPauseBetweenCheckpoints(30_000L);
        cpConfig.setCheckpointTimeout(120_000L);
        cpConfig.setMaxConcurrentCheckpoints(1);
        cpConfig.setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
        );

        log.info("Flink environment configured: parallelism={}, checkpointInterval={}ms",
                config.getParallelism(), config.getCheckpointIntervalMs());
    }

    private static KafkaSource<PolicyEvent> buildKafkaSource(StreamingConfig config) {
        KafkaSource<PolicyEvent> source = KafkaSource.<PolicyEvent>builder()
                .setBootstrapServers(config.getKafkaBootstrapServers())
                .setTopics(config.getKafkaSourceTopic())
                .setGroupId(config.getKafkaGroupId())
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new PolicyEventDeserializer())
                .build();

        log.info("Kafka source configured: topic='{}', servers='{}', groupId='{}'",
                config.getKafkaSourceTopic(), config.getKafkaBootstrapServers(), config.getKafkaGroupId());

        return source;
    }

    private static FileSink<GenericRecord> buildParquetSink(StreamingConfig config) {
        String outputPath = config.getS3OutputPath();

        // Date/Hour bucket assigner: produces paths like 2024-01-15/14/
        DateTimeBucketAssigner<GenericRecord> bucketAssigner =
                new DateTimeBucketAssigner<>("yyyy-MM-dd/HH");

        // Output file naming
        OutputFileConfig fileConfig = OutputFileConfig.builder()
                .withPartPrefix("policy")
                .withPartSuffix(".parquet")
                .build();

        FileSink<GenericRecord> sink = FileSink
                .forBulkFormat(
                        new Path(outputPath),
                        AvroParquetWriters.forGenericRecord(PolicyFlatRecord.AVRO_SCHEMA)
                )
                .withBucketAssigner(bucketAssigner)
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .withOutputFileConfig(fileConfig)
                .build();

        log.info("Parquet sink configured: outputPath='{}', rollingPolicy=OnCheckpoint", outputPath);
        return sink;
    }
}
