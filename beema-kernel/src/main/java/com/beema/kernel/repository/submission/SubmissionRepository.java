package com.beema.kernel.repository.submission;

import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.submission.Submission;
import com.beema.kernel.domain.submission.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Submission entity.
 *
 * Provides bitemporal queries following the same patterns as AgreementRepository.
 */
@Repository
public interface SubmissionRepository extends
    JpaRepository<Submission, TemporalKey>,
    JpaSpecificationExecutor<Submission> {

    // ========================================================================
    // Current Version Queries
    // ========================================================================

    /**
     * Find current version by submission ID (workflow ID).
     */
    @Query("""
        SELECT s FROM Submission s
        WHERE s.submissionId = :submissionId
          AND s.isCurrent = TRUE
        """)
    Optional<Submission> findCurrentBySubmissionId(@Param("submissionId") UUID submissionId);

    /**
     * Find all current submissions for a tenant.
     */
    @Query("""
        SELECT s FROM Submission s
        WHERE s.tenantId = :tenantId
          AND s.isCurrent = TRUE
        ORDER BY s.temporalKey.transactionTime DESC
        """)
    Page<Submission> findAllCurrentByTenant(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find all current submissions for a tenant filtered by status.
     */
    @Query("""
        SELECT s FROM Submission s
        WHERE s.tenantId = :tenantId
          AND s.status = :status
          AND s.isCurrent = TRUE
        ORDER BY s.temporalKey.transactionTime DESC
        """)
    Page<Submission> findAllCurrentByTenantAndStatus(
        @Param("tenantId") String tenantId,
        @Param("status") SubmissionStatus status,
        Pageable pageable
    );

    /**
     * Find all current submissions for a tenant filtered by product.
     */
    @Query("""
        SELECT s FROM Submission s
        WHERE s.tenantId = :tenantId
          AND s.product = :product
          AND s.isCurrent = TRUE
        ORDER BY s.temporalKey.transactionTime DESC
        """)
    List<Submission> findAllCurrentByTenantAndProduct(
        @Param("tenantId") String tenantId,
        @Param("product") String product
    );

    // ========================================================================
    // Temporal Version Management
    // ========================================================================

    /**
     * Mark previous versions as non-current when creating a new temporal version.
     */
    @Modifying
    @Query("""
        UPDATE Submission s
        SET s.isCurrent = FALSE
        WHERE s.submissionId = :submissionId
          AND s.isCurrent = TRUE
        """)
    void markPreviousVersionsNonCurrent(@Param("submissionId") UUID submissionId);

    // ========================================================================
    // Audit Trail
    // ========================================================================

    /**
     * Find complete audit trail for a submission.
     */
    @Query("""
        SELECT s FROM Submission s
        WHERE s.submissionId = :submissionId
        ORDER BY s.temporalKey.transactionTime DESC
        """)
    List<Submission> findAuditTrail(@Param("submissionId") UUID submissionId);
}
