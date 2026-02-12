package com.beema.kernel.domain.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.Map;

/**
 * Message Hook Data Transfer Object
 *
 * Used for API requests and responses. Provides validation and simplified
 * structure for client communication.
 */
public class MessageHookDTO {

    private Long hookId;

    @NotBlank(message = "Hook name is required")
    private String hookName;

    @NotBlank(message = "Message type is required")
    private String messageType;

    @NotBlank(message = "Source system is required")
    private String sourceSystem;

    private String targetSystem;

    private String preprocessingJexl;
    private Integer preprocessingOrder;

    @NotBlank(message = "Transformation JEXL is required")
    private String transformationJexl;
    private Integer transformationOrder;

    private String postprocessingJexl;
    private Integer postprocessingOrder;

    @NotNull(message = "Error handling strategy is required")
    @Pattern(regexp = "fail_fast|log_continue|retry", message = "Invalid error handling strategy")
    private String errorHandlingStrategy;

    private Map<String, Object> retryConfig;
    private Map<String, Object> metadata;

    @NotNull
    private Boolean enabled;

    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    // Factory method to create DTO from Entity
    public static MessageHookDTO fromEntity(MessageHook entity) {
        MessageHookDTO dto = new MessageHookDTO();
        dto.setHookId(entity.getHookId());
        dto.setHookName(entity.getHookName());
        dto.setMessageType(entity.getMessageType());
        dto.setSourceSystem(entity.getSourceSystem());
        dto.setTargetSystem(entity.getTargetSystem());
        dto.setPreprocessingJexl(entity.getPreprocessingJexl());
        dto.setPreprocessingOrder(entity.getPreprocessingOrder());
        dto.setTransformationJexl(entity.getTransformationJexl());
        dto.setTransformationOrder(entity.getTransformationOrder());
        dto.setPostprocessingJexl(entity.getPostprocessingJexl());
        dto.setPostprocessingOrder(entity.getPostprocessingOrder());
        dto.setErrorHandlingStrategy(entity.getErrorHandlingStrategy());
        dto.setRetryConfig(entity.getRetryConfig());
        dto.setMetadata(entity.getMetadata());
        dto.setEnabled(entity.getEnabled());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }

    // Method to convert DTO to Entity
    public MessageHook toEntity() {
        MessageHook entity = new MessageHook();
        entity.setHookId(this.hookId);
        entity.setHookName(this.hookName);
        entity.setMessageType(this.messageType);
        entity.setSourceSystem(this.sourceSystem);
        entity.setTargetSystem(this.targetSystem);
        entity.setPreprocessingJexl(this.preprocessingJexl);
        entity.setPreprocessingOrder(this.preprocessingOrder != null ? this.preprocessingOrder : 0);
        entity.setTransformationJexl(this.transformationJexl);
        entity.setTransformationOrder(this.transformationOrder != null ? this.transformationOrder : 100);
        entity.setPostprocessingJexl(this.postprocessingJexl);
        entity.setPostprocessingOrder(this.postprocessingOrder != null ? this.postprocessingOrder : 200);
        entity.setErrorHandlingStrategy(this.errorHandlingStrategy != null ? this.errorHandlingStrategy : "fail_fast");
        entity.setRetryConfig(this.retryConfig);
        entity.setMetadata(this.metadata);
        entity.setEnabled(this.enabled != null ? this.enabled : true);
        entity.setDescription(this.description);
        entity.setCreatedBy(this.createdBy);
        entity.setUpdatedBy(this.updatedBy);
        return entity;
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
}
