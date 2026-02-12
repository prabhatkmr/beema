package com.beema.kernel.workflow.renewal;

import java.time.LocalDateTime;
import java.util.Map;

public record RenewalPolicyInfo(
        String policyNumber,
        Double currentPremium,
        LocalDateTime expiryDate,
        Map<String, Object> coverageDetails,
        Map<String, Object> claimsHistory,
        String insuredEmail
) {
}
