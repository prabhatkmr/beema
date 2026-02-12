package com.beema.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Transformed message ready for emission to beema-events Kafka topic.
 */
public class TransformedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("message_type")
    private String messageType;

    @JsonProperty("source_system")
    private String sourceSystem;

    @JsonProperty("result_data")
    private Map<String, Object> resultData;

    @JsonProperty("processed_at")
    private Instant processedAt;

    @JsonProperty("hook_id")
    private String hookId;

    public TransformedMessage() {
    }

    public TransformedMessage(String messageId, Map<String, Object> resultData) {
        this.messageId = messageId;
        this.resultData = resultData;
        this.processedAt = Instant.now();
    }

    public TransformedMessage(String messageId, String messageType, String sourceSystem,
                             Map<String, Object> resultData, String hookId) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.sourceSystem = sourceSystem;
        this.resultData = resultData;
        this.processedAt = Instant.now();
        this.hookId = hookId;
    }

    // Getters and setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getHookId() {
        return hookId;
    }

    public void setHookId(String hookId) {
        this.hookId = hookId;
    }

    @Override
    public String toString() {
        return "TransformedMessage{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", processedAt=" + processedAt +
                ", hookId='" + hookId + '\'' +
                '}';
    }
}
