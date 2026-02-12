package com.beema.processor.processor;

import com.beema.processor.model.MessageHookMetadata;
import com.beema.processor.model.RawMessage;
import com.beema.processor.model.TransformedMessage;
import com.beema.processor.service.JexlTransformService;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Flink BroadcastProcessFunction that transforms RawMessage to TransformedMessage
 * using dynamically updated JEXL hooks from a broadcast state.
 *
 * Architecture:
 * - Main Stream: RawMessage from Kafka (raw-messages topic)
 * - Broadcast Stream: MessageHookMetadata from Kafka (message-hooks-control topic)
 * - Broadcast State: Map<MessageType, JEXL Script>
 *
 * When a hook is updated in sys_message_hooks table:
 * 1. metadata-service emits MessageHookMetadata to control stream
 * 2. processBroadcastElement() updates the local broadcast state
 * 3. processElement() uses the updated hook for transformations
 */
public class JexlMessageTransformer extends BroadcastProcessFunction<RawMessage, MessageHookMetadata, TransformedMessage> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JexlMessageTransformer.class);

    // MapState descriptor for storing JEXL hooks (Key: MessageType, Value: JEXL Script)
    public static final MapStateDescriptor<String, String> HOOK_DESCRIPTOR =
            new MapStateDescriptor<>("MessageHooks", String.class, String.class);

    private final JexlTransformService jexlService;

    public JexlMessageTransformer(JexlTransformService jexlService) {
        this.jexlService = jexlService;
    }

    @Override
    public void processElement(RawMessage message, ReadOnlyContext ctx, Collector<TransformedMessage> out) throws Exception {
        log.debug("Processing message: {} (type: {})", message.getMessageId(), message.getMessageType());

        // 1. Retrieve the JEXL script for this specific message type (e.g., 'CDR', 'LIMCLM')
        ReadOnlyBroadcastState<String, String> hooks = ctx.getBroadcastState(HOOK_DESCRIPTOR);
        String jexlScript = hooks.get(message.getMessageType());

        if (jexlScript != null && !jexlScript.isBlank()) {
            try {
                // 2. Execute the Sandboxed JEXL Engine
                // message.getPayload() contains the raw message fields as Map<String, Object>
                Map<String, Object> resultData = jexlService.transform(message.getPayload(), jexlScript);

                // 3. Emit the transformed message to the 'beema-events' Kafka topic
                TransformedMessage transformed = new TransformedMessage(
                        message.getMessageId(),
                        message.getMessageType(),
                        message.getSourceSystem(),
                        resultData,
                        message.getMessageType() + "-hook"
                );
                out.collect(transformed);

                log.debug("Successfully transformed message {} using hook for type {}",
                        message.getMessageId(), message.getMessageType());

            } catch (Exception e) {
                log.error("Failed to transform message {} with JEXL script: {}",
                        message.getMessageId(), e.getMessage(), e);

                // Emit passthrough or send to DLQ
                emitPassthrough(message, out, "TRANSFORMATION_ERROR");
            }
        } else {
            // Passthrough: no hook found for this message type
            log.warn("No JEXL hook found for message type '{}'. Passing through raw payload.",
                    message.getMessageType());
            emitPassthrough(message, out, "NO_HOOK_FOUND");
        }
    }

    @Override
    public void processBroadcastElement(MessageHookMetadata hook, Context ctx, Collector<TransformedMessage> out) throws Exception {
        log.info("Received hook update: {} for message type '{}' (operation: {})",
                hook.getHookId(), hook.getMessageType(), hook.getOperation());

        BroadcastState<String, String> state = ctx.getBroadcastState(HOOK_DESCRIPTOR);

        // Update the local state when a user changes a rule in Beema Studio
        if ("DELETE".equalsIgnoreCase(hook.getOperation()) || !hook.isEnabled()) {
            // Remove hook from state
            state.remove(hook.getMessageType());
            log.info("Removed hook for message type '{}'", hook.getMessageType());
        } else {
            // Insert or Update hook
            state.put(hook.getMessageType(), hook.getScript());
            log.info("Updated hook for message type '{}' with script length: {}",
                    hook.getMessageType(), hook.getScript().length());
        }
    }

    /**
     * Emits a passthrough message when no hook is found or transformation fails.
     */
    private void emitPassthrough(RawMessage message, Collector<TransformedMessage> out, String reason) {
        TransformedMessage passthrough = new TransformedMessage(
                message.getMessageId(),
                message.getMessageType(),
                message.getSourceSystem(),
                message.getPayload(), // Pass raw payload
                reason
        );
        out.collect(passthrough);
        log.debug("Emitted passthrough for message {} (reason: {})", message.getMessageId(), reason);
    }
}
