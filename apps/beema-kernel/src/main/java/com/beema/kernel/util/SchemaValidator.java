package com.beema.kernel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    public ValidationResult validate(Map<String, Object> attributes, Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return ValidationResult.valid();
        }

        try {
            JsonNode schemaNode = objectMapper.valueToTree(schema);
            JsonNode dataNode = objectMapper.valueToTree(attributes);

            JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);
            Set<ValidationMessage> errors = jsonSchema.validate(dataNode);

            if (errors.isEmpty()) {
                return ValidationResult.valid();
            }

            List<String> errorMessages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .toList();

            log.debug("Schema validation failed with {} errors: {}", errorMessages.size(), errorMessages);
            return ValidationResult.invalid(errorMessages);
        } catch (Exception e) {
            log.error("Schema validation error: {}", e.getMessage(), e);
            return ValidationResult.invalid(List.of("Schema validation error: " + e.getMessage()));
        }
    }

    public record ValidationResult(boolean isValid, List<String> errors) {

        public static ValidationResult valid() {
            return new ValidationResult(true, Collections.emptyList());
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }
}
