package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.domain.agreement.MarketContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Request body for registering or updating a metadata agreement type")
public class MetadataTypeRequest {

    @NotNull(message = "Tenant ID is required")
    @Schema(description = "Tenant identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;

    @NotBlank(message = "Type code is required")
    @Size(max = 100, message = "Type code must not exceed 100 characters")
    @Schema(description = "Unique type code within tenant and market context", example = "MOTOR_FLEET")
    private String typeCode;

    @NotBlank(message = "Type name is required")
    @Size(max = 255, message = "Type name must not exceed 255 characters")
    @Schema(description = "Human-readable type name", example = "Motor Fleet Insurance")
    private String typeName;

    @Schema(description = "Description of the agreement type")
    private String description;

    @NotNull(message = "Market context is required")
    @Schema(description = "Market context (RETAIL, COMMERCIAL, LONDON_MARKET)", example = "COMMERCIAL")
    private MarketContext marketContext;

    @NotNull(message = "Attribute schema is required")
    @Schema(description = "JSON Schema defining the attributes structure for this agreement type")
    private Map<String, Object> attributeSchema;

    @Schema(description = "Validation rules for this agreement type")
    private Map<String, Object> validationRules;

    @Schema(description = "UI configuration for rendering this agreement type")
    private Map<String, Object> uiConfiguration;

    @Schema(description = "User who is creating/updating this type")
    private String updatedBy;

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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
