package com.beema.kernel.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

/**
 * Workflow Hook Entity
 *
 * Represents a workflow hook configuration that defines when and how
 * a Temporal workflow should execute actions based on events.
 */
@Entity
@Table(name = "sys_workflow_hooks")
public class WorkflowHook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hook_id")
    private Long hookId;

    @Column(name = "hook_name", nullable = false, unique = true)
    private String hookName;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "trigger_condition", nullable = false, columnDefinition = "TEXT")
    private String triggerCondition;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Type(JsonBinaryType.class)
    @Column(name = "action_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> actionConfig;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(Map<String, Object> actionConfig) {
        this.actionConfig = actionConfig;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
}
