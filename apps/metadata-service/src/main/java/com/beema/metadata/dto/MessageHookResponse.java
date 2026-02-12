package com.beema.metadata.dto;

import com.beema.metadata.model.MessageHook;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for MessageHook.
 */
public class MessageHookResponse {

    private UUID id;
    private String hookName;
    private String messageType;
    private String script;
    private Boolean enabled;
    private Integer priority;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;
    private Long version;

    // Constructor from entity
    public MessageHookResponse(MessageHook hook) {
        this.id = hook.getId();
        this.hookName = hook.getHookName();
        this.messageType = hook.getMessageType();
        this.script = hook.getScript();
        this.enabled = hook.getEnabled();
        this.priority = hook.getPriority();
        this.description = hook.getDescription();
        this.createdBy = hook.getCreatedBy();
        this.createdAt = hook.getCreatedAt();
        this.updatedBy = hook.getUpdatedBy();
        this.updatedAt = hook.getUpdatedAt();
        this.version = hook.getVersion();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
