package com.beema.kernel.workflow.renewal;

import io.temporal.activity.ActivityInterface;

import java.util.Map;

@ActivityInterface
public interface RenewalRatingActivities {

    /**
     * Calculate renewal premium based on current policy and claims history.
     *
     * @param policyNumber Policy to renew
     * @param currentPremium Current premium amount
     * @param coverageDetails Current coverage specifications
     * @param claimsHistory Claims made during the term
     * @return Renewal quote result
     */
    RenewalQuoteResult calculateRenewalPremium(
            String policyNumber,
            Double currentPremium,
            Map<String, Object> coverageDetails,
            Map<String, Object> claimsHistory
    );
}
