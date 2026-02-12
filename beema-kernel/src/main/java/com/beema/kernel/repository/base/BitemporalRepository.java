package com.beema.kernel.repository.base;

import com.beema.kernel.domain.base.BitemporalEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for bitemporal entities.
 *
 * Provides standard temporal query operations:
 * - Current version queries (is_current = TRUE)
 * - Point-in-time queries (valid time + transaction time)
 * - History queries (all versions)
 * - Version creation
 *
 * @param <T> Bitemporal entity type
 */
public interface BitemporalRepository<T extends BitemporalEntity> {

    /**
     * Find current version of entity.
     *
     * @param id Business ID
     * @return Current version if found
     */
    Optional<T> findCurrent(UUID id);

    /**
     * Find entity as of a specific point in time.
     *
     * Queries:
     * - Valid time: Entity was valid at this time
     * - Transaction time: We knew about it at this time
     *
     * @param id Business ID
     * @param validTime Valid time (when data was effective)
     * @param transactionTime Transaction time (when we knew about it)
     * @return Version valid at that point in time
     */
    Optional<T> findAsOf(UUID id, OffsetDateTime validTime, OffsetDateTime transactionTime);

    /**
     * Find all versions of an entity within a time range.
     *
     * @param id Business ID
     * @param validFromStart Start of valid time range
     * @param validFromEnd End of valid time range
     * @return List of versions in range
     */
    List<T> findHistoryInRange(UUID id, OffsetDateTime validFromStart, OffsetDateTime validFromEnd);

    /**
     * Find complete audit trail for an entity.
     *
     * Returns all temporal versions ordered by:
     * 1. valid_from (ascending)
     * 2. transaction_time (descending)
     *
     * @param id Business ID
     * @return Complete history
     */
    List<T> findAuditTrail(UUID id);

    /**
     * Create new temporal version.
     *
     * Automatically:
     * 1. Sets is_current = FALSE on previous version
     * 2. Sets is_current = TRUE on new version
     * 3. Sets transaction_time = CURRENT_TIMESTAMP
     *
     * @param entity New version to create
     * @return Saved entity
     */
    T createNewVersion(T entity);

    /**
     * Save entity (standard JPA save).
     *
     * Use this for initial creation. Use createNewVersion() for temporal updates.
     *
     * @param entity Entity to save
     * @return Saved entity
     */
    T save(T entity);

    /**
     * Find all current versions for a tenant.
     *
     * @param tenantId Tenant ID
     * @return List of current versions
     */
    List<T> findAllCurrentByTenant(String tenantId);
}
