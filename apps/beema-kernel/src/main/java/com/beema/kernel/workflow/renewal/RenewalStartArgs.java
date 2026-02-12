package com.beema.kernel.workflow.renewal;

import java.time.LocalDateTime;
import java.util.Map;

public record RenewalStartArgs(
        String policyNumber,
        Double currentPremium,
        LocalDateTime currentExpiryDate,
        LocalDateTime newExpiryDate,
        Map<String, Object> coverageDetails,
        Map<String, Object> claimsHistory,
        String insuredEmail
) {
}
