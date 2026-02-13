package com.beema.kernel.api.v1.submission.dto;

/**
 * Response DTO for a quote submission.
 */
public record SubmissionResponse(
    String submissionId,
    String status
) {
}
