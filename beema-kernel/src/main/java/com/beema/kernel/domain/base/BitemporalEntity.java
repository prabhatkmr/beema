package com.beema.kernel.domain.base;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Abstract base class for all bitemporal entities.
 *
 * Bitemporal Pattern:
 * - Valid Time: When data is/was/will be true in the real world (valid_from, valid_to)
 * - Transaction Time: When data was recorded in the database (transaction_time)
 *
 * This enables:
 * - Time travel queries: "What did we know on 2024-01-15?"
 * - Point-in-time queries: "What was valid on 2024-01-15?"
 * - Full audit trail: "Who changed what, when?"
 * - Corrections without data loss: Create new transaction version
 *
 * Example Usage:
 * <pre>
 * // Query current version
 * Agreement current = repo.findCurrent(agreementId);
 *
 * // Query as of specific time
 * Agreement asOf = repo.findAsOf(agreementId,
 *     OffsetDateTime.parse("2024-01-15T10:00:00Z"),
 *     OffsetDateTime.parse("2024-01-15T10:00:00Z"));
 *
 * // Create new version (temporal update)
 * Agreement newVersion = current.clone();
 * newVersion.setValidFrom(OffsetDateTime.now());
 * newVersion.setTransactionTime(OffsetDateTime.now());
 * newVersion.setVersion(current.getVersion() + 1);
 * repo.save(newVersion);
 * </pre>
 */
@MappedSuperclass
public abstract class BitemporalEntity {

    /**
     * Composite primary key: (id, valid_from, transaction_time)
     */
    @EmbeddedId
    private TemporalKey temporalKey;

    /**
     * End of validity period. Defaults to far future (9999-12-31).
     * This creates a half-open interval: [valid_from, valid_to)
     */
    @Column(name = "valid_to", nullable = false)
    private OffsetDateTime validTo = OffsetDateTime.parse("9999-12-31T23:59:59Z");

    /**
     * Flag indicating this is the current version (latest transaction_time).
     * Optimizes queries for "current state" which is the most common use case.
     */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    /**
     * Multi-tenancy: Tenant identifier for row-level security.
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Audit: User who created this version.
     */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    /**
     * Audit: User who last updated this version.
     */
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    /**
     * Optimistic locking version.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /**
     * Row creation timestamp (auto-populated).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Row update timestamp (auto-populated).
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Getters and Setters
    public TemporalKey getTemporalKey() {
        return temporalKey;
    }

    public void setTemporalKey(TemporalKey temporalKey) {
        this.temporalKey = temporalKey;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Lifecycle callback: Set defaults before persisting new entity.
     */
    @PrePersist
    protected void onCreate() {
        if (temporalKey == null) {
            throw new IllegalStateException("TemporalKey must be set before persisting");
        }

        if (validTo == null) {
            validTo = OffsetDateTime.parse("9999-12-31T23:59:59Z");
        }

        if (isCurrent == null) {
            isCurrent = true;
        }

        if (version == null) {
            version = 1L;
        }
    }

    /**
     * Helper: Get the business ID from the composite key.
     */
    public java.util.UUID getId() {
        return temporalKey != null ? temporalKey.getId() : null;
    }

    /**
     * Helper: Get valid_from from the composite key.
     */
    public OffsetDateTime getValidFrom() {
        return temporalKey != null ? temporalKey.getValidFrom() : null;
    }

    /**
     * Helper: Get transaction_time from the composite key.
     */
    public OffsetDateTime getTransactionTime() {
        return temporalKey != null ? temporalKey.getTransactionTime() : null;
    }

    /**
     * Check if this version is valid at a specific point in time.
     */
    public boolean isValidAt(OffsetDateTime pointInTime) {
        return !getValidFrom().isAfter(pointInTime) && getValidTo().isAfter(pointInTime);
    }

    /**
     * Check if this version overlaps with a time range.
     */
    public boolean overlaps(OffsetDateTime start, OffsetDateTime end) {
        return !getValidFrom().isAfter(end) && !getValidTo().isBefore(start);
    }
}
