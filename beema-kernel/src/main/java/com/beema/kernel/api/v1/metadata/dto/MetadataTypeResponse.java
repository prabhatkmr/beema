package com.beema.kernel.api.v1.metadata.dto;

import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for agreement types.
 */
public record MetadataTypeResponse(
    UUID id,
    String typeCode,
    MarketContext marketContext,
    Integer schemaVersion,
    String displayName,
    String description,
    Map<String, Object> attributeSchema,
    Map<String, Object> validationRules,
    Boolean isActive,
    String createdBy,
    String updatedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long version
) {
    /**
     * Create response from entity.
     */
    public static MetadataTypeResponse from(MetadataAgreementType entity) {
        return new MetadataTypeResponse(
            entity.getId(),
            entity.getTypeCode(),
            entity.getMarketContext(),
            entity.getSchemaVersion(),
            entity.getDisplayName(),
            entity.getDescription(),
            entity.getAttributeSchema(),
            entity.getValidationRules(),
            entity.getIsActive(),
            entity.getCreatedBy(),
            entity.getUpdatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }
}
