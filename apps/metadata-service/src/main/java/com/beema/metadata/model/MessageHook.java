package com.beema.metadata.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Message Hook entity for dynamic JEXL-based message transformation.
 *
 * Represents a transformation script that processes messages of a specific type.
 * Changes to this entity trigger Kafka events for the Flink broadcast stream.
 */
@Entity
@Table(name = "sys_message_hooks")
public class MessageHook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hook_name", nullable = false, unique = true)
    @NotBlank(message = "Hook name is required")
    private String hookName;

    @Column(name = "message_type", nullable = false, length = 100)
    @NotBlank(message = "Message type is required")
    private String messageType;

    @Column(name = "script", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "JEXL script is required")
    private String script;

    @Column(name = "enabled", nullable = false)
    @NotNull
    private Boolean enabled = true;

    @Column(name = "priority", nullable = false)
    @NotNull
    private Integer priority = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false)
    @NotBlank(message = "Created by is required")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // Constructors
    public MessageHook() {
    }

    public MessageHook(String hookName, String messageType, String script, Boolean enabled) {
        this.hookName = hookName;
        this.messageType = messageType;
        this.script = script;
        this.enabled = enabled;
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

    @Override
    public String toString() {
        return "MessageHook{" +
                "id=" + id +
                ", hookName='" + hookName + '\'' +
                ", messageType='" + messageType + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}
