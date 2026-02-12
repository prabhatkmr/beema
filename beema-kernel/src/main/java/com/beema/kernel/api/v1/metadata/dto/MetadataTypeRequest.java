package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.domain.metadata.MarketContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for creating/updating agreement types.
 */
public record MetadataTypeRequest(
    @NotBlank(message = "Type code is required")
    String typeCode,

    @NotNull(message = "Market context is required")
    MarketContext marketContext,

    @NotBlank(message = "Display name is required")
    String displayName,

    String description,

    @NotNull(message = "Attribute schema is required")
    Map<String, Object> attributeSchema,

    Map<String, Object> validationRules,

    Boolean isActive,

    String createdBy,

    String updatedBy
) {
    public MetadataTypeRequest {
        // Default values
        if (attributeSchema == null) {
            attributeSchema = new HashMap<>();
        }
        if (validationRules == null) {
            validationRules = new HashMap<>();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
