package com.beema.processor;

import com.beema.processor.config.DatabaseConfig;
import com.beema.processor.config.FlinkConfig;
import com.beema.processor.config.KafkaConfig;
import com.beema.processor.model.MessageHookMetadata;
import com.beema.processor.model.RawMessage;
import com.beema.processor.model.TransformedMessage;
import com.beema.processor.processor.JexlMessageTransformer;
import com.beema.processor.repository.MessageHookRepository;
import com.beema.processor.service.JexlTransformService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Beema Message Processor - Flink Streaming Job with Broadcast State Pattern
 *
 * Architecture:
 * 1. Main Stream: Kafka (raw-messages) -> RawMessage
 * 2. Broadcast Stream: Kafka (message-hooks-control) -> MessageHookMetadata
 * 3. Broadcast State: Dynamic JEXL hooks updated in real-time
 * 4. JexlMessageTransformer: Applies hooks from broadcast state
 * 5. Kafka Sink (beema-events) -> TransformedMessage
 *
 * When sys_message_hooks table is updated:
 * - metadata-service emits MessageHookMetadata to control topic
 * - All parallel instances receive the broadcast update
 * - Next messages use the updated hook automatically
 */
public class MessageProcessorJob {
    private static final Logger log = LoggerFactory.getLogger(MessageProcessorJob.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static void main(String[] args) throws Exception {
        log.info("Starting Beema Message Processor Job...");

        // Load configurations
        FlinkConfig flinkConfig = FlinkConfig.fromEnv();
        KafkaConfig kafkaConfig = KafkaConfig.fromEnv();
        DatabaseConfig dbConfig = DatabaseConfig.fromEnv();

        log.info("Configuration loaded: job='{}', kafka='{}', db='{}'",
                flinkConfig.getJobName(), kafkaConfig.getBootstrapServers(), dbConfig.getJdbcUrl());

        // Set up Flink execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(flinkConfig.getParallelism());
        env.enableCheckpointing(flinkConfig.getCheckpointInterval());

        // Initialize JEXL transformation service
        JexlTransformService jexlService = new JexlTransformService();

        // Create Kafka source for raw messages
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaConfig.getBootstrapServers())
                .setTopics(kafkaConfig.getSourceTopicName())
                .setGroupId(kafkaConfig.getGroupId())
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        log.info("Kafka source configured: topic='{}', servers='{}'",
                kafkaConfig.getSourceTopicName(), kafkaConfig.getBootstrapServers());

        // Create Kafka source for control stream (message hook metadata updates)
        String controlTopicName = System.getenv().getOrDefault("KAFKA_CONTROL_TOPIC", "message-hooks-control");
        KafkaSource<String> controlSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaConfig.getBootstrapServers())
                .setTopics(controlTopicName)
                .setGroupId(kafkaConfig.getGroupId() + "-control")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        log.info("Kafka control source configured: topic='{}'", controlTopicName);

        // Create data stream from Kafka (main stream)
        DataStream<String> rawStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka Raw Messages Source"
        );

        // Create broadcast stream from control topic
        DataStream<String> controlStream = env.fromSource(
                controlSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka Control Stream"
        );

        // Parse JSON to RawMessage
        DataStream<RawMessage> parsedStream = rawStream
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RawMessage.class);
                    } catch (Exception e) {
                        log.error("Failed to parse RawMessage: {}", e.getMessage());
                        throw new RuntimeException("JSON parsing failed", e);
                    }
                })
                .name("Parse Raw Message");

        // Parse control stream to MessageHookMetadata
        DataStream<MessageHookMetadata> hookMetadataStream = controlStream
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, MessageHookMetadata.class);
                    } catch (Exception e) {
                        log.error("Failed to parse MessageHookMetadata: {}", e.getMessage());
                        throw new RuntimeException("JSON parsing failed", e);
                    }
                })
                .name("Parse Hook Metadata");

        // Create broadcast stream with MapStateDescriptor
        org.apache.flink.api.common.state.BroadcastStream<MessageHookMetadata> broadcastStream =
                hookMetadataStream.broadcast(JexlMessageTransformer.HOOK_DESCRIPTOR);

        // Connect main stream with broadcast stream and apply JexlMessageTransformer
        DataStream<TransformedMessage> transformedStream = parsedStream
                .connect(broadcastStream)
                .process(new JexlMessageTransformer(jexlService))
                .name("Transform Message with Broadcast State");

        // Filter out failed transformations if needed (optional - already handled in transformer)
        DataStream<TransformedMessage> successfulStream = transformedStream
                .filter(msg -> msg.getResultData() != null && !msg.getResultData().isEmpty())
                .name("Filter Successful Transformations");

        // Create Kafka sink for transformed messages (beema-events topic)
        String sinkTopicName = System.getenv().getOrDefault("KAFKA_SINK_TOPIC", "beema-events");
        KafkaSink<TransformedMessage> kafkaSink = KafkaSink.<TransformedMessage>builder()
                .setBootstrapServers(kafkaConfig.getBootstrapServers())
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<TransformedMessage>builder()
                                .setTopic(sinkTopicName)
                                .setValueSerializationSchema(
                                        (message, timestamp) -> {
                                            try {
                                                String json = objectMapper.writeValueAsString(message);
                                                return json.getBytes(StandardCharsets.UTF_8);
                                            } catch (Exception e) {
                                                log.error("Failed to serialize TransformedMessage: {}", e.getMessage());
                                                return null;
                                            }
                                        }
                                )
                                .build()
                )
                .build();

        log.info("Kafka sink configured: topic='{}'", sinkTopicName);

        // Write to Kafka sink
        successfulStream.sinkTo(kafkaSink).name("Kafka Transformed Messages Sink");

        // Execute job
        log.info("Executing Flink job: '{}'", flinkConfig.getJobName());
        env.execute(flinkConfig.getJobName());
    }
}
