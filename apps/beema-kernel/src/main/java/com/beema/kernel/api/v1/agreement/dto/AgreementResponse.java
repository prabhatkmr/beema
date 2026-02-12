package com.beema.kernel.api.v1.agreement.dto;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Response body for an agreement")
public class AgreementResponse {

    @Schema(description = "Agreement ID")
    private UUID id;

    @Schema(description = "Agreement number")
    private String agreementNumber;

    @Schema(description = "External system reference")
    private String externalReference;

    @Schema(description = "Market context")
    private MarketContext marketContext;

    @Schema(description = "Agreement type ID")
    private UUID agreementTypeId;

    @Schema(description = "Current status")
    private AgreementStatus status;

    @Schema(description = "Tenant identifier")
    private String tenantId;

    @Schema(description = "Data residency region")
    private String dataResidencyRegion;

    @Schema(description = "Policy inception date")
    private LocalDate inceptionDate;

    @Schema(description = "Policy expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Currency code")
    private String currencyCode;

    @Schema(description = "Total premium")
    private BigDecimal totalPremium;

    @Schema(description = "Total sum insured")
    private BigDecimal totalSumInsured;

    @Schema(description = "Flexible JSONB attributes")
    private Map<String, Object> attributes;

    @Schema(description = "Business validity start")
    private OffsetDateTime validFrom;

    @Schema(description = "Business validity end")
    private OffsetDateTime validTo;

    @Schema(description = "System transaction time")
    private OffsetDateTime transactionTime;

    @Schema(description = "Whether this is the current version")
    private Boolean isCurrent;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated by")
    private String updatedBy;

    public static AgreementResponse fromEntity(Agreement entity) {
        AgreementResponse response = new AgreementResponse();
        response.setId(entity.getTemporalKey().getId());
        response.setAgreementNumber(entity.getAgreementNumber());
        response.setExternalReference(entity.getExternalReference());
        response.setMarketContext(entity.getMarketContext());
        response.setAgreementTypeId(entity.getAgreementTypeId());
        response.setStatus(entity.getStatus());
        response.setTenantId(entity.getTenantId().toString());
        response.setDataResidencyRegion(entity.getDataResidencyRegion());
        response.setInceptionDate(entity.getInceptionDate());
        response.setExpiryDate(entity.getExpiryDate());
        response.setCurrencyCode(entity.getCurrencyCode());
        response.setTotalPremium(entity.getTotalPremium());
        response.setTotalSumInsured(entity.getTotalSumInsured());
        response.setAttributes(entity.getAttributes());
        response.setValidFrom(entity.getTemporalKey().getValidFrom());
        response.setValidTo(entity.getValidTo());
        response.setTransactionTime(entity.getTemporalKey().getTransactionTime());
        response.setIsCurrent(entity.getIsCurrent());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedBy(entity.getUpdatedBy());
        return response;
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAgreementNumber() { return agreementNumber; }
    public void setAgreementNumber(String agreementNumber) { this.agreementNumber = agreementNumber; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public MarketContext getMarketContext() { return marketContext; }
    public void setMarketContext(MarketContext marketContext) { this.marketContext = marketContext; }

    public UUID getAgreementTypeId() { return agreementTypeId; }
    public void setAgreementTypeId(UUID agreementTypeId) { this.agreementTypeId = agreementTypeId; }

    public AgreementStatus getStatus() { return status; }
    public void setStatus(AgreementStatus status) { this.status = status; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDataResidencyRegion() { return dataResidencyRegion; }
    public void setDataResidencyRegion(String dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; }

    public LocalDate getInceptionDate() { return inceptionDate; }
    public void setInceptionDate(LocalDate inceptionDate) { this.inceptionDate = inceptionDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getTotalPremium() { return totalPremium; }
    public void setTotalPremium(BigDecimal totalPremium) { this.totalPremium = totalPremium; }

    public BigDecimal getTotalSumInsured() { return totalSumInsured; }
    public void setTotalSumInsured(BigDecimal totalSumInsured) { this.totalSumInsured = totalSumInsured; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public OffsetDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(OffsetDateTime validFrom) { this.validFrom = validFrom; }

    public OffsetDateTime getValidTo() { return validTo; }
    public void setValidTo(OffsetDateTime validTo) { this.validTo = validTo; }

    public OffsetDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(OffsetDateTime transactionTime) { this.transactionTime = transactionTime; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
