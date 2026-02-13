package com.beema.kernel.service.submission;

import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.submission.Submission;
import com.beema.kernel.domain.submission.SubmissionStatus;
import com.beema.kernel.repository.submission.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing submission persistence with bitemporal versioning.
 *
 * Each state transition creates a new temporal version of the submission,
 * preserving full audit trail while keeping the latest version marked as current.
 */
@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Create a new submission in DRAFT status.
     *
     * @param submissionId workflow ID
     * @param product      product type
     * @param formData     user-submitted form data
     * @param tenantId     tenant identifier
     * @return persisted submission
     */
    @Transactional
    public Submission createSubmission(UUID submissionId, String product, Map<String, Object> formData, String tenantId) {
        log.info("Creating submission: submissionId={}, product={}, tenant={}", submissionId, product, tenantId);

        OffsetDateTime now = OffsetDateTime.now();

        Submission submission = new Submission();
        submission.setTemporalKey(TemporalKey.now(UUID.randomUUID(), now));
        submission.setSubmissionId(submissionId);
        submission.setProduct(product);
        submission.setStatus(SubmissionStatus.DRAFT);
        submission.setFormData(formData != null ? formData : Map.of());
        submission.setRatingResult(Map.of());
        submission.setTenantId(tenantId);
        submission.setCreatedBy("system");
        submission.setUpdatedBy("system");

        Submission saved = submissionRepository.save(submission);
        log.info("Submission created: submissionId={}, id={}", submissionId, saved.getId());
        return saved;
    }

    /**
     * Update submission status with a new temporal version.
     *
     * Marks previous versions as non-current and creates a new version
     * with the updated status and optional rating result.
     *
     * @param submissionId  workflow ID
     * @param status        new status
     * @param ratingResult  rating result (nullable, used when transitioning to QUOTED)
     * @return new temporal version
     */
    @Transactional
    public Submission updateStatus(UUID submissionId, SubmissionStatus status, Map<String, Object> ratingResult) {
        log.info("Updating submission status: submissionId={}, newStatus={}", submissionId, status);

        Submission current = submissionRepository.findCurrentBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));

        // Mark previous versions as non-current
        submissionRepository.markPreviousVersionsNonCurrent(submissionId);

        // Create new temporal version
        OffsetDateTime now = OffsetDateTime.now();
        Submission newVersion = new Submission();
        newVersion.setTemporalKey(TemporalKey.now(current.getId(), now));
        newVersion.setSubmissionId(submissionId);
        newVersion.setProduct(current.getProduct());
        newVersion.setStatus(status);
        newVersion.setFormData(current.getFormData());
        newVersion.setRatingResult(ratingResult != null ? ratingResult : current.getRatingResult());
        newVersion.setTenantId(current.getTenantId());
        newVersion.setCreatedBy(current.getCreatedBy());
        newVersion.setUpdatedBy("system");
        newVersion.setVersion(current.getVersion() + 1);

        Submission saved = submissionRepository.save(newVersion);
        log.info("Submission updated: submissionId={}, status={}, version={}", submissionId, status, saved.getVersion());
        return saved;
    }

    /**
     * Get the current version of a submission.
     *
     * @param submissionId workflow ID
     * @return current submission or null if not found
     */
    @Transactional(readOnly = true)
    public Submission getSubmission(UUID submissionId) {
        return submissionRepository.findCurrentBySubmissionId(submissionId).orElse(null);
    }

    /**
     * List current submissions for a tenant, optionally filtered by status.
     *
     * @param tenantId tenant identifier
     * @param status   optional status filter
     * @param pageable pagination parameters
     * @return page of submissions
     */
    @Transactional(readOnly = true)
    public Page<Submission> listSubmissions(String tenantId, SubmissionStatus status, Pageable pageable) {
        if (status != null) {
            return submissionRepository.findAllCurrentByTenantAndStatus(tenantId, status, pageable);
        }
        return submissionRepository.findAllCurrentByTenant(tenantId, pageable);
    }
}
