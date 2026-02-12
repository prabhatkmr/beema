package com.beema.kernel.repository.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.base.TemporalKey;
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

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, TemporalKey>,
        JpaSpecificationExecutor<Agreement> {

    @Query("SELECT a FROM Agreement a WHERE a.agreementNumber = :agreementNumber " +
            "AND a.tenantId = :tenantId AND a.isCurrent = true AND a.validTo IS NULL")
    Optional<Agreement> findCurrentByAgreementNumber(
            @Param("agreementNumber") String agreementNumber,
            @Param("tenantId") String tenantId);

    @Query("SELECT a FROM Agreement a WHERE a.tenantId = :tenantId " +
            "AND a.marketContext = :marketContext AND a.isCurrent = true AND a.validTo IS NULL")
    Page<Agreement> findAllCurrentByTenantAndContext(
            @Param("tenantId") String tenantId,
            @Param("marketContext") MarketContext marketContext,
            Pageable pageable);

    @Query("SELECT a FROM Agreement a WHERE a.temporalKey.id = :id " +
            "AND a.tenantId = :tenantId AND a.isCurrent = true AND a.validTo IS NULL")
    Optional<Agreement> findCurrentById(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId);

    @Query("SELECT a FROM Agreement a WHERE a.temporalKey.id = :id " +
            "AND a.tenantId = :tenantId " +
            "AND a.temporalKey.validFrom <= :validAt " +
            "AND (a.validTo IS NULL OR a.validTo > :validAt) " +
            "ORDER BY a.temporalKey.transactionTime DESC")
    List<Agreement> findAsOf(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId,
            @Param("validAt") OffsetDateTime validAt);

    @Query("SELECT a FROM Agreement a WHERE a.temporalKey.id = :id " +
            "AND a.tenantId = :tenantId " +
            "ORDER BY a.temporalKey.validFrom ASC, a.temporalKey.transactionTime ASC")
    List<Agreement> findHistory(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId);

    @Query("SELECT a FROM Agreement a WHERE a.tenantId = :tenantId " +
            "AND a.isCurrent = true AND a.validTo IS NULL AND a.status = :status")
    Page<Agreement> findAllCurrentByTenantAndStatus(
            @Param("tenantId") String tenantId,
            @Param("status") AgreementStatus status,
            Pageable pageable);

    @Query(value = "SELECT a.* FROM agreements a WHERE a.tenant_id = :tenantId " +
            "AND a.is_current = true AND a.valid_to IS NULL " +
            "AND a.attributes @> CAST(:jsonFilter AS jsonb)",
            nativeQuery = true)
    List<Agreement> findByAttribute(
            @Param("tenantId") String tenantId,
            @Param("jsonFilter") String jsonFilter);
}
