package com.beema.kernel.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize JSONB attribute", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(dbData, MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSONB column", e);
        }
    }
}
