package com.beema.kernel.api.v1.admin.dto;

import com.beema.kernel.domain.admin.Region;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RegionResponse(
    UUID id,
    String code,
    String name,
    String description,
    Map<String, Object> dataResidencyRules,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(
            region.getId(),
            region.getCode(),
            region.getName(),
            region.getDescription(),
            region.getDataResidencyRules(),
            region.getIsActive(),
            region.getCreatedAt(),
            region.getUpdatedAt()
        );
    }
}
