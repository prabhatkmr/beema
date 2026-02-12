package com.beema.kernel.workflow.renewal;

import io.temporal.activity.ActivityInterface;

import java.time.LocalDateTime;

@ActivityInterface
public interface RenewalPolicyActivities {

    /**
     * Create a renewal version of the policy.
     *
     * @param policyNumber Policy to renew
     * @param renewalPremium New premium amount
     * @param newExpiryDate New expiry date
     */
    void createRenewalVersion(
            String policyNumber,
            Double renewalPremium,
            LocalDateTime newExpiryDate
    );
}
