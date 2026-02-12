package com.beema.kernel.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Converter
public class StringArrayConverter implements AttributeConverter<List<String>, String[]> {

    @Override
    public String[] convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.toArray(new String[0]);
    }

    @Override
    public List<String> convertToEntityAttribute(String[] dbData) {
        if (dbData == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(dbData);
    }
}
