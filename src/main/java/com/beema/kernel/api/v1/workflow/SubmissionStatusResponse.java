package com.beema.kernel.api.v1.workflow;

public record SubmissionStatusResponse(
        String submissionId,
        String status,
        Double quotedPremium,
        String policyNumber,
        String workflowId
) {
}
