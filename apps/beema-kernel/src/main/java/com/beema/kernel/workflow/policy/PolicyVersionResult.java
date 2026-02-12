package com.beema.kernel.workflow.policy;

public record PolicyVersionResult(
        String policyNumber,
        Integer version,
        String status
) {
}
