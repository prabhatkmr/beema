package com.beema.kernel.domain.message;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

/**
 * Message Hook Entity
 *
 * Represents a message transformation hook with pre-processing, transformation,
 * and post-processing JEXL scripts. Supports multiple error handling strategies
 * and retry policies.
 */
@Entity
@Table(name = "sys_message_hooks")
public class MessageHook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hook_id")
    private Long hookId;

    @NotBlank(message = "Hook name is required")
    @Column(name = "hook_name", nullable = false, unique = true)
    private String hookName;

    @NotBlank(message = "Message type is required")
    @Column(name = "message_type", nullable = false, length = 100)
    private String messageType;

    @NotBlank(message = "Source system is required")
    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "target_system", length = 100)
    private String targetSystem;

    // Pre-processing: validation, normalization, enrichment
    @Column(name = "preprocessing_jexl", columnDefinition = "TEXT")
    private String preprocessingJexl;

    @Column(name = "preprocessing_order")
    private Integer preprocessingOrder = 0;

    // Main transformation: field mapping and conversion
    @NotBlank(message = "Transformation JEXL is required")
    @Column(name = "transformation_jexl", nullable = false, columnDefinition = "TEXT")
    private String transformationJexl;

    @Column(name = "transformation_order")
    private Integer transformationOrder = 100;

    // Post-processing: calculated fields, audit, notifications
    @Column(name = "postprocessing_jexl", columnDefinition = "TEXT")
    private String postprocessingJexl;

    @Column(name = "postprocessing_order")
    private Integer postprocessingOrder = 200;

    // Error handling
    @NotNull(message = "Error handling strategy is required")
    @Pattern(regexp = "fail_fast|log_continue|retry", message = "Invalid error handling strategy")
    @Column(name = "error_handling_strategy", nullable = false, length = 50)
    private String errorHandlingStrategy = "fail_fast";

    @Type(JsonBinaryType.class)
    @Column(name = "retry_config", columnDefinition = "jsonb")
    private Map<String, Object> retryConfig;

    // Metadata
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // Status
    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
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

    public String getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
    }

    public String getPreprocessingJexl() {
        return preprocessingJexl;
    }

    public void setPreprocessingJexl(String preprocessingJexl) {
        this.preprocessingJexl = preprocessingJexl;
    }

    public Integer getPreprocessingOrder() {
        return preprocessingOrder;
    }

    public void setPreprocessingOrder(Integer preprocessingOrder) {
        this.preprocessingOrder = preprocessingOrder;
    }

    public String getTransformationJexl() {
        return transformationJexl;
    }

    public void setTransformationJexl(String transformationJexl) {
        this.transformationJexl = transformationJexl;
    }

    public Integer getTransformationOrder() {
        return transformationOrder;
    }

    public void setTransformationOrder(Integer transformationOrder) {
        this.transformationOrder = transformationOrder;
    }

    public String getPostprocessingJexl() {
        return postprocessingJexl;
    }

    public void setPostprocessingJexl(String postprocessingJexl) {
        this.postprocessingJexl = postprocessingJexl;
    }

    public Integer getPostprocessingOrder() {
        return postprocessingOrder;
    }

    public void setPostprocessingOrder(Integer postprocessingOrder) {
        this.postprocessingOrder = postprocessingOrder;
    }

    public String getErrorHandlingStrategy() {
        return errorHandlingStrategy;
    }

    public void setErrorHandlingStrategy(String errorHandlingStrategy) {
        this.errorHandlingStrategy = errorHandlingStrategy;
    }

    public Map<String, Object> getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(Map<String, Object> retryConfig) {
        this.retryConfig = retryConfig;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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

    @Override
    public String toString() {
        return "MessageHook{" +
                "hookId=" + hookId +
                ", hookName='" + hookName + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
