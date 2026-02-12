package com.beema.kernel.api.v1.agreement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request DTO for point-in-time queries.
 */
public record TemporalQueryRequest(
    @NotNull(message = "Agreement ID is required")
    UUID id,

    @NotNull(message = "Valid time is required")
    OffsetDateTime validTime,

    @NotNull(message = "Transaction time is required")
    OffsetDateTime transactionTime
) {
}
