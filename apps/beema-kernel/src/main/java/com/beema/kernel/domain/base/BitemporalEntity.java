package com.beema.kernel.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BitemporalEntity implements Serializable {

    @EmbeddedId
    private TemporalKey temporalKey;

    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    protected BitemporalEntity() {
    }

    @PrePersist
    protected void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.temporalKey == null) {
            this.temporalKey = new TemporalKey(UUID.randomUUID(), now, now);
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitemporalEntity that = (BitemporalEntity) o;
        return Objects.equals(temporalKey, that.temporalKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(temporalKey);
    }
}
