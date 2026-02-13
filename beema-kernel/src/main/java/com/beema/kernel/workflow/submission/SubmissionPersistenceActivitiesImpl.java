package com.beema.kernel.workflow.submission;

import com.beema.kernel.domain.submission.SubmissionStatus;
import com.beema.kernel.service.submission.SubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of SubmissionPersistenceActivities.
 *
 * Delegates to SubmissionService for actual database operations.
 * Runs outside workflow context so Spring services are accessible.
 */
public class SubmissionPersistenceActivitiesImpl implements SubmissionPersistenceActivities {

    private static final Logger log = LoggerFactory.getLogger(SubmissionPersistenceActivitiesImpl.class);

    private final SubmissionService submissionService;

    public SubmissionPersistenceActivitiesImpl(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Override
    public void saveDraft(String submissionId, String product, Map<String, Object> formData, String tenantId) {
        log.info("Persisting DRAFT submission: {}", submissionId);
        submissionService.createSubmission(UUID.fromString(submissionId), product, formData, tenantId);
    }

    @Override
    public void saveQuoted(String submissionId, Map<String, Object> ratingResult) {
        log.info("Persisting QUOTED submission: {}", submissionId);
        submissionService.updateStatus(UUID.fromString(submissionId), SubmissionStatus.QUOTED, ratingResult);
    }

    @Override
    public void saveBound(String submissionId) {
        log.info("Persisting BOUND submission: {}", submissionId);
        submissionService.updateStatus(UUID.fromString(submissionId), SubmissionStatus.BOUND, null);
    }
}
