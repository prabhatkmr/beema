package com.beema.kernel.api.v1.agreement.dto;

import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary response for agreement lists.
 *
 * Excludes attributes to reduce payload size.
 */
public record AgreementSummaryResponse(
    UUID id,
    String agreementNumber,
    String agreementTypeCode,
    MarketContext marketContext,
    AgreementStatus status,
    OffsetDateTime validFrom,
    OffsetDateTime validTo,
    Boolean isCurrent
) {
}
