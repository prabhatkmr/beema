package com.beema.kernel.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TemporalKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "transaction_time", nullable = false, updatable = false)
    private OffsetDateTime transactionTime;

    protected TemporalKey() {
    }

    public TemporalKey(UUID id, OffsetDateTime validFrom, OffsetDateTime transactionTime) {
        this.id = id;
        this.validFrom = validFrom;
        this.transactionTime = transactionTime;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getTransactionTime() {
        return transactionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemporalKey that = (TemporalKey) o;
        return Objects.equals(id, that.id)
                && Objects.equals(validFrom, that.validFrom)
                && Objects.equals(transactionTime, that.transactionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, validFrom, transactionTime);
    }

    @Override
    public String toString() {
        return "TemporalKey{id=" + id + ", validFrom=" + validFrom + ", transactionTime=" + transactionTime + '}';
    }
}
