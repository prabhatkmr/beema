package com.beema.kernel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for multi-tenancy.
 *
 * Maps properties from application.yml:
 * <pre>
 * beema:
 *   kernel:
 *     multi-tenancy:
 *       enabled: true
 *       default-tenant: default
 *       tenant-header: X-Tenant-ID
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "beema.kernel.multi-tenancy")
public class TenantConfig {

    private boolean enabled = true;
    private String defaultTenant = "default";
    private String tenantHeader = "X-Tenant-ID";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultTenant() {
        return defaultTenant;
    }

    public void setDefaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    public String getTenantHeader() {
        return tenantHeader;
    }

    public void setTenantHeader(String tenantHeader) {
        this.tenantHeader = tenantHeader;
    }
}
