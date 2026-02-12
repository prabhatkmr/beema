package com.beema.kernel.service.tenant;

/**
 * Tenant context data.
 *
 * Stored in ThreadLocal for request-scoped access.
 */
public class TenantContext {

    private final String tenantId;
    private final String userId;
    private final String dataResidencyRegion;

    public TenantContext(String tenantId, String userId, String dataResidencyRegion) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.dataResidencyRegion = dataResidencyRegion;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDataResidencyRegion() {
        return dataResidencyRegion;
    }

    @Override
    public String toString() {
        return "TenantContext{" +
               "tenantId='" + tenantId + '\'' +
               ", userId='" + userId + '\'' +
               ", region='" + dataResidencyRegion + '\'' +
               '}';
    }
}
