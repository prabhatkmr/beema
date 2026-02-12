package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.MessageHook;
import com.beema.kernel.domain.message.MessageHookDTO;
import com.beema.kernel.domain.message.MessageHookRepository;
import com.beema.kernel.domain.message.MessageProcessingContext;
import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Implementation of MessageHookService
 *
 * Provides CRUD operations and validation for message hooks.
 * Uses JexlExpressionEngine for script validation and execution.
 */
@Service
@Transactional
public class MessageHookServiceImpl implements MessageHookService {

    private static final Logger log = LoggerFactory.getLogger(MessageHookServiceImpl.class);

    private final MessageHookRepository hookRepository;
    private final JexlExpressionEngine jexlEngine;
    private final MessageProcessingService processingService;

    public MessageHookServiceImpl(MessageHookRepository hookRepository,
                                  JexlExpressionEngine jexlEngine,
                                  MessageProcessingService processingService) {
        this.hookRepository = hookRepository;
        this.jexlEngine = jexlEngine;
        this.processingService = processingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageHook> findAll() {
        return hookRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageHook> findById(Long id) {
        return hookRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageHook> findByName(String hookName) {
        return hookRepository.findByHookName(hookName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageHook> findHooksByMessageType(String messageType, String sourceSystem) {
        if (messageType == null || sourceSystem == null) {
            throw new IllegalArgumentException("messageType and sourceSystem are required");
        }
        return hookRepository.findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem);
    }

    @Override
    public MessageHook createHook(MessageHookDTO hookDTO) {
        log.info("Creating new message hook: {}", hookDTO.getHookName());

        // Check if hook name already exists
        if (hookRepository.existsByHookName(hookDTO.getHookName())) {
            throw new IllegalArgumentException("Hook with name '" + hookDTO.getHookName() + "' already exists");
        }

        // Convert DTO to entity
        MessageHook hook = hookDTO.toEntity();

        // Validate hook configuration
        ValidationResult validation = validateHook(hook);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Hook validation failed: " + String.join(", ", validation.getErrors()));
        }

        // Save hook
        MessageHook saved = hookRepository.save(hook);
        log.info("Created message hook: {} with ID: {}", saved.getHookName(), saved.getHookId());

        return saved;
    }

    @Override
    public MessageHook updateHook(Long id, MessageHookDTO hookDTO) {
        log.info("Updating message hook ID: {}", id);

        MessageHook existing = hookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hook not found with ID: " + id));

        // Check if hook name is being changed and if new name already exists
        if (!existing.getHookName().equals(hookDTO.getHookName())) {
            if (hookRepository.existsByHookName(hookDTO.getHookName())) {
                throw new IllegalArgumentException("Hook with name '" + hookDTO.getHookName() + "' already exists");
            }
        }

        // Update fields
        existing.setHookName(hookDTO.getHookName());
        existing.setMessageType(hookDTO.getMessageType());
        existing.setSourceSystem(hookDTO.getSourceSystem());
        existing.setTargetSystem(hookDTO.getTargetSystem());
        existing.setPreprocessingJexl(hookDTO.getPreprocessingJexl());
        existing.setPreprocessingOrder(hookDTO.getPreprocessingOrder());
        existing.setTransformationJexl(hookDTO.getTransformationJexl());
        existing.setTransformationOrder(hookDTO.getTransformationOrder());
        existing.setPostprocessingJexl(hookDTO.getPostprocessingJexl());
        existing.setPostprocessingOrder(hookDTO.getPostprocessingOrder());
        existing.setErrorHandlingStrategy(hookDTO.getErrorHandlingStrategy());
        existing.setRetryConfig(hookDTO.getRetryConfig());
        existing.setMetadata(hookDTO.getMetadata());
        existing.setEnabled(hookDTO.getEnabled());
        existing.setDescription(hookDTO.getDescription());
        existing.setUpdatedBy(hookDTO.getUpdatedBy());

        // Validate updated hook
        ValidationResult validation = validateHook(existing);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Hook validation failed: " + String.join(", ", validation.getErrors()));
        }

        MessageHook saved = hookRepository.save(existing);
        log.info("Updated message hook: {}", saved.getHookName());

        return saved;
    }

    @Override
    public void deleteHook(Long id) {
        log.info("Deleting message hook ID: {}", id);

        if (!hookRepository.existsById(id)) {
            throw new IllegalArgumentException("Hook not found with ID: " + id);
        }

        hookRepository.deleteById(id);
        log.info("Deleted message hook ID: {}", id);
    }

    @Override
    public ValidationResult validateHook(MessageHook hook) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate required fields
        if (hook.getHookName() == null || hook.getHookName().isBlank()) {
            errors.add("Hook name is required");
        }
        if (hook.getMessageType() == null || hook.getMessageType().isBlank()) {
            errors.add("Message type is required");
        }
        if (hook.getSourceSystem() == null || hook.getSourceSystem().isBlank()) {
            errors.add("Source system is required");
        }
        if (hook.getTransformationJexl() == null || hook.getTransformationJexl().isBlank()) {
            errors.add("Transformation JEXL is required");
        }

        // Validate error handling strategy
        if (hook.getErrorHandlingStrategy() == null ||
            (!hook.getErrorHandlingStrategy().equals("fail_fast") &&
             !hook.getErrorHandlingStrategy().equals("log_continue") &&
             !hook.getErrorHandlingStrategy().equals("retry"))) {
            errors.add("Invalid error handling strategy. Must be: fail_fast, log_continue, or retry");
        }

        // Validate retry configuration if strategy is 'retry'
        if ("retry".equals(hook.getErrorHandlingStrategy())) {
            if (hook.getRetryConfig() == null) {
                warnings.add("Retry strategy specified but no retry configuration provided. Using defaults.");
            } else {
                if (!hook.getRetryConfig().containsKey("maxAttempts")) {
                    warnings.add("Retry configuration missing 'maxAttempts'. Using default: 3");
                }
            }
        }

        // Validate JEXL syntax for preprocessing
        if (hook.getPreprocessingJexl() != null && !hook.getPreprocessingJexl().isBlank()) {
            if (!jexlEngine.isValidSyntax(hook.getPreprocessingJexl())) {
                errors.add("Invalid JEXL syntax in preprocessing script");
            }
        }

        // Validate JEXL syntax for transformation
        if (hook.getTransformationJexl() != null && !hook.getTransformationJexl().isBlank()) {
            if (!jexlEngine.isValidSyntax(hook.getTransformationJexl())) {
                errors.add("Invalid JEXL syntax in transformation script");
            }
        }

        // Validate JEXL syntax for postprocessing
        if (hook.getPostprocessingJexl() != null && !hook.getPostprocessingJexl().isBlank()) {
            if (!jexlEngine.isValidSyntax(hook.getPostprocessingJexl())) {
                errors.add("Invalid JEXL syntax in postprocessing script");
            }
        }

        boolean isValid = errors.isEmpty();
        log.debug("Hook validation for '{}': valid={}, errors={}, warnings={}",
                hook.getHookName(), isValid, errors.size(), warnings.size());

        return new ValidationResult(isValid, errors, warnings);
    }

    @Override
    public TestExecutionResult testHookExecution(Long hookId, Map<String, Object> sampleData) {
        log.info("Testing hook execution for ID: {}", hookId);

        MessageHook hook = hookRepository.findById(hookId)
                .orElseThrow(() -> new IllegalArgumentException("Hook not found with ID: " + hookId));

        Instant startTime = Instant.now();
        Map<String, Object> stageResults = new HashMap<>();

        try {
            // Create processing context
            MessageProcessingContext context = new MessageProcessingContext(sampleData)
                    .messageType(hook.getMessageType())
                    .sourceSystem(hook.getSourceSystem())
                    .targetSystem(hook.getTargetSystem())
                    .hookId(hook.getHookId())
                    .hookName(hook.getHookName());

            // Execute full pipeline
            context = processingService.executeFullPipeline(context, List.of(hook));

            long executionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();

            if (context.isHasErrors()) {
                return new TestExecutionResult(
                        false,
                        context.getResult(),
                        context.getErrorMessage(),
                        executionTime,
                        context.getStageResults()
                );
            }

            return new TestExecutionResult(
                    true,
                    context.getResult(),
                    null,
                    executionTime,
                    context.getStageResults()
            );

        } catch (Exception e) {
            long executionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            log.error("Test execution failed for hook ID: {}", hookId, e);

            return new TestExecutionResult(
                    false,
                    null,
                    e.getMessage(),
                    executionTime,
                    stageResults
            );
        }
    }

    @Override
    public MessageHook setHookEnabled(Long id, boolean enabled) {
        log.info("Setting hook ID {} enabled status to: {}", id, enabled);

        MessageHook hook = hookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hook not found with ID: " + id));

        hook.setEnabled(enabled);
        return hookRepository.save(hook);
    }
}
