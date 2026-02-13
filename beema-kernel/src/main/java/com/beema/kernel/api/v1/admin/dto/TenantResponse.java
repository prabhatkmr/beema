package com.beema.kernel.api.v1.admin.dto;

import com.beema.kernel.domain.admin.Tenant;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    String tenantId,
    String name,
    String slug,
    String status,
    String tier,
    String regionCode,
    String contactEmail,
    Map<String, Object> config,
    String datasourceKey,
    String createdBy,
    String updatedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getTenantId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getStatus(),
            tenant.getTier(),
            tenant.getRegionCode(),
            tenant.getContactEmail(),
            tenant.getConfig(),
            tenant.getDatasourceKey(),
            tenant.getCreatedBy(),
            tenant.getUpdatedBy(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }
}
