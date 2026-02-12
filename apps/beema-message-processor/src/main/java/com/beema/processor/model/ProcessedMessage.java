package com.beema.processor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Processed message in Beema's internal format.
 * Written to Kafka 'processed-messages' topic.
 */
public class ProcessedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String messageId;
    private final String messageType;
    private final String sourceSystem;
    private final Map<String, Object> transformedData;
    private final Instant processedAt;
    private final String hookName;
    private final Long hookId;

    @JsonCreator
    public ProcessedMessage(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("messageType") String messageType,
            @JsonProperty("sourceSystem") String sourceSystem,
            @JsonProperty("transformedData") Map<String, Object> transformedData,
            @JsonProperty("processedAt") Instant processedAt,
            @JsonProperty("hookName") String hookName,
            @JsonProperty("hookId") Long hookId) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.sourceSystem = sourceSystem;
        this.transformedData = transformedData;
        this.processedAt = processedAt != null ? processedAt : Instant.now();
        this.hookName = hookName;
        this.hookId = hookId;
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

    public Map<String, Object> getTransformedData() {
        return transformedData;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getHookName() {
        return hookName;
    }

    public Long getHookId() {
        return hookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedMessage that = (ProcessedMessage) o;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(messageType, that.messageType) &&
                Objects.equals(sourceSystem, that.sourceSystem) &&
                Objects.equals(transformedData, that.transformedData) &&
                Objects.equals(processedAt, that.processedAt) &&
                Objects.equals(hookName, that.hookName) &&
                Objects.equals(hookId, that.hookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, messageType, sourceSystem, transformedData, processedAt, hookName, hookId);
    }

    @Override
    public String toString() {
        return "ProcessedMessage{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", hookName='" + hookName + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}
