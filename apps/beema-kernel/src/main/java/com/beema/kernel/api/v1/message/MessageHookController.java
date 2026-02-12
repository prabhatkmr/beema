package com.beema.kernel.api.v1.message;

import com.beema.kernel.domain.message.MessageHook;
import com.beema.kernel.domain.message.MessageHookDTO;
import com.beema.kernel.domain.message.MessageProcessingContext;
import com.beema.kernel.service.message.MessageHookService;
import com.beema.kernel.service.message.MessageProcessingPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API Controller for Message Hooks
 *
 * Provides endpoints for managing message transformation hooks and executing
 * message processing pipelines.
 */
@RestController
@RequestMapping("/api/v1/message-hooks")
@Tag(name = "Message Hooks", description = "Message transformation hook management and processing")
public class MessageHookController {

    private static final Logger log = LoggerFactory.getLogger(MessageHookController.class);

    private final MessageHookService hookService;
    private final MessageProcessingPipeline processingPipeline;

    public MessageHookController(MessageHookService hookService,
                                MessageProcessingPipeline processingPipeline) {
        this.hookService = hookService;
        this.processingPipeline = processingPipeline;
    }

    /**
     * Get all message hooks
     */
    @GetMapping
    @Operation(summary = "List all message hooks", description = "Retrieve all message hooks, optionally filtered by message type and source system")
    public ResponseEntity<List<MessageHookDTO>> getAllHooks(
            @Parameter(description = "Filter by message type")
            @RequestParam(required = false) String messageType,
            @Parameter(description = "Filter by source system")
            @RequestParam(required = false) String sourceSystem) {

        log.info("GET /api/v1/message-hooks - messageType={}, sourceSystem={}", messageType, sourceSystem);

        List<MessageHook> hooks;

        if (messageType != null && sourceSystem != null) {
            hooks = hookService.findHooksByMessageType(messageType, sourceSystem);
        } else {
            hooks = hookService.findAll();
        }

        List<MessageHookDTO> dtos = hooks.stream()
                .map(MessageHookDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get hook by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get message hook by ID", description = "Retrieve a specific message hook by its ID")
    public ResponseEntity<MessageHookDTO> getHookById(@PathVariable Long id) {
        log.info("GET /api/v1/message-hooks/{}", id);

        return hookService.findById(id)
                .map(MessageHookDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new message hook
     */
    @PostMapping
    @Operation(summary = "Create message hook", description = "Create a new message transformation hook")
    public ResponseEntity<MessageHookDTO> createHook(@Valid @RequestBody MessageHookDTO hookDTO) {
        log.info("POST /api/v1/message-hooks - hookName={}", hookDTO.getHookName());

        try {
            MessageHook created = hookService.createHook(hookDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(MessageHookDTO.fromEntity(created));
        } catch (IllegalArgumentException e) {
            log.error("Failed to create hook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing message hook
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update message hook", description = "Update an existing message hook")
    public ResponseEntity<MessageHookDTO> updateHook(
            @PathVariable Long id,
            @Valid @RequestBody MessageHookDTO hookDTO) {
        log.info("PUT /api/v1/message-hooks/{} - hookName={}", id, hookDTO.getHookName());

        try {
            MessageHook updated = hookService.updateHook(id, hookDTO);
            return ResponseEntity.ok(MessageHookDTO.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            log.error("Failed to update hook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete message hook
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete message hook", description = "Delete a message hook by ID")
    public ResponseEntity<Void> deleteHook(@PathVariable Long id) {
        log.info("DELETE /api/v1/message-hooks/{}", id);

        try {
            hookService.deleteHook(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete hook: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validate hook JEXL scripts
     */
    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate message hook", description = "Validate JEXL scripts and configuration of a message hook")
    public ResponseEntity<Map<String, Object>> validateHook(@PathVariable Long id) {
        log.info("POST /api/v1/message-hooks/{}/validate", id);

        return hookService.findById(id)
                .map(hook -> {
                    MessageHookService.ValidationResult result = hookService.validateHook(hook);

                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", result.isValid());
                    response.put("errors", result.getErrors());
                    response.put("warnings", result.getWarnings());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Test hook with sample data
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "Test message hook", description = "Test a message hook with sample data")
    public ResponseEntity<Map<String, Object>> testHook(
            @PathVariable Long id,
            @RequestBody Map<String, Object> sampleData) {
        log.info("POST /api/v1/message-hooks/{}/test", id);

        try {
            MessageHookService.TestExecutionResult result = hookService.testHookExecution(id, sampleData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("result", result.getResult());
            response.put("errorMessage", result.getErrorMessage());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            response.put("stageResults", result.getStageResults());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to test hook: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Enable or disable hook
     */
    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Enable/disable hook", description = "Enable or disable a message hook")
    public ResponseEntity<MessageHookDTO> setHookEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> enabledRequest) {
        log.info("PATCH /api/v1/message-hooks/{}/enabled", id);

        try {
            Boolean enabled = enabledRequest.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().build();
            }

            MessageHook updated = hookService.setHookEnabled(id, enabled);
            return ResponseEntity.ok(MessageHookDTO.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            log.error("Failed to set hook enabled status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Execute full processing pipeline
     */
    @PostMapping("/process")
    @Operation(summary = "Process message", description = "Execute full message processing pipeline")
    public ResponseEntity<Map<String, Object>> processMessage(
            @RequestBody ProcessMessageRequest request) {
        log.info("POST /api/v1/message-hooks/process - messageType={}, sourceSystem={}",
                request.getMessageType(), request.getSourceSystem());

        try {
            MessageProcessingContext result = processingPipeline.process(
                    request.getMessageType(),
                    request.getSourceSystem(),
                    request.getMessage()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", !result.isHasErrors());
            response.put("result", result.getResult());
            response.put("errorMessage", result.getErrorMessage());
            response.put("executionTime", result.getExecutionTime());
            response.put("stageResults", result.getStageResults());
            response.put("attemptNumber", result.getAttemptNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to process message: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorMessage", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Request DTO for processing messages
     */
    public static class ProcessMessageRequest {
        private String messageType;
        private String sourceSystem;
        private Map<String, Object> message;

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

        public Map<String, Object> getMessage() {
            return message;
        }

        public void setMessage(Map<String, Object> message) {
            this.message = message;
        }
    }
}
