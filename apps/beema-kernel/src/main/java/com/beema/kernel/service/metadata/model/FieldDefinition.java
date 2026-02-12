package com.beema.kernel.service.metadata.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FieldDefinition(
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

    public boolean isCalculated() {
        return "CALCULATED".equals(fieldType);
    }

    public boolean isDerived() {
        return "DERIVED".equals(fieldType);
    }

    public boolean isStandard() {
        return "STANDARD".equals(fieldType);
    }

    public boolean hasDependencies() {
        return dependsOn != null && !dependsOn.isEmpty();
    }
}
