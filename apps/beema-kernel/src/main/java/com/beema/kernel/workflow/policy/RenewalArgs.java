package com.beema.kernel.workflow.policy;

import java.time.LocalDateTime;
import java.util.Map;

public record RenewalArgs(
        LocalDateTime newInceptionDate,
        LocalDateTime newExpiryDate,
        Double newPremium,
        Map<String, Object> coverageDetails
) {
}
