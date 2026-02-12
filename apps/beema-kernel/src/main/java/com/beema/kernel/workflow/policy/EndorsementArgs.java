package com.beema.kernel.workflow.policy;

import java.time.LocalDateTime;
import java.util.Map;

public record EndorsementArgs(
        LocalDateTime effectiveDate,
        Double oldPremium,
        Double newPremium,
        Map<String, Object> changes
) {
}
