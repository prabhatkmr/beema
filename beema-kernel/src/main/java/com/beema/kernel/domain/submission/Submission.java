package com.beema.kernel.domain.submission;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Submission entity - Bitemporal quote submission.
 *
 * Tracks the lifecycle of a quote submission from DRAFT through rating
 * to QUOTED and finally BOUND or DECLINED. Each state transition creates
 * a new bitemporal version for full audit trail.
 *
 * Links to a Temporal workflow via submissionId (workflow ID).
 */
@Entity
@Table(
    name = "submissions",
    indexes = {
        @Index(name = "idx_submissions_current", columnList = "submission_id, is_current"),
        @Index(name = "idx_submissions_tenant", columnList = "tenant_id, is_current"),
        @Index(name = "idx_submissions_product", columnList = "product, tenant_id, is_current"),
        @Index(name = "idx_submissions_status", columnList = "status, tenant_id, is_current"),
        @Index(name = "idx_submissions_temporal_range", columnList = "id, valid_from, valid_to")
    }
)
public class Submission extends BitemporalEntity {

    /**
     * Workflow ID - links to the Temporal workflow execution.
     */
    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    /**
     * Product type (e.g., "gadget", "auto", "home").
     */
    @Column(name = "product", nullable = false, length = 100)
    private String product;

    /**
     * Current lifecycle status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    /**
     * User-submitted form data (JSONB flex-schema).
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "form_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> formData = new HashMap<>();

    /**
     * Rating engine output: premium, tax, total (JSONB).
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "rating_result", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> ratingResult = new HashMap<>();

    public Submission() {
        super();
    }

    // Getters and Setters

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public Map<String, Object> getRatingResult() {
        return ratingResult;
    }

    public void setRatingResult(Map<String, Object> ratingResult) {
        this.ratingResult = ratingResult;
    }

    @Override
    public String toString() {
        return "Submission{" +
               "id=" + getId() +
               ", submissionId=" + submissionId +
               ", product='" + product + '\'' +
               ", status=" + status +
               ", validFrom=" + getValidFrom() +
               ", validTo=" + getValidTo() +
               ", isCurrent=" + getIsCurrent() +
               '}';
    }
}
