package com.beema.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

/**
 * Metadata for a message transformation hook.
 * Broadcasted from the metadata-service when sys_message_hooks table is updated.
 */
public class MessageHookMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("hook_id")
    private String hookId;

    @JsonProperty("message_type")
    private String messageType;

    @JsonProperty("script")
    private String script;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("operation")
    private String operation; // "INSERT", "UPDATE", "DELETE"

    public MessageHookMetadata() {
    }

    public MessageHookMetadata(String hookId, String messageType, String script, boolean enabled, Instant updatedAt, String operation) {
        this.hookId = hookId;
        this.messageType = messageType;
        this.script = script;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
        this.operation = operation;
    }

    // Getters and setters
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
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
                ", operation='" + operation + '\'' +
                ", enabled=" + enabled +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
