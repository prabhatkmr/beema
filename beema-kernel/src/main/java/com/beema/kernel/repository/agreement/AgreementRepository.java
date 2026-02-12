package com.beema.kernel.repository.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.metadata.MarketContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Agreement entity.
 *
 * Extends JpaRepository for standard CRUD and
 * JpaSpecificationExecutor for dynamic queries.
 */
@Repository
public interface AgreementRepository extends
    JpaRepository<Agreement, TemporalKey>,
    JpaSpecificationExecutor<Agreement> {

    // ========================================================================
    // Current Version Queries
    // ========================================================================

    /**
     * Find current version by business ID.
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.temporalKey.id = :id
          AND a.isCurrent = TRUE
        """)
    Optional<Agreement> findCurrentById(@Param("id") UUID id);

    /**
     * Find current version by agreement number and tenant.
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.agreementNumber = :agreementNumber
          AND a.tenantId = :tenantId
          AND a.isCurrent = TRUE
        """)
    Optional<Agreement> findCurrentByAgreementNumber(
        @Param("agreementNumber") String agreementNumber,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all current agreements for tenant and market context.
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.tenantId = :tenantId
          AND a.marketContext = :marketContext
          AND a.isCurrent = TRUE
        ORDER BY a.agreementNumber
        """)
    Page<Agreement> findAllCurrentByTenantAndContext(
        @Param("tenantId") String tenantId,
        @Param("marketContext") MarketContext marketContext,
        Pageable pageable
    );

    /**
     * Find all current agreements by status.
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.tenantId = :tenantId
          AND a.status = :status
          AND a.isCurrent = TRUE
        ORDER BY a.agreementNumber
        """)
    List<Agreement> findAllCurrentByTenantAndStatus(
        @Param("tenantId") String tenantId,
        @Param("status") AgreementStatus status
    );

    // ========================================================================
    // Temporal Queries
    // ========================================================================

    /**
     * Find agreement as of a specific point in time.
     *
     * @param id Business ID
     * @param validTime Valid time (when data was effective)
     * @param transactionTime Transaction time (when we knew about it)
     * @return Version valid at that point in time
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.temporalKey.id = :id
          AND a.temporalKey.validFrom <= :validTime
          AND a.validTo > :validTime
          AND a.temporalKey.transactionTime <= :transactionTime
        ORDER BY a.temporalKey.transactionTime DESC
        LIMIT 1
        """)
    Optional<Agreement> findAsOf(
        @Param("id") UUID id,
        @Param("validTime") OffsetDateTime validTime,
        @Param("transactionTime") OffsetDateTime transactionTime
    );

    /**
     * Find all versions within a validity period.
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.temporalKey.id = :id
          AND a.temporalKey.validFrom >= :validFromStart
          AND a.temporalKey.validFrom < :validFromEnd
        ORDER BY a.temporalKey.validFrom, a.temporalKey.transactionTime DESC
        """)
    List<Agreement> findHistoryInRange(
        @Param("id") UUID id,
        @Param("validFromStart") OffsetDateTime validFromStart,
        @Param("validFromEnd") OffsetDateTime validFromEnd
    );

    /**
     * Find complete audit trail.
     */
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.temporalKey.id = :id
        ORDER BY a.temporalKey.validFrom, a.temporalKey.transactionTime DESC
        """)
    List<Agreement> findAuditTrail(@Param("id") UUID id);

    // ========================================================================
    // JSONB Queries
    // ========================================================================

    /**
     * Find agreements by JSONB attribute containment.
     *
     * Uses PostgreSQL @> operator for JSONB containment.
     * Example: Find all Honda vehicles
     *
     * @param tenantId Tenant ID
     * @param attributeJson JSON string to match (e.g., '{"vehicle_make": "Honda"}')
     * @return List of matching agreements
     */
    @Query(value = """
        SELECT * FROM agreements a
        WHERE a.tenant_id = :tenantId
          AND a.is_current = TRUE
          AND a.attributes @> CAST(:attributeJson AS jsonb)
        ORDER BY a.agreement_number
        """, nativeQuery = true)
    List<Agreement> findByAttributeContainment(
        @Param("tenantId") String tenantId,
        @Param("attributeJson") String attributeJson
    );

    /**
     * Find agreements by specific JSONB field value.
     *
     * Example: Find by VIN
     *
     * @param tenantId Tenant ID
     * @param fieldPath JSONB field path (e.g., 'vehicle_vin')
     * @param value Field value
     * @return List of matching agreements
     */
    @Query(value = """
        SELECT * FROM agreements a
        WHERE a.tenant_id = :tenantId
          AND a.is_current = TRUE
          AND a.attributes->>:fieldPath = :value
        ORDER BY a.agreement_number
        """, nativeQuery = true)
    List<Agreement> findByAttributeField(
        @Param("tenantId") String tenantId,
        @Param("fieldPath") String fieldPath,
        @Param("value") String value
    );

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Check if agreement exists (current version).
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM Agreement a
        WHERE a.temporalKey.id = :id
          AND a.isCurrent = TRUE
        """)
    boolean existsCurrentById(@Param("id") UUID id);

    /**
     * Count current agreements by market context.
     */
    @Query("""
        SELECT COUNT(a) FROM Agreement a
        WHERE a.tenantId = :tenantId
          AND a.marketContext = :marketContext
          AND a.isCurrent = TRUE
        """)
    long countCurrentByTenantAndContext(
        @Param("tenantId") String tenantId,
        @Param("marketContext") MarketContext marketContext
    );
}
