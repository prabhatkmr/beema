package com.beema.kernel.domain.layout;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sys_layouts")
public class Layout {

    @Id
    @GeneratedValue
    @Column(name = "layout_id")
    private UUID layoutId;

    @Column(name = "layout_name", nullable = false)
    private String layoutName;

    @Column(name = "layout_type", nullable = false)
    private String layoutType;

    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "object_type", nullable = false)
    private String objectType;

    @Column(name = "market_context", nullable = false)
    private String marketContext;

    @Column(name = "role")
    private String role;

    @Column(name = "tenant_id")
    private String tenantId;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "layout_schema", columnDefinition = "jsonb")
    private Map<String, Object> layoutSchema;

    @Column(name = "version")
    private Integer version;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    // Getters and Setters
    public UUID getLayoutId() {
        return layoutId;
    }

    public void setLayoutId(UUID layoutId) {
        this.layoutId = layoutId;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(String layoutType) {
        this.layoutType = layoutType;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(String marketContext) {
        this.marketContext = marketContext;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getLayoutSchema() {
        return layoutSchema;
    }

    public void setLayoutSchema(Map<String, Object> layoutSchema) {
        this.layoutSchema = layoutSchema;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
