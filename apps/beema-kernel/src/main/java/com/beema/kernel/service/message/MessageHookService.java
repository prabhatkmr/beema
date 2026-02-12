package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.MessageHook;
import com.beema.kernel.domain.message.MessageHookDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for MessageHook operations
 */
public interface MessageHookService {

    /**
     * Find all message hooks
     */
    List<MessageHook> findAll();

    /**
     * Find hook by ID
     */
    Optional<MessageHook> findById(Long id);

    /**
     * Find hook by name
     */
    Optional<MessageHook> findByName(String hookName);

    /**
     * Find hooks by message type and source system
     */
    List<MessageHook> findHooksByMessageType(String messageType, String sourceSystem);

    /**
     * Create new message hook
     */
    MessageHook createHook(MessageHookDTO hookDTO);

    /**
     * Update existing message hook
     */
    MessageHook updateHook(Long id, MessageHookDTO hookDTO);

    /**
     * Delete message hook
     */
    void deleteHook(Long id);

    /**
     * Validate hook configuration (JEXL syntax, error handling, etc.)
     * Returns validation result with any errors or warnings
     */
    ValidationResult validateHook(MessageHook hook);

    /**
     * Test hook execution with sample data
     * Returns the transformed result and execution details
     */
    TestExecutionResult testHookExecution(Long hookId, Map<String, Object> sampleData);

    /**
     * Enable or disable a hook
     */
    MessageHook setHookEnabled(Long id, boolean enabled);

    /**
     * Validation result
     */
    class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    /**
     * Test execution result
     */
    class TestExecutionResult {
        private boolean success;
        private Map<String, Object> result;
        private String errorMessage;
        private Long executionTimeMs;
        private Map<String, Object> stageResults;

        public TestExecutionResult(boolean success, Map<String, Object> result, String errorMessage,
                                   Long executionTimeMs, Map<String, Object> stageResults) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.executionTimeMs = executionTimeMs;
            this.stageResults = stageResults;
        }

        public boolean isSuccess() {
            return success;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public Map<String, Object> getStageResults() {
            return stageResults;
        }
    }
}
