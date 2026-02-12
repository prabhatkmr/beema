package com.beema.kernel.workflow.submission;

import java.util.Map;

public record QuoteResult(
        String submissionId,
        Double premium,
        Map<String, Double> premiumBreakdown,
        String ratingTier
) {
}
