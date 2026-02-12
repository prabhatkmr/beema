package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.MessageHook;
import com.beema.kernel.domain.message.MessageHookRepository;
import com.beema.kernel.domain.message.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Message Processing Pipeline
 *
 * Builder pattern for constructing and executing message processing pipelines.
 * Supports chainable operations:
 * pipeline.preProcess().transform().postProcess().execute()
 */
@Component
public class MessageProcessingPipeline {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessingPipeline.class);

    private final MessageProcessingService processingService;
    private final MessageHookRepository hookRepository;

    // Pipeline configuration
    private MessageProcessingContext context;
    private List<MessageHook> hooks;
    private boolean executePreProcessing;
    private boolean executeTransformation;
    private boolean executePostProcessing;

    public MessageProcessingPipeline(MessageProcessingService processingService,
                                    MessageHookRepository hookRepository) {
        this.processingService = processingService;
        this.hookRepository = hookRepository;
    }

    /**
     * Create a new pipeline builder
     */
    public Builder builder() {
        return new Builder(this);
    }

    /**
     * Builder for MessageProcessingPipeline
     */
    public static class Builder {
        private final MessageProcessingPipeline pipeline;
        private MessageProcessingContext context;
        private List<MessageHook> hooks;
        private boolean preProcess = true;
        private boolean transform = true;
        private boolean postProcess = true;

        private Builder(MessageProcessingPipeline pipeline) {
            this.pipeline = pipeline;
            this.hooks = new ArrayList<>();
        }

        /**
         * Set the message to process
         */
        public Builder message(Map<String, Object> message) {
            this.context = new MessageProcessingContext(message);
            return this;
        }

        /**
         * Set the processing context
         */
        public Builder context(MessageProcessingContext context) {
            this.context = context;
            return this;
        }

        /**
         * Set message type
         */
        public Builder messageType(String messageType) {
            if (this.context == null) {
                this.context = new MessageProcessingContext();
            }
            this.context.setMessageType(messageType);
            return this;
        }

        /**
         * Set source system
         */
        public Builder sourceSystem(String sourceSystem) {
            if (this.context == null) {
                this.context = new MessageProcessingContext();
            }
            this.context.setSourceSystem(sourceSystem);
            return this;
        }

        /**
         * Set target system
         */
        public Builder targetSystem(String targetSystem) {
            if (this.context == null) {
                this.context = new MessageProcessingContext();
            }
            this.context.setTargetSystem(targetSystem);
            return this;
        }

        /**
         * Load hooks automatically based on message type and source system
         */
        public Builder autoLoadHooks() {
            if (this.context == null || this.context.getMessageType() == null || this.context.getSourceSystem() == null) {
                throw new IllegalStateException("messageType and sourceSystem must be set before auto-loading hooks");
            }

            this.hooks = pipeline.hookRepository.findByMessageTypeAndSourceSystemAndEnabledTrue(
                    this.context.getMessageType(),
                    this.context.getSourceSystem()
            );

            log.info("Auto-loaded {} hooks for messageType={}, sourceSystem={}",
                    this.hooks.size(), this.context.getMessageType(), this.context.getSourceSystem());

            return this;
        }

        /**
         * Add a specific hook
         */
        public Builder hook(MessageHook hook) {
            this.hooks.add(hook);
            return this;
        }

        /**
         * Add multiple hooks
         */
        public Builder hooks(List<MessageHook> hooks) {
            this.hooks.addAll(hooks);
            return this;
        }

        /**
         * Enable/disable pre-processing stage
         */
        public Builder preProcess(boolean enabled) {
            this.preProcess = enabled;
            return this;
        }

        /**
         * Enable/disable transformation stage
         */
        public Builder transform(boolean enabled) {
            this.transform = enabled;
            return this;
        }

        /**
         * Enable/disable post-processing stage
         */
        public Builder postProcess(boolean enabled) {
            this.postProcess = enabled;
            return this;
        }

        /**
         * Skip pre-processing stage
         */
        public Builder skipPreProcess() {
            this.preProcess = false;
            return this;
        }

        /**
         * Skip transformation stage
         */
        public Builder skipTransform() {
            this.transform = false;
            return this;
        }

        /**
         * Skip post-processing stage
         */
        public Builder skipPostProcess() {
            this.postProcess = false;
            return this;
        }

        /**
         * Execute the pipeline
         */
        public MessageProcessingContext execute() {
            if (context == null) {
                throw new IllegalStateException("Context must be set before execution");
            }

            if (hooks.isEmpty()) {
                log.warn("No hooks configured for pipeline execution");
                return context;
            }

            log.info("Executing pipeline with {} hooks (preProcess={}, transform={}, postProcess={})",
                    hooks.size(), preProcess, transform, postProcess);

            // Execute stages based on configuration
            for (MessageHook hook : hooks) {
                try {
                    if (preProcess && hook.getPreprocessingJexl() != null && !hook.getPreprocessingJexl().isBlank()) {
                        context = pipeline.processingService.executePreProcessing(context, hook);
                        if (context.isHasErrors() && "fail_fast".equals(hook.getErrorHandlingStrategy())) {
                            return context;
                        }
                    }

                    if (transform && hook.getTransformationJexl() != null && !hook.getTransformationJexl().isBlank()) {
                        context = pipeline.processingService.executeTransformation(context, hook);
                        if (context.isHasErrors() && "fail_fast".equals(hook.getErrorHandlingStrategy())) {
                            return context;
                        }
                    }

                    if (postProcess && hook.getPostprocessingJexl() != null && !hook.getPostprocessingJexl().isBlank()) {
                        context = pipeline.processingService.executePostProcessing(context, hook);
                        if (context.isHasErrors() && "fail_fast".equals(hook.getErrorHandlingStrategy())) {
                            return context;
                        }
                    }

                } catch (Exception e) {
                    log.error("Pipeline execution error for hook: {}", hook.getHookName(), e);
                    context.recordError("Pipeline execution failed: " + e.getMessage(), e);

                    if ("fail_fast".equals(hook.getErrorHandlingStrategy())) {
                        return context;
                    }
                }
            }

            context.completeProcessing();
            return context;
        }
    }

    /**
     * Quick execution with automatic hook loading
     */
    public MessageProcessingContext process(String messageType, String sourceSystem, Map<String, Object> message) {
        return builder()
                .message(message)
                .messageType(messageType)
                .sourceSystem(sourceSystem)
                .autoLoadHooks()
                .execute();
    }

    /**
     * Execute with custom hooks
     */
    public MessageProcessingContext process(MessageProcessingContext context, List<MessageHook> hooks) {
        return builder()
                .context(context)
                .hooks(hooks)
                .execute();
    }
}
