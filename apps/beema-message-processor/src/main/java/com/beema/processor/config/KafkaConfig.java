package com.beema.processor.config;

import java.io.Serializable;
import java.util.Properties;

/**
 * Kafka configuration for Flink connectors.
 */
public class KafkaConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String bootstrapServers;
    private final String groupId;
    private final String sourceTopicName;
    private final String sinkTopicName;

    public KafkaConfig(String bootstrapServers, String groupId,
                       String sourceTopicName, String sinkTopicName) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.sourceTopicName = sourceTopicName;
        this.sinkTopicName = sinkTopicName;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getSourceTopicName() {
        return sourceTopicName;
    }

    public String getSinkTopicName() {
        return sinkTopicName;
    }

    /**
     * Creates Kafka consumer properties.
     */
    public Properties getConsumerProperties() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", bootstrapServers);
        props.setProperty("group.id", groupId);
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty("enable.auto.commit", "true");
        return props;
    }

    /**
     * Creates Kafka producer properties.
     */
    public Properties getProducerProperties() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", bootstrapServers);
        props.setProperty("acks", "all");
        props.setProperty("retries", "3");
        props.setProperty("compression.type", "snappy");
        return props;
    }

    public static KafkaConfig fromEnv() {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String groupId = System.getenv().getOrDefault("KAFKA_GROUP_ID", "beema-message-processor");
        String sourceTopicName = System.getenv().getOrDefault("KAFKA_SOURCE_TOPIC", "raw-messages");
        String sinkTopicName = System.getenv().getOrDefault("KAFKA_SINK_TOPIC", "processed-messages");

        return new KafkaConfig(bootstrapServers, groupId, sourceTopicName, sinkTopicName);
    }
}
