package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.domain.metadata.MarketContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for validating attributes against a schema.
 */
public record ValidationRequest(
    @NotBlank(message = "Type code is required")
    String typeCode,

    @NotNull(message = "Market context is required")
    MarketContext marketContext,

    @NotNull(message = "Attributes are required")
    Map<String, Object> attributes
) {
}
