package com.beema.kernel.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA AttributeConverter for PostgreSQL JSONB columns.
 *
 * Converts between Java Map<String, Object> and PostgreSQL JSONB.
 * This enables flex-schema storage where different agreement types
 * can have different attributes without schema migrations.
 *
 * Example Usage:
 * <pre>
 * @Entity
 * public class Agreement {
 *     @Convert(converter = JsonbConverter.class)
 *     @Column(name = "attributes", columnDefinition = "jsonb")
 *     private Map<String, Object> attributes;
 * }
 *
 * // Usage
 * Agreement agreement = new Agreement();
 * agreement.setAttributes(Map.of(
 *     "vehicle_vin", "1HGCM82633A123456",
 *     "vehicle_year", 2024,
 *     "coverage_type", "COMPREHENSIVE"
 * ));
 * </pre>
 *
 * PostgreSQL JSONB Benefits:
 * - Indexed queries: CREATE INDEX USING GIN (attributes)
 * - Containment operator: WHERE attributes @> '{"coverage_type": "COMPREHENSIVE"}'
 * - Path extraction: attributes->'vehicle_year'
 * - Binary storage (more efficient than JSON text)
 */
@Converter
@Component
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final Logger log = LoggerFactory.getLogger(JsonbConverter.class);
    private final ObjectMapper objectMapper;

    public JsonbConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert Java Map to JSON string for database storage.
     */
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert Map to JSON: {}", attribute, e);
            throw new IllegalArgumentException("Could not convert map to JSON string", e);
        }
    }

    /**
     * Convert JSON string from database to Java Map.
     */
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("{}")) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("Failed to convert JSON to Map: {}", dbData, e);
            throw new IllegalArgumentException("Could not convert JSON string to map", e);
        }
    }
}
