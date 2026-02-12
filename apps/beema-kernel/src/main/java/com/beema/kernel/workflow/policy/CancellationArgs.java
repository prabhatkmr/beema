package com.beema.kernel.workflow.policy;

import java.time.LocalDateTime;

public record CancellationArgs(
        LocalDateTime effectiveDate,
        String reason,
        Double currentPremium
) {
}
