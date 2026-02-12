package com.beema.kernel.workflow.submission;

import io.temporal.activity.ActivityInterface;

import java.time.LocalDateTime;
import java.util.Map;

@ActivityInterface
public interface PolicyCreationActivities {

    /**
     * Create a policy from a bound submission.
     *
     * @param submissionId Source submission
     * @param premium Final premium amount
     * @param coverageDetails Coverage specifications
     * @param effectiveDate Policy effective date
     * @param expiryDate Policy expiry date
     * @return Policy creation result
     */
    PolicyCreationResult createPolicy(
            String submissionId,
            Double premium,
            Map<String, Object> coverageDetails,
            LocalDateTime effectiveDate,
            LocalDateTime expiryDate
    );
}
