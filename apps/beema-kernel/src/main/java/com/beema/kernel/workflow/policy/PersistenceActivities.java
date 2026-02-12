package com.beema.kernel.workflow.policy;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.time.LocalDateTime;
import java.util.Map;

@ActivityInterface
public interface PersistenceActivities {

    @ActivityMethod
    PolicyVersionResult createPolicyVersion(
            String policyNumber,
            LocalDateTime inceptionDate,
            LocalDateTime expiryDate,
            Double premium,
            Map<String, Object> coverageDetails
    );

    @ActivityMethod
    PolicyVersionResult createEndorsementVersion(
            String policyNumber,
            LocalDateTime effectiveDate,
            Map<String, Object> changes,
            Double proRataAdjustment
    );

    @ActivityMethod
    void createCancellationVersion(
            String policyNumber,
            LocalDateTime effectiveDate,
            String reason,
            Double refundAmount
    );
}
