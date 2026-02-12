package com.beema.kernel.api.v1.agreement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for JSONB attribute searches.
 */
public record AttributeSearchRequest(
    @NotBlank(message = "Tenant ID is required")
    String tenantId,

    @NotNull(message = "Attributes are required")
    Map<String, Object> attributes
) {
}
