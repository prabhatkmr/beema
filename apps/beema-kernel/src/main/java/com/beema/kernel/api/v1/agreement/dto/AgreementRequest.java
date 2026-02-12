package com.beema.kernel.api.v1.agreement.dto;

import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Request body for creating or updating an agreement")
public class AgreementRequest {

    @NotBlank(message = "Agreement number is required")
    @Size(max = 50, message = "Agreement number must not exceed 50 characters")
    @Schema(description = "Unique agreement number", example = "AGR-2026-001")
    private String agreementNumber;

    @Size(max = 100, message = "External reference must not exceed 100 characters")
    @Schema(description = "External system reference")
    private String externalReference;

    @NotNull(message = "Market context is required")
    @Schema(description = "Market context", example = "COMMERCIAL")
    private MarketContext marketContext;

    @NotNull(message = "Agreement type ID is required")
    @Schema(description = "Metadata agreement type ID")
    private UUID agreementTypeId;

    @Schema(description = "Agreement status", example = "DRAFT")
    private AgreementStatus status;

    @NotNull(message = "Tenant ID is required")
    @Schema(description = "Tenant identifier")
    private String tenantId;

    @Schema(description = "Data residency region", example = "EU")
    private String dataResidencyRegion;

    @NotNull(message = "Inception date is required")
    @Schema(description = "Policy inception date", example = "2026-01-01")
    private LocalDate inceptionDate;

    @NotNull(message = "Expiry date is required")
    @Schema(description = "Policy expiry date", example = "2027-01-01")
    private LocalDate expiryDate;

    @Schema(description = "Currency code", example = "GBP")
    private String currencyCode;

    @Schema(description = "Total premium amount")
    private BigDecimal totalPremium;

    @Schema(description = "Total sum insured")
    private BigDecimal totalSumInsured;

    @Schema(description = "Flexible JSONB attributes validated against agreement type schema")
    private Map<String, Object> attributes;

    @Schema(description = "User performing the operation")
    private String updatedBy;

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(MarketContext marketContext) {
        this.marketContext = marketContext;
    }

    public UUID getAgreementTypeId() {
        return agreementTypeId;
    }

    public void setAgreementTypeId(UUID agreementTypeId) {
        this.agreementTypeId = agreementTypeId;
    }

    public AgreementStatus getStatus() {
        return status;
    }

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDataResidencyRegion() {
        return dataResidencyRegion;
    }

    public void setDataResidencyRegion(String dataResidencyRegion) {
        this.dataResidencyRegion = dataResidencyRegion;
    }

    public LocalDate getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(LocalDate inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public BigDecimal getTotalSumInsured() {
        return totalSumInsured;
    }

    public void setTotalSumInsured(BigDecimal totalSumInsured) {
        this.totalSumInsured = totalSumInsured;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
