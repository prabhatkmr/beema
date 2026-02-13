package com.beema.streaming.serde;

import com.beema.streaming.model.PolicyEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Flink DeserializationSchema for PolicyEvent JSON messages from Kafka.
 * Handles malformed messages gracefully by logging and returning null (filtered downstream).
 */
public class PolicyEventDeserializer implements DeserializationSchema<PolicyEvent> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PolicyEventDeserializer.class);

    private transient ObjectMapper objectMapper;

    @Override
    public void open(InitializationContext context) {
        this.objectMapper = createObjectMapper();
    }

    @Override
    public PolicyEvent deserialize(byte[] message) throws IOException {
        if (message == null || message.length == 0) {
            log.warn("Received null or empty message, skipping");
            return null;
        }
        try {
            if (objectMapper == null) {
                objectMapper = createObjectMapper();
            }
            return objectMapper.readValue(message, PolicyEvent.class);
        } catch (Exception e) {
            String raw = new String(message, StandardCharsets.UTF_8);
            log.error("Failed to deserialize PolicyEvent: {} | raw={}", e.getMessage(),
                    raw.length() > 200 ? raw.substring(0, 200) + "..." : raw);
            return null;
        }
    }

    @Override
    public boolean isEndOfStream(PolicyEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<PolicyEvent> getProducedType() {
        return TypeInformation.of(PolicyEvent.class);
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    }
}
