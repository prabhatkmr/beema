package com.beema.kernel.workflow.submission;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Activity interface for rating a submission.
 *
 * Calculates premium, tax, and total for a given submission's data.
 * In production, this would call an external rating engine;
 * the current implementation returns mock values.
 */
@ActivityInterface
public interface RatingActivities {

    /**
     * Rate a submission based on its data.
     *
     * @param data the submission form data
     * @return a map containing "premium", "tax", and "total" values
     */
    @ActivityMethod
    Map<String, Object> rate(Map<String, Object> data);
}
