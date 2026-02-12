package com.beema.kernel.service.tenant;

import java.util.UUID;

public record TenantContext(
        UUID tenantId,
        String tenantName,
        String marketContext
) {
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String JWT_CLAIM_TENANT_ID = "tenant_id";
    public static final String JWT_CLAIM_TENANT_NAME = "tenant_name";
    public static final String JWT_CLAIM_MARKET_CONTEXT = "market_context";
}
