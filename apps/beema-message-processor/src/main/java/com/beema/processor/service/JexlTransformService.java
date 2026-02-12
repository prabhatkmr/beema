package com.beema.processor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JEXL-based transformation service for message processing.
 * Reuses the sandboxed JEXL engine pattern from beema-kernel.
 *
 * Thread-safe and serializable for Flink distribution.
 */
public class JexlTransformService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JexlTransformService.class);

    private transient JexlEngine jexlEngine;
    private final ObjectMapper objectMapper;

    public JexlTransformService() {
        this.objectMapper = new ObjectMapper();
        initializeJexlEngine();
    }

    private void initializeJexlEngine() {
        if (jexlEngine == null) {
            this.jexlEngine = new JexlBuilder()
                    .strict(false)        // Allows null propagation
                    .silent(true)         // Silent mode: undefined variables return null
                    .safe(false)          // Safe=false allows proper null handling
                    .permissions(JexlPermissions.RESTRICTED)
                    .create();
            log.info("JexlTransformService initialized with sandboxed permissions");
        }
    }

    /**
     * Transforms message payload using JEXL field mappings.
     *
     * @param payload Raw message payload (JsonNode)
     * @param fieldMapping JSONB field mapping with JEXL expressions
     * @return Transformed data as Map
     */
    public Map<String, Object> transformMessage(JsonNode payload, JsonNode fieldMapping) {
        initializeJexlEngine(); // Ensure engine is initialized after deserialization

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> context = jsonNodeToMap(payload);

        // Iterate through field mapping and apply JEXL transformations
        Iterator<Map.Entry<String, JsonNode>> fields = fieldMapping.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String targetField = entry.getKey();
            JsonNode mappingConfig = entry.getValue();

            try {
                if (mappingConfig.has("jexl")) {
                    String jexlExpression = mappingConfig.get("jexl").asText();
                    Object transformedValue = evaluateJexl(context, jexlExpression);
                    result.put(targetField, transformedValue);
                } else if (mappingConfig.has("source")) {
                    // Simple field mapping without JEXL
                    String sourcePath = mappingConfig.get("source").asText();
                    Object value = getValueByPath(payload, sourcePath);
                    result.put(targetField, value);
                }
            } catch (Exception e) {
                log.error("Failed to transform field '{}': {}", targetField, e.getMessage());
                result.put(targetField, null);
            }
        }

        return result;
    }

    /**
     * Transforms message payload using a single JEXL script.
     * Used by JexlMessageTransformer for broadcast state pattern.
     *
     * @param payload Raw message payload as Map
     * @param jexlScript JEXL script to transform the entire message
     * @return Transformed data as Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> transform(Map<String, Object> payload, String jexlScript) {
        initializeJexlEngine(); // Ensure engine is initialized

        if (jexlScript == null || jexlScript.isBlank()) {
            return payload; // Return original if no script
        }

        try {
            // Create JEXL context with direct access to fields
            MapContext jexlContext = new MapContext();
            // Make all payload fields available directly (e.g., premium, sumInsured)
            payload.forEach(jexlContext::set);
            // Also make full payload available as "message" object
            jexlContext.set("message", payload);
            jexlContext.set("Math", Math.class);

            JexlScript script = jexlEngine.createScript(jexlScript);
            Object result = script.execute(jexlContext);

            // If result is a Map, return it directly
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }

            // If result is a single value, wrap it
            Map<String, Object> wrappedResult = new HashMap<>();
            wrappedResult.put("result", result);
            return wrappedResult;

        } catch (JexlException e) {
            log.error("JEXL transformation error for script: {}", e.getMessage());
            throw new TransformationException(
                    String.format("JEXL transformation failed: %s", e.getMessage()), e);
        }
    }

    /**
     * Evaluates a JEXL expression against the message context.
     *
     * @param context Message data context
     * @param expression JEXL expression
     * @return Evaluated result
     */
    public Object evaluateJexl(Map<String, Object> context, String expression) {
        initializeJexlEngine(); // Ensure engine is initialized

        if (expression == null || expression.isBlank()) {
            return null;
        }

        try {
            // Create JEXL context with "message" variable
            MapContext jexlContext = new MapContext();
            jexlContext.set("message", context);
            jexlContext.set("Math", Math.class);

            JexlExpression jexlExpression = jexlEngine.createExpression(expression);
            Object result = jexlExpression.evaluate(jexlContext);

            log.debug("Evaluated JEXL '{}' => {}", expression, result);
            return result;

        } catch (JexlException e) {
            log.error("JEXL evaluation error for expression '{}': {}", expression, e.getMessage());
            throw new TransformationException(
                    String.format("JEXL evaluation failed for '%s': %s", expression, e.getMessage()), e);
        }
    }

    /**
     * Validates that the JEXL expression is syntactically correct.
     *
     * @param expression JEXL expression
     * @return true if valid, false otherwise
     */
    public boolean isValidJexlSyntax(String expression) {
        initializeJexlEngine(); // Ensure engine is initialized

        if (expression == null || expression.isBlank()) {
            return false;
        }

        try {
            jexlEngine.createExpression(expression);
            return true;
        } catch (JexlException.Parsing e) {
            log.debug("Invalid JEXL syntax: '{}' - {}", expression, e.getMessage());
            return false;
        }
    }

    /**
     * Converts JsonNode to Map for JEXL context.
     */
    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        try {
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            log.error("Failed to convert JsonNode to Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets value from JsonNode by path (e.g., "policy.premium").
     */
    private Object getValueByPath(JsonNode node, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return null;
            }
            current = current.get(part);
        }

        return current != null ? nodeToObject(current) : null;
    }

    /**
     * Converts JsonNode to appropriate Java type.
     */
    private Object nodeToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt() || node.isLong()) {
            return node.asLong();
        } else if (node.isDouble() || node.isFloat()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray() || node.isObject()) {
            return jsonNodeToMap(node);
        }
        return node.asText();
    }

    /**
     * Exception thrown when transformation fails.
     */
    public static class TransformationException extends RuntimeException {
        public TransformationException(String message) {
            super(message);
        }

        public TransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
