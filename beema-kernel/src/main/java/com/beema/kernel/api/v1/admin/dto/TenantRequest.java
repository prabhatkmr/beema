package com.beema.kernel.api.v1.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record TenantRequest(
    @NotBlank @Size(max = 100) String tenantId,
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 100) String slug,
    @Size(max = 50) String tier,
    @Size(max = 20) String regionCode,
    @Size(max = 255) String contactEmail,
    Map<String, Object> config,
    String datasourceKey,
    String createdBy
) {}
