package com.beema.kernel.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Converter(autoApply = false)
public class JsonbListConverter implements AttributeConverter<List<Map<String, Object>>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize JSONB list attribute", e);
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSONB list column", e);
        }
    }
}
