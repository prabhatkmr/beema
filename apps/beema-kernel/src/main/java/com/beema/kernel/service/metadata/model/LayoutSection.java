package com.beema.kernel.service.metadata.model;

import java.util.List;

public record LayoutSection(
        String sectionName,
        int order,
        List<FieldDefinition> fields
) {
}
