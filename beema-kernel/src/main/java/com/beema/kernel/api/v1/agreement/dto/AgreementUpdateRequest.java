package com.beema.kernel.api.v1.agreement.dto;

import com.beema.kernel.domain.agreement.AgreementStatus;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Request DTO for updating agreements (temporal versioning).
 */
public record AgreementUpdateRequest(
    AgreementStatus status,

    Map<String, Object> attributes,

    @NotNull(message = "Effective from date is required")
    OffsetDateTime effectiveFrom,

    @NotNull(message = "Updated by is required")
    String updatedBy
) {
}
