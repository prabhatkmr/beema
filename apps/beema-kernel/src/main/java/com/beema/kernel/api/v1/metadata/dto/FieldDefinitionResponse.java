package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.service.metadata.model.FieldDefinition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FieldDefinitionResponse(
        UUID id,
        String attributeName,
        String displayName,
        String description,
        String dataType,
        String fieldType,
        String validationPattern,
        BigDecimal minValue,
        BigDecimal maxValue,
        Map<String, Object> allowedValues,
        Map<String, Object> defaultValue,
        boolean isRequired,
        boolean isSearchable,
        String uiComponent,
        int uiOrder,
        String category,
        String sectionName,
        String calculationScript,
        List<String> dependsOn
) {

    public static FieldDefinitionResponse from(FieldDefinition field) {
        return new FieldDefinitionResponse(
                field.id(),
                field.attributeName(),
                field.displayName(),
                field.description(),
                field.dataType(),
                field.fieldType(),
                field.validationPattern(),
                field.minValue(),
                field.maxValue(),
                field.allowedValues(),
                field.defaultValue(),
                field.isRequired(),
                field.isSearchable(),
                field.uiComponent(),
                field.uiOrder(),
                field.category(),
                field.sectionName(),
                field.calculationScript(),
                field.dependsOn()
        );
    }
}
