package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Response body for a metadata agreement type")
public class MetadataTypeResponse {

    @Schema(description = "Unique identifier")
    private UUID id;

    @Schema(description = "Tenant identifier")
    private UUID tenantId;

    @Schema(description = "Unique type code within tenant and market context")
    private String typeCode;

    @Schema(description = "Human-readable type name")
    private String typeName;

    @Schema(description = "Description of the agreement type")
    private String description;

    @Schema(description = "Market context")
    private MarketContext marketContext;

    @Schema(description = "Schema version number")
    private Integer schemaVersion;

    @Schema(description = "JSON Schema defining the attributes structure")
    private Map<String, Object> attributeSchema;

    @Schema(description = "Validation rules")
    private Map<String, Object> validationRules;

    @Schema(description = "UI configuration")
    private Map<String, Object> uiConfiguration;

    @Schema(description = "Whether this type is active")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    private OffsetDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private OffsetDateTime updatedAt;

    @Schema(description = "User who created this type")
    private String createdBy;

    @Schema(description = "User who last updated this type")
    private String updatedBy;

    public static MetadataTypeResponse fromEntity(MetadataAgreementType entity) {
        MetadataTypeResponse response = new MetadataTypeResponse();
        response.setId(entity.getId());
        response.setTenantId(entity.getTenantId());
        response.setTypeCode(entity.getTypeCode());
        response.setTypeName(entity.getTypeName());
        response.setDescription(entity.getDescription());
        response.setMarketContext(entity.getMarketContext());
        response.setSchemaVersion(entity.getSchemaVersion());
        @SuppressWarnings("unchecked")
        Map<String, Object> attributeSchema = (Map<String, Object>) entity.getAttributeSchema();
        response.setAttributeSchema(attributeSchema);
        @SuppressWarnings("unchecked")
        Map<String, Object> validationRules = (Map<String, Object>) entity.getValidationRules();
        response.setValidationRules(validationRules);
        @SuppressWarnings("unchecked")
        Map<String, Object> uiConfiguration = (Map<String, Object>) entity.getUiConfiguration();
        response.setUiConfiguration(uiConfiguration);
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());
        return response;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        this.isActive = active;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
