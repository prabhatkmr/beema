package com.beema.kernel.service.tenant;

import java.util.Optional;

public class TenantContextService {

    private static final ThreadLocal<TenantContext> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantContext(TenantContext context) {
        CURRENT_TENANT.set(context);
    }

    public static Optional<TenantContext> getTenantContext() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static TenantContext requireTenantContext() {
        return getTenantContext()
                .orElseThrow(() -> new IllegalStateException("No tenant context available"));
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
