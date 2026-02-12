package com.beema.kernel.service.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing tenant context.
 *
 * Uses ThreadLocal to store tenant information for the current request.
 * The TenantFilter automatically sets this at the start of each request
 * and clears it at the end.
 *
 * Usage:
 * <pre>
 * String tenantId = tenantContextService.getCurrentTenantId();
 * String userId = tenantContextService.getCurrentUserId();
 * </pre>
 */
@Service
public class TenantContextService {

    private static final Logger log = LoggerFactory.getLogger(TenantContextService.class);

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    /**
     * Set tenant context for current request.
     *
     * Called by TenantFilter at start of request.
     *
     * @param context Tenant context
     */
    public void setContext(TenantContext context) {
        CONTEXT.set(context);
        log.debug("Set tenant context: {}", context);
    }

    /**
     * Get current tenant context.
     *
     * @return Tenant context or null if not set
     */
    public TenantContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Get current tenant ID.
     *
     * @return Tenant ID or null if not set
     */
    public String getCurrentTenantId() {
        TenantContext context = CONTEXT.get();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * Get current user ID.
     *
     * @return User ID or null if not set
     */
    public String getCurrentUserId() {
        TenantContext context = CONTEXT.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * Get current data residency region.
     *
     * @return Region or null if not set
     */
    public String getCurrentRegion() {
        TenantContext context = CONTEXT.get();
        return context != null ? context.getDataResidencyRegion() : null;
    }

    /**
     * Clear tenant context.
     *
     * Called by TenantFilter at end of request.
     */
    public void clear() {
        TenantContext context = CONTEXT.get();
        if (context != null) {
            log.debug("Clearing tenant context: {}", context);
        }
        CONTEXT.remove();
    }

    /**
     * Check if tenant context is set.
     *
     * @return true if context exists
     */
    public boolean hasContext() {
        return CONTEXT.get() != null;
    }
}
