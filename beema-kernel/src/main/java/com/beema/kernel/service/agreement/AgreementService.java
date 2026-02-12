package com.beema.kernel.service.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for agreement management.
 *
 * Provides:
 * - CRUD operations with temporal versioning
 * - Validation against metadata schemas
 * - Point-in-time queries
 * - Audit trail access
 */
public interface AgreementService {

    /**
     * Create new agreement.
     *
     * Validates attributes against metadata schema before saving.
     *
     * @param agreement Agreement to create
     * @return Created agreement
     * @throws IllegalArgumentException if validation fails
     */
    Agreement createAgreement(Agreement agreement);

    /**
     * Update agreement (creates new temporal version).
     *
     * Workflow:
     * 1. Validate attributes against schema
     * 2. Close previous version (set is_current = FALSE)
     * 3. Create new version with updated data
     * 4. Set effectiveFrom as new valid_from
     *
     * @param id Business ID
     * @param updates Updated fields
     * @param effectiveFrom When changes become effective (valid_from)
     * @param updatedBy User making the change
     * @return New version
     */
    Agreement updateAgreement(
        UUID id,
        Map<String, Object> updates,
        OffsetDateTime effectiveFrom,
        String updatedBy
    );

    /**
     * Get current version of agreement.
     *
     * @param id Business ID
     * @return Current version if found
     */
    Optional<Agreement> getCurrentAgreement(UUID id);

    /**
     * Get agreement by number.
     *
     * @param agreementNumber Agreement number
     * @param tenantId Tenant ID
     * @return Current version if found
     */
    Optional<Agreement> getAgreementByNumber(String agreementNumber, String tenantId);

    /**
     * Get agreement as of a specific point in time.
     *
     * @param id Business ID
     * @param validTime When the data was effective
     * @param transactionTime When we knew about it
     * @return Version at that point in time
     */
    Optional<Agreement> getAgreementAsOf(
        UUID id,
        OffsetDateTime validTime,
        OffsetDateTime transactionTime
    );

    /**
     * Get complete audit trail for an agreement.
     *
     * @param id Business ID
     * @return All temporal versions
     */
    List<Agreement> getAgreementHistory(UUID id);

    /**
     * Find agreements by tenant and market context.
     *
     * @param tenantId Tenant ID
     * @param marketContext Market context
     * @param pageable Pagination
     * @return Page of current agreements
     */
    Page<Agreement> findAgreementsByTenantAndContext(
        String tenantId,
        MarketContext marketContext,
        Pageable pageable
    );

    /**
     * Find agreements by status.
     *
     * @param tenantId Tenant ID
     * @param status Agreement status
     * @return List of current agreements
     */
    List<Agreement> findAgreementsByStatus(String tenantId, AgreementStatus status);

    /**
     * Find agreements by JSONB attribute.
     *
     * Example: Find all Honda vehicles
     * <pre>
     * findByAttribute("tenant-123", Map.of("vehicle_make", "Honda"))
     * </pre>
     *
     * @param tenantId Tenant ID
     * @param attributes Attributes to match
     * @return List of matching agreements
     */
    List<Agreement> findByAttribute(String tenantId, Map<String, Object> attributes);

    /**
     * Change agreement status.
     *
     * Creates new temporal version with updated status.
     *
     * @param id Business ID
     * @param newStatus New status
     * @param effectiveFrom When status change is effective
     * @param updatedBy User making the change
     * @return Updated agreement
     */
    Agreement changeStatus(
        UUID id,
        AgreementStatus newStatus,
        OffsetDateTime effectiveFrom,
        String updatedBy
    );

    /**
     * Validate agreement attributes against schema.
     *
     * @param agreement Agreement to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateAgreement(Agreement agreement);

    /**
     * Count agreements by market context.
     *
     * @param tenantId Tenant ID
     * @param marketContext Market context
     * @return Count of current agreements
     */
    long countByTenantAndContext(String tenantId, MarketContext marketContext);
}
