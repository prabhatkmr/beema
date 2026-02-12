package com.beema.kernel.workflow.policy;

import java.time.LocalDateTime;
import java.util.Map;

public record PolicyStartArgs(
        String policyNumber,
        LocalDateTime inceptionDate,
        LocalDateTime expiryDate,
        Double premium,
        Map<String, Object> coverageDetails
) {
}
