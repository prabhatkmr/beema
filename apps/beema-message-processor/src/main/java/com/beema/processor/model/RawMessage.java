package com.beema.processor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Raw message received from Kafka 'raw-messages' topic.
 * Contains the external message payload and metadata.
 */
public class RawMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String messageId;
    private final String messageType;
    private final String sourceSystem;
    private final JsonNode payload;
    private final Instant timestamp;

    @JsonCreator
    public RawMessage(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("messageType") String messageType,
            @JsonProperty("sourceSystem") String sourceSystem,
            @JsonProperty("payload") JsonNode payload,
            @JsonProperty("timestamp") Instant timestamp) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.sourceSystem = sourceSystem;
        this.payload = payload;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawMessage that = (RawMessage) o;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(messageType, that.messageType) &&
                Objects.equals(sourceSystem, that.sourceSystem) &&
                Objects.equals(payload, that.payload) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, messageType, sourceSystem, payload, timestamp);
    }

    @Override
    public String toString() {
        return "RawMessage{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
