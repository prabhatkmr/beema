package com.beema.kernel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * JSON Schema validator for agreement attributes.
 *
 * Validates JSONB data against JSON Schema definitions stored in metadata.
 *
 * Supports:
 * - Type validation (string, integer, number, boolean, object, array)
 * - Required fields
 * - Pattern matching (regex)
 * - Range validation (minimum, maximum)
 * - Enum validation
 * - Nested objects
 *
 * Note: This is a basic implementation. For production, consider using
 * a full JSON Schema library like everit-org/json-schema or networknt/json-schema-validator.
 */
@Component
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    private final ObjectMapper objectMapper;

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validate data against a JSON Schema.
     *
     * @param data Data to validate
     * @param schema JSON Schema
     * @return Validation result with errors if any
     */
    public ValidationResult validate(Map<String, Object> data, Map<String, Object> schema) {
        List<String> errors = new ArrayList<>();

        try {
            // Convert to JsonNode for easier traversal
            JsonNode dataNode = objectMapper.valueToTree(data);
            JsonNode schemaNode = objectMapper.valueToTree(schema);

            validateNode(dataNode, schemaNode, "", errors);

        } catch (Exception e) {
            log.error("Schema validation failed with exception", e);
            errors.add("Schema validation error: " + e.getMessage());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Recursive validation of a node against schema.
     */
    private void validateNode(JsonNode data, JsonNode schema, String path, List<String> errors) {
        // Check type
        String expectedType = schema.has("type") ? schema.get("type").asText() : null;
        if (expectedType != null && !validateType(data, expectedType, path, errors)) {
            return; // Type mismatch, skip further validation
        }

        // Check required fields (for objects)
        if ("object".equals(expectedType) && schema.has("required")) {
            JsonNode required = schema.get("required");
            if (required.isArray()) {
                required.forEach(fieldNode -> {
                    String fieldName = fieldNode.asText();
                    if (!data.has(fieldName)) {
                        errors.add(String.format("Required field '%s%s' is missing", path, fieldName));
                    }
                });
            }
        }

        // Validate properties (for objects)
        if ("object".equals(expectedType) && schema.has("properties") && data.isObject()) {
            JsonNode properties = schema.get("properties");
            properties.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldSchema = entry.getValue();

                if (data.has(fieldName)) {
                    validateNode(
                        data.get(fieldName),
                        fieldSchema,
                        path + fieldName + ".",
                        errors
                    );
                }
            });
        }

        // Validate string patterns
        if ("string".equals(expectedType) && data.isTextual() && schema.has("pattern")) {
            String pattern = schema.get("pattern").asText();
            String value = data.asText();
            if (!Pattern.matches(pattern, value)) {
                errors.add(String.format("Field '%s' value '%s' does not match pattern '%s'",
                    path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                    value, pattern));
            }
        }

        // Validate string min/max length
        if ("string".equals(expectedType) && data.isTextual()) {
            String value = data.asText();
            if (schema.has("minLength")) {
                int minLength = schema.get("minLength").asInt();
                if (value.length() < minLength) {
                    errors.add(String.format("Field '%s' length %d is less than minimum %d",
                        path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                        value.length(), minLength));
                }
            }
            if (schema.has("maxLength")) {
                int maxLength = schema.get("maxLength").asInt();
                if (value.length() > maxLength) {
                    errors.add(String.format("Field '%s' length %d exceeds maximum %d",
                        path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                        value.length(), maxLength));
                }
            }
        }

        // Validate number ranges
        if (("integer".equals(expectedType) || "number".equals(expectedType)) && data.isNumber()) {
            double value = data.asDouble();
            if (schema.has("minimum")) {
                double minimum = schema.get("minimum").asDouble();
                if (value < minimum) {
                    errors.add(String.format("Field '%s' value %s is less than minimum %s",
                        path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                        value, minimum));
                }
            }
            if (schema.has("maximum")) {
                double maximum = schema.get("maximum").asDouble();
                if (value > maximum) {
                    errors.add(String.format("Field '%s' value %s exceeds maximum %s",
                        path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                        value, maximum));
                }
            }
        }

        // Validate enums
        if (schema.has("enum")) {
            JsonNode enumNode = schema.get("enum");
            boolean found = false;
            for (JsonNode allowedValue : enumNode) {
                if (allowedValue.equals(data)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errors.add(String.format("Field '%s' value '%s' is not in allowed values %s",
                    path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                    data.toString(), enumNode.toString()));
            }
        }
    }

    /**
     * Validate that data matches expected type.
     */
    private boolean validateType(JsonNode data, String expectedType, String path, List<String> errors) {
        boolean valid = switch (expectedType) {
            case "string" -> data.isTextual();
            case "integer" -> data.isIntegralNumber();
            case "number" -> data.isNumber();
            case "boolean" -> data.isBoolean();
            case "object" -> data.isObject();
            case "array" -> data.isArray();
            default -> true; // Unknown type, skip validation
        };

        if (!valid) {
            errors.add(String.format("Field '%s' expected type '%s' but got '%s'",
                path.isEmpty() ? "root" : path.substring(0, path.length() - 1),
                expectedType,
                data.getNodeType().toString().toLowerCase()));
        }

        return valid;
    }

    /**
     * Validation result.
     */
    public record ValidationResult(boolean isValid, List<String> errors) {
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
