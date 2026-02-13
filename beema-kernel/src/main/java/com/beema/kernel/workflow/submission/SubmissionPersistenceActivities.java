package com.beema.kernel.workflow.submission;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Temporal activity interface for submission persistence.
 *
 * Workflow code must be deterministic, so database operations
 * are performed through activities that delegate to SubmissionService.
 */
@ActivityInterface
public interface SubmissionPersistenceActivities {

    /**
     * Persist a new submission as DRAFT.
     */
    @ActivityMethod
    void saveDraft(String submissionId, String product, Map<String, Object> formData, String tenantId);

    /**
     * Update submission status to QUOTED with rating result.
     */
    @ActivityMethod
    void saveQuoted(String submissionId, Map<String, Object> ratingResult);

    /**
     * Update submission status to BOUND.
     */
    @ActivityMethod
    void saveBound(String submissionId);
}
