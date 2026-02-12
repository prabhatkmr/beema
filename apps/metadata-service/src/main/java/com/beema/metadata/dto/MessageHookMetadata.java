package com.beema.metadata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * DTO for MessageHook metadata sent to Kafka control stream.
 * This format matches the MessageHookMetadata model in beema-message-processor.
 */
public class MessageHookMetadata {

    @JsonProperty("hookId")
    @NotBlank
    private String hookId;

    @JsonProperty("messageType")
    @NotBlank
    private String messageType;

    @JsonProperty("script")
    private String script;

    @JsonProperty("enabled")
    @NotNull
    private Boolean enabled;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("operation")
    @NotBlank
    private String operation; // INSERT, UPDATE, DELETE

    // Constructors
    public MessageHookMetadata() {
    }

    public MessageHookMetadata(String hookId, String messageType, String script, Boolean enabled, String operation) {
        this.hookId = hookId;
        this.messageType = messageType;
        this.script = script;
        this.enabled = enabled;
        this.operation = operation;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getHookId() {
        return hookId;
    }

    public void setHookId(String hookId) {
        this.hookId = hookId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "MessageHookMetadata{" +
                "hookId='" + hookId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", enabled=" + enabled +
                ", operation='" + operation + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
