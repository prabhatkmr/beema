package com.beema.kernel.api.v1.agreement.dto;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for agreements.
 */
public record AgreementResponse(
    UUID id,
    String agreementNumber,
    String agreementTypeCode,
    MarketContext marketContext,
    AgreementStatus status,
    Map<String, Object> attributes,
    String dataResidencyRegion,
    String tenantId,

    // Temporal fields
    OffsetDateTime validFrom,
    OffsetDateTime validTo,
    OffsetDateTime transactionTime,
    Boolean isCurrent,

    // Audit fields
    String createdBy,
    String updatedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long version
) {
    /**
     * Create response from entity.
     */
    public static AgreementResponse from(Agreement entity) {
        return new AgreementResponse(
            entity.getId(),
            entity.getAgreementNumber(),
            entity.getAgreementTypeCode(),
            entity.getMarketContext(),
            entity.getStatus(),
            entity.getAttributes(),
            entity.getDataResidencyRegion(),
            entity.getTenantId(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getTransactionTime(),
            entity.getIsCurrent(),
            entity.getCreatedBy(),
            entity.getUpdatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    /**
     * Create summary response (without attributes for lists).
     */
    public static AgreementSummaryResponse toSummary(Agreement entity) {
        return new AgreementSummaryResponse(
            entity.getId(),
            entity.getAgreementNumber(),
            entity.getAgreementTypeCode(),
            entity.getMarketContext(),
            entity.getStatus(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getIsCurrent()
        );
    }
}
