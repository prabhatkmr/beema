package com.beema.kernel.repository.base;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.domain.base.TemporalKey;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BitemporalRepositoryImpl<T extends BitemporalEntity> implements BitemporalRepository<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;

    public BitemporalRepositoryImpl(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Override
    public Optional<T> findCurrent(UUID id, String tenantId) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e " +
                "WHERE e.temporalKey.id = :id " +
                "AND e.tenantId = :tenantId " +
                "AND e.validTo IS NULL " +
                "ORDER BY e.temporalKey.transactionTime DESC";

        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .setMaxResults(1);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<T> findAsOf(UUID id, OffsetDateTime validAt, OffsetDateTime transactionAt, String tenantId) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e " +
                "WHERE e.temporalKey.id = :id " +
                "AND e.tenantId = :tenantId " +
                "AND e.temporalKey.validFrom <= :validAt " +
                "AND (e.validTo IS NULL OR e.validTo > :validAt) " +
                "AND e.temporalKey.transactionTime <= :transactionAt " +
                "ORDER BY e.temporalKey.transactionTime DESC";

        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .setParameter("validAt", validAt)
                .setParameter("transactionAt", transactionAt)
                .setMaxResults(1);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<T> findHistoryInRange(UUID id, OffsetDateTime validFrom, OffsetDateTime validTo, String tenantId) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e " +
                "WHERE e.temporalKey.id = :id " +
                "AND e.tenantId = :tenantId " +
                "AND e.temporalKey.validFrom < :validTo " +
                "AND (e.validTo IS NULL OR e.validTo > :validFrom) " +
                "ORDER BY e.temporalKey.validFrom ASC, e.temporalKey.transactionTime DESC";

        return entityManager.createQuery(jpql, entityClass)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .setParameter("validFrom", validFrom)
                .setParameter("validTo", validTo)
                .getResultList();
    }

    @Override
    public T createNewVersion(T entity) {
        UUID id = entity.getTemporalKey().getId();
        UUID tenantId = entity.getTenantId();

        // Close the current version by setting its validTo
        Optional<T> current = findCurrent(id, tenantId.toString());
        OffsetDateTime now = OffsetDateTime.now();

        current.ifPresent(existing -> {
            existing.setValidTo(now);
            entityManager.merge(existing);
        });

        // Create the new version with a fresh temporal key
        TemporalKey newKey = new TemporalKey(id, now, now);
        entity.setTemporalKey(newKey);
        entity.setValidTo(null);

        entityManager.persist(entity);
        return entity;
    }

    @Override
    public List<T> findAuditTrail(UUID id, String tenantId) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e " +
                "WHERE e.temporalKey.id = :id " +
                "AND e.tenantId = :tenantId " +
                "ORDER BY e.temporalKey.transactionTime ASC";

        return entityManager.createQuery(jpql, entityClass)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }
}
