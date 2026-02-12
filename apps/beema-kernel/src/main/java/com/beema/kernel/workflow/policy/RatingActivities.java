package com.beema.kernel.workflow.policy;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.time.LocalDateTime;

@ActivityInterface
public interface RatingActivities {

    @ActivityMethod
    ProRataResult calculateProRata(
            Double oldPremium,
            Double newPremium,
            LocalDateTime effectiveDate,
            LocalDateTime expiryDate
    );
}
