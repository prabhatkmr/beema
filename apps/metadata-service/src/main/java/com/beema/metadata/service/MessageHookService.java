package com.beema.metadata.service;

import com.beema.metadata.dto.MessageHookMetadata;
import com.beema.metadata.dto.MessageHookRequest;
import com.beema.metadata.dto.MessageHookResponse;
import com.beema.metadata.model.MessageHook;
import com.beema.metadata.repository.MessageHookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing message hooks.
 * Database changes automatically trigger Kafka events via PostgreSQL NOTIFY.
 */
@Service
public class MessageHookService {

    private static final Logger log = LoggerFactory.getLogger(MessageHookService.class);

    private final MessageHookRepository repository;
    private final MessageHookEventPublisher eventPublisher;

    public MessageHookService(MessageHookRepository repository, MessageHookEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get all message hooks.
     *
     * @return List of MessageHookResponse
     */
    @Transactional(readOnly = true)
    public List<MessageHookResponse> getAllHooks() {
        return repository.findAll().stream()
                .map(MessageHookResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get hooks by message type.
     *
     * @param messageType Message type
     * @return List of MessageHookResponse
     */
    @Transactional(readOnly = true)
    public List<MessageHookResponse> getHooksByMessageType(String messageType) {
        return repository.findByMessageType(messageType).stream()
                .map(MessageHookResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get enabled hooks by message type, ordered by priority.
     *
     * @param messageType Message type
     * @return List of MessageHookResponse
     */
    @Transactional(readOnly = true)
    public List<MessageHookResponse> getEnabledHooksByMessageType(String messageType) {
        return repository.findByMessageTypeAndEnabledOrderByPriorityAsc(messageType, true).stream()
                .map(MessageHookResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get hook by ID.
     *
     * @param id Hook ID
     * @return MessageHookResponse
     */
    @Transactional(readOnly = true)
    public MessageHookResponse getHookById(UUID id) {
        MessageHook hook = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MessageHook not found with id: " + id));
        return new MessageHookResponse(hook);
    }

    /**
     * Get hook by name.
     *
     * @param hookName Hook name
     * @return MessageHookResponse
     */
    @Transactional(readOnly = true)
    public MessageHookResponse getHookByName(String hookName) {
        MessageHook hook = repository.findByHookName(hookName)
                .orElseThrow(() -> new EntityNotFoundException("MessageHook not found with name: " + hookName));
        return new MessageHookResponse(hook);
    }

    /**
     * Create a new message hook.
     * PostgreSQL trigger will automatically send NOTIFY event.
     *
     * @param request MessageHookRequest
     * @param createdBy User who created the hook
     * @return MessageHookResponse
     */
    @Transactional
    public MessageHookResponse createHook(MessageHookRequest request, String createdBy) {
        log.info("Creating message hook: hookName={}, messageType={}", request.getHookName(), request.getMessageType());

        if (repository.existsByHookName(request.getHookName())) {
            throw new IllegalArgumentException("Hook with name '" + request.getHookName() + "' already exists");
        }

        MessageHook hook = new MessageHook();
        hook.setHookName(request.getHookName());
        hook.setMessageType(request.getMessageType());
        hook.setScript(request.getScript());
        hook.setEnabled(request.getEnabled());
        hook.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        hook.setDescription(request.getDescription());
        hook.setCreatedBy(createdBy);

        MessageHook saved = repository.save(hook);
        log.info("Created message hook: id={}, hookName={}", saved.getId(), saved.getHookName());

        return new MessageHookResponse(saved);
    }

    /**
     * Update an existing message hook.
     * PostgreSQL trigger will automatically send NOTIFY event.
     *
     * @param id Hook ID
     * @param request MessageHookRequest
     * @param updatedBy User who updated the hook
     * @return MessageHookResponse
     */
    @Transactional
    public MessageHookResponse updateHook(UUID id, MessageHookRequest request, String updatedBy) {
        log.info("Updating message hook: id={}, hookName={}", id, request.getHookName());

        MessageHook hook = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MessageHook not found with id: " + id));

        // Check for hook name conflict
        if (!hook.getHookName().equals(request.getHookName()) &&
                repository.existsByHookName(request.getHookName())) {
            throw new IllegalArgumentException("Hook with name '" + request.getHookName() + "' already exists");
        }

        hook.setHookName(request.getHookName());
        hook.setMessageType(request.getMessageType());
        hook.setScript(request.getScript());
        hook.setEnabled(request.getEnabled());
        hook.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        hook.setDescription(request.getDescription());
        hook.setUpdatedBy(updatedBy);

        MessageHook updated = repository.save(hook);
        log.info("Updated message hook: id={}, hookName={}", updated.getId(), updated.getHookName());

        return new MessageHookResponse(updated);
    }

    /**
     * Delete a message hook.
     * PostgreSQL trigger will automatically send NOTIFY event.
     *
     * @param id Hook ID
     */
    @Transactional
    public void deleteHook(UUID id) {
        log.info("Deleting message hook: id={}", id);

        MessageHook hook = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MessageHook not found with id: " + id));

        repository.delete(hook);
        log.info("Deleted message hook: id={}, hookName={}", id, hook.getHookName());
    }

    /**
     * Manually publish a hook to Kafka (for testing or re-syncing).
     *
     * @param id Hook ID
     */
    @Transactional(readOnly = true)
    public void republishHook(UUID id) {
        log.info("Manually republishing message hook: id={}", id);

        MessageHook hook = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MessageHook not found with id: " + id));

        MessageHookMetadata metadata = new MessageHookMetadata(
                hook.getId().toString(),
                hook.getMessageType(),
                hook.getScript(),
                hook.getEnabled(),
                "UPDATE"
        );

        eventPublisher.publishToKafka(metadata);
        log.info("Republished message hook: id={}, hookName={}", id, hook.getHookName());
    }

    /**
     * Republish all hooks to Kafka (for bootstrap or recovery).
     */
    @Transactional(readOnly = true)
    public void republishAllHooks() {
        log.info("Republishing all message hooks to Kafka");

        List<MessageHook> hooks = repository.findAll();
        for (MessageHook hook : hooks) {
            MessageHookMetadata metadata = new MessageHookMetadata(
                    hook.getId().toString(),
                    hook.getMessageType(),
                    hook.getScript(),
                    hook.getEnabled(),
                    "UPDATE"
            );
            eventPublisher.publishToKafka(metadata);
        }

        log.info("Republished {} message hooks to Kafka", hooks.size());
    }
}
