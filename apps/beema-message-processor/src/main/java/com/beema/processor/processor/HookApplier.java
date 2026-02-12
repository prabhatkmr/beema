package com.beema.processor.processor;

import com.beema.processor.model.MessageHook;
import com.beema.processor.model.ProcessedMessage;
import com.beema.processor.model.RawMessage;
import com.beema.processor.repository.MessageHookRepository;
import com.beema.processor.service.JexlTransformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Applies JEXL transformation hooks to raw messages.
 * Serializable for Flink distribution.
 */
public class HookApplier implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HookApplier.class);

    private final MessageHookRepository repository;
    private final JexlTransformService jexlService;

    public HookApplier(MessageHookRepository repository, JexlTransformService jexlService) {
        this.repository = repository;
        this.jexlService = jexlService;
    }

    /**
     * Applies transformation hook to a raw message.
     *
     * @param rawMessage Raw message to transform
     * @return Optional ProcessedMessage if transformation succeeds
     */
    public Optional<ProcessedMessage> apply(RawMessage rawMessage) {
        try {
            // Find matching hook
            Optional<MessageHook> hookOpt = repository.findHookForMessage(
                    rawMessage.getMessageType(),
                    rawMessage.getSourceSystem()
            );

            if (hookOpt.isEmpty()) {
                log.warn("No hook found for messageType='{}', sourceSystem='{}'. Skipping message '{}'",
                        rawMessage.getMessageType(), rawMessage.getSourceSystem(), rawMessage.getMessageId());
                return Optional.empty();
            }

            MessageHook hook = hookOpt.get();
            log.debug("Applying hook '{}' to message '{}'", hook.getHookName(), rawMessage.getMessageId());

            // Transform message using JEXL
            Map<String, Object> transformedData = jexlService.transformMessage(
                    rawMessage.getPayload(),
                    hook.getFieldMapping()
            );

            // Create processed message
            ProcessedMessage processedMessage = new ProcessedMessage(
                    rawMessage.getMessageId(),
                    rawMessage.getMessageType(),
                    rawMessage.getSourceSystem(),
                    transformedData,
                    Instant.now(),
                    hook.getHookName(),
                    hook.getHookId()
            );

            log.info("Successfully transformed message '{}' using hook '{}'",
                    rawMessage.getMessageId(), hook.getHookName());

            return Optional.of(processedMessage);

        } catch (Exception e) {
            log.error("Failed to apply hook to message '{}': {}",
                    rawMessage.getMessageId(), e.getMessage(), e);
            return Optional.empty();
        }
    }
}
