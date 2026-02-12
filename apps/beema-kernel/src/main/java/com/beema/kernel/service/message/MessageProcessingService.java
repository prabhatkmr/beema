package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.*;
import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Message Processing Service
 *
 * Orchestrates the execution of message transformation pipeline:
 * 1. Pre-processing: validation, normalization, enrichment
 * 2. Transformation: field mapping and conversion
 * 3. Post-processing: calculated fields, audit, notifications
 *
 * Handles error strategies and retry logic.
 */
@Service
@Transactional
public class MessageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessingService.class);

    private final JexlExpressionEngine jexlEngine;
    private final MessageProcessingExecutionRepository executionRepository;
    private final MessageHookRepository hookRepository;

    // Configuration defaults
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_BACKOFF_MS = 1000;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_EXECUTION_TIME_MS = 5000;

    public MessageProcessingService(JexlExpressionEngine jexlEngine,
                                   MessageProcessingExecutionRepository executionRepository,
                                   MessageHookRepository hookRepository) {
        this.jexlEngine = jexlEngine;
        this.executionRepository = executionRepository;
        this.hookRepository = hookRepository;
    }

    /**
     * Execute full processing pipeline for a message
     */
    public MessageProcessingContext executeFullPipeline(MessageProcessingContext context, List<MessageHook> hooks) {
        log.info("Executing full processing pipeline for message type: {}, source: {}",
                context.getMessageType(), context.getSourceSystem());

        context.startProcessing();

        // Sort hooks by execution order
        List<MessageHook> sortedHooks = new ArrayList<>(hooks);
        sortedHooks.sort(Comparator.comparingInt(MessageHook::getPreprocessingOrder));

        for (MessageHook hook : sortedHooks) {
            try {
                // Pre-processing
                if (hook.getPreprocessingJexl() != null && !hook.getPreprocessingJexl().isBlank()) {
                    context = executePreProcessing(context, hook);
                    if (context.isHasErrors() && "fail_fast".equals(hook.getErrorHandlingStrategy())) {
                        break;
                    }
                }

                // Transformation
                context = executeTransformation(context, hook);
                if (context.isHasErrors() && "fail_fast".equals(hook.getErrorHandlingStrategy())) {
                    break;
                }

                // Post-processing
                if (hook.getPostprocessingJexl() != null && !hook.getPostprocessingJexl().isBlank()) {
                    context = executePostProcessing(context, hook);
                    if (context.isHasErrors() && "fail_fast".equals(hook.getErrorHandlingStrategy())) {
                        break;
                    }
                }

            } catch (Exception e) {
                log.error("Error executing pipeline for hook: {}", hook.getHookName(), e);
                context.recordError("Pipeline execution failed: " + e.getMessage(), e);

                if ("fail_fast".equals(hook.getErrorHandlingStrategy())) {
                    break;
                }
            }
        }

        context.completeProcessing();
        return context;
    }

    /**
     * Execute pre-processing stage
     */
    public MessageProcessingContext executePreProcessing(MessageProcessingContext context, MessageHook hook) {
        return executeStage(context, hook, "preprocessing", hook.getPreprocessingJexl());
    }

    /**
     * Execute transformation stage
     */
    public MessageProcessingContext executeTransformation(MessageProcessingContext context, MessageHook hook) {
        return executeStage(context, hook, "transformation", hook.getTransformationJexl());
    }

    /**
     * Execute post-processing stage
     */
    public MessageProcessingContext executePostProcessing(MessageProcessingContext context, MessageHook hook) {
        return executeStage(context, hook, "postprocessing", hook.getPostprocessingJexl());
    }

    /**
     * Execute a single processing stage with error handling and retry logic
     */
    private MessageProcessingContext executeStage(MessageProcessingContext context,
                                                 MessageHook hook,
                                                 String stage,
                                                 String jexlScript) {
        log.debug("Executing {} stage for hook: {}", stage, hook.getHookName());

        context.setCurrentStage(stage);
        Instant startTime = Instant.now();

        // Prepare retry configuration
        Map<String, Object> retryConfig = hook.getRetryConfig();
        int maxAttempts = getRetryMaxAttempts(retryConfig, hook.getErrorHandlingStrategy());
        long backoffMs = getRetryBackoffMs(retryConfig);
        double backoffMultiplier = getRetryBackoffMultiplier(retryConfig);

        context.setMaxAttempts(maxAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            context.setAttemptNumber(attempt);

            MessageProcessingExecution execution = createExecution(context, hook, stage, attempt, maxAttempts);

            try {
                // Execute JEXL script
                Object result = executeJexlScript(context, jexlScript, stage);

                // Update context based on stage
                updateContextWithResult(context, result, stage);

                // Record successful execution
                execution.setStatus("SUCCESS");
                execution.setOutputData(context.getResult());
                execution.setCompletedAt(Instant.now());
                execution.setExecutionTimeMs((int) (Instant.now().toEpochMilli() - startTime.toEpochMilli()));
                executionRepository.save(execution);

                // Store stage result
                context.getStageResults().put(stage, result);
                context.clearError();

                log.debug("Successfully executed {} stage for hook: {} (attempt {})",
                        stage, hook.getHookName(), attempt);
                break;

            } catch (Exception e) {
                log.warn("Error in {} stage for hook: {} (attempt {}/{}): {}",
                        stage, hook.getHookName(), attempt, maxAttempts, e.getMessage());

                // Record failed execution
                execution.setStatus(attempt < maxAttempts ? "RETRYING" : "FAILED");
                execution.setErrorMessage(e.getMessage());
                execution.setErrorStacktrace(getStackTrace(e));
                execution.setCompletedAt(Instant.now());
                execution.setExecutionTimeMs((int) (Instant.now().toEpochMilli() - startTime.toEpochMilli()));
                executionRepository.save(execution);

                context.recordError(e.getMessage(), e);

                // Handle error strategy
                if ("fail_fast".equals(hook.getErrorHandlingStrategy())) {
                    log.error("Failing fast due to error in {} stage", stage);
                    break;
                } else if ("log_continue".equals(hook.getErrorHandlingStrategy())) {
                    log.warn("Logging error and continuing due to log_continue strategy");
                    context.clearError();
                    break;
                } else if ("retry".equals(hook.getErrorHandlingStrategy()) && attempt < maxAttempts) {
                    // Calculate backoff
                    long currentBackoff = (long) (backoffMs * Math.pow(backoffMultiplier, attempt - 1));
                    log.info("Retrying after {} ms", currentBackoff);

                    try {
                        Thread.sleep(currentBackoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry backoff interrupted");
                        break;
                    }
                }
            }
        }

        return context;
    }

    /**
     * Execute JEXL script with proper context
     */
    private Object executeJexlScript(MessageProcessingContext context, String jexlScript, String stage) {
        // Prepare JEXL context
        Map<String, Object> jexlContext = new HashMap<>();
        jexlContext.put("message", context.getMessage());
        jexlContext.put("result", context.getResult());
        jexlContext.put("context", prepareContextForJexl(context));

        // Add stage-specific behavior
        if ("transformation".equals(stage)) {
            // For transformation, evaluate and return result
            return jexlEngine.evaluate(jexlContext, jexlScript);
        } else {
            // For pre/post processing, execute script (may modify message/result in place)
            return jexlEngine.evaluate(jexlContext, jexlScript);
        }
    }

    /**
     * Update context with stage result
     */
    private void updateContextWithResult(MessageProcessingContext context, Object result, String stage) {
        if ("transformation".equals(stage)) {
            // Transformation stage produces the result
            if (result instanceof Map) {
                context.setResult((Map<String, Object>) result);
            } else {
                throw new IllegalStateException("Transformation must return a Map, got: " +
                        (result != null ? result.getClass().getName() : "null"));
            }
        } else if ("preprocessing".equals(stage)) {
            // Pre-processing modifies the message in place
            // The message object is already modified by JEXL script
        } else if ("postprocessing".equals(stage)) {
            // Post-processing modifies the result in place
            // The result object is already modified by JEXL script
        }
    }

    /**
     * Prepare context for JEXL evaluation
     */
    private Map<String, Object> prepareContextForJexl(MessageProcessingContext context) {
        Map<String, Object> jexlContext = new HashMap<>();
        jexlContext.put("messageType", context.getMessageType());
        jexlContext.put("sourceSystem", context.getSourceSystem());
        jexlContext.put("targetSystem", context.getTargetSystem());
        jexlContext.put("receivedAt", context.getReceivedAt() != null ? context.getReceivedAt().toString() : null);
        jexlContext.put("executionTime", context.getExecutionTime());
        jexlContext.put("attemptNumber", context.getAttemptNumber());
        jexlContext.putAll(context.getMetadata());
        return jexlContext;
    }

    /**
     * Create execution record
     */
    private MessageProcessingExecution createExecution(MessageProcessingContext context,
                                                      MessageHook hook,
                                                      String stage,
                                                      int attemptNumber,
                                                      int maxAttempts) {
        MessageProcessingExecution execution = new MessageProcessingExecution();
        execution.setHookId(hook.getHookId());
        execution.setMessageType(context.getMessageType());
        execution.setSourceSystem(context.getSourceSystem());
        execution.setProcessingStage(stage);
        execution.setInputData(context.getMessage());
        execution.setAttemptNumber(attemptNumber);
        execution.setMaxAttempts(maxAttempts);
        execution.setStartedAt(Instant.now());
        return execution;
    }

    /**
     * Get retry max attempts from configuration
     */
    private int getRetryMaxAttempts(Map<String, Object> retryConfig, String errorHandlingStrategy) {
        if (!"retry".equals(errorHandlingStrategy)) {
            return 1;
        }
        if (retryConfig != null && retryConfig.containsKey("maxAttempts")) {
            return ((Number) retryConfig.get("maxAttempts")).intValue();
        }
        return DEFAULT_MAX_ATTEMPTS;
    }

    /**
     * Get retry backoff from configuration
     */
    private long getRetryBackoffMs(Map<String, Object> retryConfig) {
        if (retryConfig != null && retryConfig.containsKey("backoffMs")) {
            return ((Number) retryConfig.get("backoffMs")).longValue();
        }
        return DEFAULT_BACKOFF_MS;
    }

    /**
     * Get retry backoff multiplier from configuration
     */
    private double getRetryBackoffMultiplier(Map<String, Object> retryConfig) {
        if (retryConfig != null && retryConfig.containsKey("backoffMultiplier")) {
            return ((Number) retryConfig.get("backoffMultiplier")).doubleValue();
        }
        return DEFAULT_BACKOFF_MULTIPLIER;
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}
