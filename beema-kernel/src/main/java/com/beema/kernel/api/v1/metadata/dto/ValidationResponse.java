package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.util.SchemaValidator;

import java.util.List;

/**
 * Response DTO for validation results.
 */
public record ValidationResponse(
    boolean isValid,
    List<String> errors,
    String errorMessage
) {
    /**
     * Create from validation result.
     */
    public static ValidationResponse from(SchemaValidator.ValidationResult result) {
        return new ValidationResponse(
            result.isValid(),
            result.errors(),
            result.isValid() ? null : result.getErrorMessage()
        );
    }
}
