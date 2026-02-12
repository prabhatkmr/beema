package com.beema.kernel.api.v1.agreement.dto;

import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for creating/updating agreements.
 */
public record AgreementRequest(
    @NotBlank(message = "Agreement number is required")
    String agreementNumber,

    @NotBlank(message = "Agreement type code is required")
    String agreementTypeCode,

    @NotNull(message = "Market context is required")
    MarketContext marketContext,

    AgreementStatus status,

    @NotNull(message = "Attributes are required")
    Map<String, Object> attributes,

    String dataResidencyRegion,

    @NotBlank(message = "Tenant ID is required")
    String tenantId,

    String createdBy,

    String updatedBy
) {
    public AgreementRequest {
        // Default status to DRAFT if not provided
        if (status == null) {
            status = AgreementStatus.DRAFT;
        }
    }
}
