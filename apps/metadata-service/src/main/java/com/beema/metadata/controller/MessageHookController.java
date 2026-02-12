package com.beema.metadata.controller;

import com.beema.metadata.dto.MessageHookRequest;
import com.beema.metadata.dto.MessageHookResponse;
import com.beema.metadata.service.MessageHookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for managing message hooks.
 */
@RestController
@RequestMapping("/api/v1/message-hooks")
@Tag(name = "Message Hooks", description = "Dynamic JEXL-based message transformation hooks")
public class MessageHookController {

    private static final Logger log = LoggerFactory.getLogger(MessageHookController.class);

    private final MessageHookService messageHookService;

    public MessageHookController(MessageHookService messageHookService) {
        this.messageHookService = messageHookService;
    }

    @GetMapping
    @Operation(summary = "Get all message hooks", description = "Retrieve all message hooks")
    public ResponseEntity<List<MessageHookResponse>> getAllHooks() {
        log.debug("GET /api/v1/message-hooks - Getting all hooks");
        List<MessageHookResponse> hooks = messageHookService.getAllHooks();
        return ResponseEntity.ok(hooks);
    }

    @GetMapping("/message-type/{messageType}")
    @Operation(summary = "Get hooks by message type", description = "Retrieve hooks for a specific message type")
    public ResponseEntity<List<MessageHookResponse>> getHooksByMessageType(
            @PathVariable String messageType,
            @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
        log.debug("GET /api/v1/message-hooks/message-type/{} - enabledOnly={}", messageType, enabledOnly);

        List<MessageHookResponse> hooks = enabledOnly
                ? messageHookService.getEnabledHooksByMessageType(messageType)
                : messageHookService.getHooksByMessageType(messageType);

        return ResponseEntity.ok(hooks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hook by ID", description = "Retrieve a specific message hook by ID")
    public ResponseEntity<MessageHookResponse> getHookById(@PathVariable UUID id) {
        log.debug("GET /api/v1/message-hooks/{}", id);
        MessageHookResponse hook = messageHookService.getHookById(id);
        return ResponseEntity.ok(hook);
    }

    @GetMapping("/name/{hookName}")
    @Operation(summary = "Get hook by name", description = "Retrieve a specific message hook by name")
    public ResponseEntity<MessageHookResponse> getHookByName(@PathVariable String hookName) {
        log.debug("GET /api/v1/message-hooks/name/{}", hookName);
        MessageHookResponse hook = messageHookService.getHookByName(hookName);
        return ResponseEntity.ok(hook);
    }

    @PostMapping
    @Operation(summary = "Create message hook", description = "Create a new message hook")
    public ResponseEntity<MessageHookResponse> createHook(
            @Valid @RequestBody MessageHookRequest request,
            Authentication authentication) {
        log.info("POST /api/v1/message-hooks - Creating hook: {}", request.getHookName());

        String createdBy = getUsername(authentication);
        MessageHookResponse created = messageHookService.createHook(request, createdBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update message hook", description = "Update an existing message hook")
    public ResponseEntity<MessageHookResponse> updateHook(
            @PathVariable UUID id,
            @Valid @RequestBody MessageHookRequest request,
            Authentication authentication) {
        log.info("PUT /api/v1/message-hooks/{} - Updating hook: {}", id, request.getHookName());

        String updatedBy = getUsername(authentication);
        MessageHookResponse updated = messageHookService.updateHook(id, request, updatedBy);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete message hook", description = "Delete a message hook")
    public ResponseEntity<Void> deleteHook(@PathVariable UUID id) {
        log.info("DELETE /api/v1/message-hooks/{}", id);
        messageHookService.deleteHook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/republish")
    @Operation(summary = "Republish hook to Kafka", description = "Manually republish a hook to Kafka control stream")
    public ResponseEntity<Void> republishHook(@PathVariable UUID id) {
        log.info("POST /api/v1/message-hooks/{}/republish", id);
        messageHookService.republishHook(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/republish-all")
    @Operation(summary = "Republish all hooks", description = "Republish all hooks to Kafka (bootstrap/recovery)")
    public ResponseEntity<Void> republishAllHooks() {
        log.info("POST /api/v1/message-hooks/republish-all");
        messageHookService.republishAllHooks();
        return ResponseEntity.ok().build();
    }

    /**
     * Extract username from authentication context.
     * Falls back to "system" if no authentication is present.
     */
    private String getUsername(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
