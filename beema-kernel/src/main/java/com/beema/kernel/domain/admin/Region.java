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
    name = "sys_regions",
    indexes = {
        @Index(name = "idx_sys_regions_active", columnList = "is_active")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_region_code", columnNames = {"code"})
    }
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "data_residency_rules", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> dataResidencyRules = new HashMap<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Region() {}

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getDataResidencyRules() { return dataResidencyRules; }
    public void setDataResidencyRules(Map<String, Object> dataResidencyRules) { this.dataResidencyRules = dataResidencyRules; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "Region{" +
               "id=" + id +
               ", code='" + code + '\'' +
               ", name='" + name + '\'' +
               ", isActive=" + isActive +
               '}';
    }
}
