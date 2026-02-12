package com.beema.kernel.api.v1.workflow;

public record UnderwriterReviewRequest(
        boolean approved,
        Double adjustedPremium,
        String notes
) {
}
