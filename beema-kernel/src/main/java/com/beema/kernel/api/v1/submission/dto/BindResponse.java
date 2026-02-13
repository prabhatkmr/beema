package com.beema.kernel.api.v1.submission.dto;

/**
 * Response DTO for a bind operation.
 */
public record BindResponse(
    String submissionId,
    String status,
    String message
) {
}
