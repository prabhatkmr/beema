package com.beema.kernel.workflow.submission;

import io.temporal.activity.ActivityInterface;

import java.util.Map;

@ActivityInterface
public interface RatingEngineActivities {

    /**
     * Calculate premium for a submission.
     *
     * @param submissionId Unique submission identifier
     * @param productType Product type (MOTOR, HOME, COMMERCIAL, etc.)
     * @param coverageDetails Coverage specifications
     * @param riskFactors Risk assessment factors
     * @return Quote result with premium and breakdown
     */
    QuoteResult calculatePremium(
            String submissionId,
            String productType,
            Map<String, Object> coverageDetails,
            Map<String, Object> riskFactors
    );
}
