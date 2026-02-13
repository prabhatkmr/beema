package com.beema.kernel.domain.admin;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "sys_tenants",
    indexes = {
        @Index(name = "idx_sys_tenants_status", columnList = "status"),
        @Index(name = "idx_sys_tenants_region", columnList = "region_code"),
        @Index(name = "idx_sys_tenants_tier", columnList = "tier")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tenant_tenant_id", columnNames = {"tenant_id"}),
        @UniqueConstraint(name = "uq_tenant_slug", columnNames = {"slug"})
    }
)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "tier", nullable = false, length = 50)
    private String tier = "STANDARD";

    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode = "US";

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    @Column(name = "datasource_key", length = 100)
    private String datasourceKey;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy = "system";

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy = "system";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Tenant() {}

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public String getDatasourceKey() { return datasourceKey; }
    public void setDatasourceKey(String datasourceKey) { this.datasourceKey = datasourceKey; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "Tenant{" +
               "id=" + id +
               ", tenantId='" + tenantId + '\'' +
               ", name='" + name + '\'' +
               ", status='" + status + '\'' +
               ", tier='" + tier + '\'' +
               ", region='" + regionCode + '\'' +
               '}';
    }
}
