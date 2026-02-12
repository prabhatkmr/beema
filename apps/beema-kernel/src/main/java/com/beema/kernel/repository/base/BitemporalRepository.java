package com.beema.kernel.repository.base;

import com.beema.kernel.domain.base.BitemporalEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BitemporalRepository<T extends BitemporalEntity> {

    Optional<T> findCurrent(UUID id, String tenantId);

    Optional<T> findAsOf(UUID id, OffsetDateTime validAt, OffsetDateTime transactionAt, String tenantId);

    List<T> findHistoryInRange(UUID id, OffsetDateTime validFrom, OffsetDateTime validTo, String tenantId);

    T createNewVersion(T entity);

    List<T> findAuditTrail(UUID id, String tenantId);
}
