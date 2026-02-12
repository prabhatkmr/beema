package com.beema.kernel.api.v1.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Schema(description = "Temporal query parameters for bitemporal queries")
public class TemporalQueryParams {

    @Schema(description = "Point-in-time for 'as-of' business validity queries", example = "2026-01-15T00:00:00Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime asOf;

    @Schema(description = "Start of business validity range", example = "2026-01-01T00:00:00Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime validFrom;

    @Schema(description = "End of business validity range", example = "2026-12-31T23:59:59Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime validTo;

    public OffsetDateTime getAsOf() {
        return asOf;
    }

    public void setAsOf(OffsetDateTime asOf) {
        this.asOf = asOf;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }
}
