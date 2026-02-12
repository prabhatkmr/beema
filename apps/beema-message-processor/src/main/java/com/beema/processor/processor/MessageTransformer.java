package com.beema.processor.processor;

import com.beema.processor.model.ProcessedMessage;
import com.beema.processor.model.RawMessage;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Flink MapFunction that transforms RawMessage to ProcessedMessage.
 * Uses HookApplier to apply JEXL transformations.
 */
public class MessageTransformer extends RichMapFunction<RawMessage, ProcessedMessage> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MessageTransformer.class);

    private final HookApplier hookApplier;

    public MessageTransformer(HookApplier hookApplier) {
        this.hookApplier = hookApplier;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.info("MessageTransformer initialized");
    }

    @Override
    public ProcessedMessage map(RawMessage rawMessage) throws Exception {
        log.debug("Processing message: {}", rawMessage.getMessageId());

        Optional<ProcessedMessage> result = hookApplier.apply(rawMessage);

        if (result.isEmpty()) {
            log.warn("Failed to transform message '{}'. Returning empty ProcessedMessage.",
                    rawMessage.getMessageId());
            // Return a ProcessedMessage with empty data to indicate failure
            // In production, you might want to send to a DLQ (Dead Letter Queue)
            return new ProcessedMessage(
                    rawMessage.getMessageId(),
                    rawMessage.getMessageType(),
                    rawMessage.getSourceSystem(),
                    null,
                    java.time.Instant.now(),
                    "NO_HOOK_FOUND",
                    null
            );
        }

        return result.get();
    }

    @Override
    public void close() throws Exception {
        super.close();
        log.info("MessageTransformer closed");
    }
}
