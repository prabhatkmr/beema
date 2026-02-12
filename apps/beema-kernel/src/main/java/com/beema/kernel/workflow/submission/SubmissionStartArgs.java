package com.beema.kernel.workflow.submission;

import java.time.LocalDateTime;
import java.util.Map;

public record SubmissionStartArgs(
        String submissionId,
        String productType,
        Map<String, Object> coverageDetails,
        Map<String, Object> riskFactors,
        LocalDateTime effectiveDate,
        LocalDateTime expiryDate
) {
}
