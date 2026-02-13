package com.beema.kernel.api.v1.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * Request DTO for submitting a quote for workflow processing.
 */
public record SubmissionRequest(
    @NotBlank(message = "Product is required")
    String product,

    @NotEmpty(message = "Data must not be empty")
    Map<String, Object> data
) {
}
