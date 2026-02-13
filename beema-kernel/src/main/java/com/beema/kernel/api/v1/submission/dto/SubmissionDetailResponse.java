package com.beema.kernel.api.v1.submission.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed response DTO for a submission.
 */
public record SubmissionDetailResponse(
    UUID submissionId,
    String product,
    String status,
    String tenantId,
    Map<String, Object> formData,
    Map<String, Object> ratingResult,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long version
) {
}
