package com.beema.kernel.api.v1.workflow;

public record RenewalStatusResponse(
        String policyNumber,
        String status,
        Double renewalPremium,
        Double premiumIncreasePercent,
        String workflowId
) {
}
