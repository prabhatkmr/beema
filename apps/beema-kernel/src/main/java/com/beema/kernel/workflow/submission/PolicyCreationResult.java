package com.beema.kernel.workflow.submission;

public record PolicyCreationResult(
        String policyNumber,
        String status,
        String workflowId
) {
}
