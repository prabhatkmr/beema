package com.beema.processor.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a message transformation hook from sys_message_hooks table.
 * Contains JEXL expressions for transforming external messages to Beema internal format.
 */
public class MessageHook implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long hookId;
    private String hookName;
    private String messageType;
    private String sourceSystem;
    private String jexlTransform;
    private JsonNode fieldMapping;
    private boolean enabled;
    private int priority;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    // Default constructor
    public MessageHook() {
    }

    // All-args constructor
    public MessageHook(Long hookId, String hookName, String messageType, String sourceSystem,
                       String jexlTransform, JsonNode fieldMapping, boolean enabled, int priority,
                       String description, Instant createdAt, Instant updatedAt,
                       String createdBy, String updatedBy) {
        this.hookId = hookId;
        this.hookName = hookName;
        this.messageType = messageType;
        this.sourceSystem = sourceSystem;
        this.jexlTransform = jexlTransform;
        this.fieldMapping = fieldMapping;
        this.enabled = enabled;
        this.priority = priority;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    // Getters and setters
    public Long getHookId() {
        return hookId;
    }

    public void setHookId(Long hookId) {
        this.hookId = hookId;
    }

    public String getHookName() {
        return hookName;
    }

    public void setHookName(String hookName) {
        this.hookName = hookName;
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

    public String getJexlTransform() {
        return jexlTransform;
    }

    public void setJexlTransform(String jexlTransform) {
        this.jexlTransform = jexlTransform;
    }

    public JsonNode getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(JsonNode fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageHook that = (MessageHook) o;
        return Objects.equals(hookId, that.hookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hookId);
    }

    @Override
    public String toString() {
        return "MessageHook{" +
                "hookId=" + hookId +
                ", hookName='" + hookName + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}
