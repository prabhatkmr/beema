package com.beema.kernel.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for bitemporal entities.
 *
 * Enforces temporal uniqueness at the database level:
 * - id: Business identifier (immutable across versions)
 * - validFrom: Start of validity period
 * - transactionTime: When this version was recorded
 *
 * Example:
 * <pre>
 * Agreement with id=123 has multiple temporal versions:
 * (123, 2024-01-01, 2024-01-01 10:00:00) - Original version
 * (123, 2024-01-01, 2024-02-15 14:30:00) - Corrected version (same valid period, later transaction)
 * (123, 2024-06-01, 2024-05-20 09:00:00) - Future version (new valid period)
 * </pre>
 */
@Embeddable
public class TemporalKey implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Business identifier - immutable across all temporal versions of the entity.
     */
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Start of the validity period (valid time dimension).
     * Represents when the data is/was/will be effective in the real world.
     */
    @Column(name = "valid_from", nullable = false, updatable = false)
    private OffsetDateTime validFrom;

    /**
     * When this record was created in the database (transaction time dimension).
     * Represents when we learned about this information.
     */
    @Column(name = "transaction_time", nullable = false, updatable = false)
    private OffsetDateTime transactionTime;

    // Constructors
    public TemporalKey() {
    }

    public TemporalKey(UUID id, OffsetDateTime validFrom, OffsetDateTime transactionTime) {
        this.id = id;
        this.validFrom = validFrom;
        this.transactionTime = transactionTime;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(OffsetDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    /**
     * Equals and hashCode must be implemented correctly for JPA composite keys.
     *
     * Note: We only use id and validFrom for equality, as transactionTime
     * represents different database records of the same logical version.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemporalKey that = (TemporalKey) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(validFrom, that.validFrom) &&
               Objects.equals(transactionTime, that.transactionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, validFrom, transactionTime);
    }

    /**
     * Creates a new temporal key with the current transaction time.
     */
    public static TemporalKey now(UUID id, OffsetDateTime validFrom) {
        return new TemporalKey(id, validFrom, OffsetDateTime.now());
    }
}
