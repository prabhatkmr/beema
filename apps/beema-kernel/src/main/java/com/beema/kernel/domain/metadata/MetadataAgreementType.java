package com.beema.kernel.domain.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.util.JsonbConverter;
import com.beema.kernel.util.JsonbListConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "metadata_agreement_types")
public class MetadataAgreementType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "type_code", nullable = false, length = 100)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 255)
    private String typeName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_context", nullable = false, columnDefinition = "market_context_type")
    private MarketContext marketContext;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "attribute_schema", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> attributeSchema;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "validation_rules", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> validationRules;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "ui_configuration", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> uiConfiguration;

    @Convert(converter = JsonbListConverter.class)
    @Column(name = "calculation_rules", nullable = false, columnDefinition = "JSONB")
    private List<Map<String, Object>> calculationRules = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    public MetadataAgreementType() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(MarketContext marketContext) {
        this.marketContext = marketContext;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, Object> getAttributeSchema() {
        return attributeSchema;
    }

    public void setAttributeSchema(Map<String, Object> attributeSchema) {
        this.attributeSchema = attributeSchema;
    }

    public Map<String, Object> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
    }

    public Map<String, Object> getUiConfiguration() {
        return uiConfiguration;
    }

    public void setUiConfiguration(Map<String, Object> uiConfiguration) {
        this.uiConfiguration = uiConfiguration;
    }

    public List<Map<String, Object>> getCalculationRules() {
        return calculationRules;
    }

    public void setCalculationRules(List<Map<String, Object>> calculationRules) {
        this.calculationRules = calculationRules != null ? calculationRules : new ArrayList<>();
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        this.isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataAgreementType that = (MetadataAgreementType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MetadataAgreementType{id=" + id + ", typeCode='" + typeCode + "', marketContext=" + marketContext + '}';
    }
}
