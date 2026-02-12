package com.beema.kernel.workflow.renewal;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RenewalNotificationActivities {

    /**
     * Send renewal email to insured.
     *
     * @param policyNumber Policy number
     * @param email Insured email address
     * @param renewalPremium New premium amount
     * @param message Custom message
     */
    void sendRenewalEmail(
            String policyNumber,
            String email,
            Double renewalPremium,
            String message
    );

    /**
     * Send notification to underwriting team for manual review.
     *
     * @param policyNumber Policy number
     * @param currentPremium Current premium
     * @param renewalPremium Calculated renewal premium
     * @param increasePercent Percentage increase
     */
    void sendUnderwriterNotification(
            String policyNumber,
            Double currentPremium,
            Double renewalPremium,
            Double increasePercent
    );

    /**
     * Send renewal declined email to insured.
     *
     * @param policyNumber Policy number
     * @param email Insured email address
     */
    void sendRenewalDeclinedEmail(
            String policyNumber,
            String email
    );
}
