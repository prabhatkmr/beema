package com.beema.kernel.domain.message;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Message Processing Context
 *
 * Holds message data, metadata, and execution context throughout the
 * processing pipeline. Used to pass data between pre-processing,
 * transformation, and post-processing stages.
 */
public class MessageProcessingContext {

    private String messageType;
    private String sourceSystem;
    private String targetSystem;

    // Message data
    private Map<String, Object> message;
    private Map<String, Object> result;

    // Execution metadata
    private Long hookId;
    private String hookName;
    private Instant receivedAt;
    private Instant processingStartedAt;
    private Long executionTime;

    // Processing stage tracking
    private String currentStage;
    private Map<String, Object> stageResults;

    // Error tracking
    private boolean hasErrors;
    private String errorMessage;
    private Exception lastException;

    // Retry tracking
    private int attemptNumber;
    private int maxAttempts;

    // Additional context
    private Map<String, Object> metadata;

    public MessageProcessingContext() {
        this.message = new HashMap<>();
        this.result = new HashMap<>();
        this.stageResults = new HashMap<>();
        this.metadata = new HashMap<>();
        this.receivedAt = Instant.now();
        this.attemptNumber = 1;
        this.maxAttempts = 1;
    }

    public MessageProcessingContext(Map<String, Object> message) {
        this();
        this.message = message != null ? new HashMap<>(message) : new HashMap<>();
    }

    // Builder pattern methods
    public MessageProcessingContext messageType(String messageType) {
        this.messageType = messageType;
        return this;
    }

    public MessageProcessingContext sourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
        return this;
    }

    public MessageProcessingContext targetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
        return this;
    }

    public MessageProcessingContext hookId(Long hookId) {
        this.hookId = hookId;
        return this;
    }

    public MessageProcessingContext hookName(String hookName) {
        this.hookName = hookName;
        return this;
    }

    public MessageProcessingContext maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public MessageProcessingContext addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public void startProcessing() {
        this.processingStartedAt = Instant.now();
    }

    public void completeProcessing() {
        if (processingStartedAt != null) {
            this.executionTime = Instant.now().toEpochMilli() - processingStartedAt.toEpochMilli();
        }
    }

    public void recordError(String errorMessage, Exception exception) {
        this.hasErrors = true;
        this.errorMessage = errorMessage;
        this.lastException = exception;
    }

    public void clearError() {
        this.hasErrors = false;
        this.errorMessage = null;
        this.lastException = null;
    }

    public void incrementAttempt() {
        this.attemptNumber++;
    }

    public boolean canRetry() {
        return attemptNumber < maxAttempts;
    }

    // Getters and Setters

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

    public Map<String, Object> getMessage() {
        return message;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

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

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(Instant processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public Map<String, Object> getStageResults() {
        return stageResults;
    }

    public void setStageResults(Map<String, Object> stageResults) {
        this.stageResults = stageResults;
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Exception getLastException() {
        return lastException;
    }

    public void setLastException(Exception lastException) {
        this.lastException = lastException;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "MessageProcessingContext{" +
                "messageType='" + messageType + '\'' +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", currentStage='" + currentStage + '\'' +
                ", hasErrors=" + hasErrors +
                ", attemptNumber=" + attemptNumber +
                '}';
    }
}
