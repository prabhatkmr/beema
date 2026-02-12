package com.beema.kernel.workflow.renewal;

public record RenewalQuoteResult(
        String policyNumber,
        Double renewalPremium,
        Double baseAdjustment,
        Double claimsAdjustment,
        String ratingTier
) {
}
