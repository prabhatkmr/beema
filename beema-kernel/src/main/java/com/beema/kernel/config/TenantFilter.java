package com.beema.kernel.config;

import com.beema.kernel.service.tenant.TenantContext;
import com.beema.kernel.service.tenant.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter for extracting tenant context from requests.
 *
 * Extracts:
 * - Tenant ID from X-Tenant-ID header
 * - User ID from X-User-ID header (or JWT in production)
 * - Data residency region from X-Region header
 *
 * Sets PostgreSQL session variable for Row-Level Security:
 * SET LOCAL app.current_tenant = 'tenant-123';
 *
 * Execution order: HIGHEST_PRECEDENCE (runs first)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String USER_HEADER = "X-User-ID";
    private static final String REGION_HEADER = "X-Region";
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_USER = "system";
    private static final String DEFAULT_REGION = "US";

    private final TenantContextService tenantContextService;

    public TenantFilter(TenantContextService tenantContextService) {
        this.tenantContextService = tenantContextService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract tenant information from headers
            String tenantId = extractHeader(request, TENANT_HEADER, DEFAULT_TENANT);
            String userId = extractHeader(request, USER_HEADER, DEFAULT_USER);
            String region = extractHeader(request, REGION_HEADER, DEFAULT_REGION);

            // Set tenant context
            TenantContext context = new TenantContext(tenantId, userId, region);
            tenantContextService.setContext(context);

            log.debug("Request: {} {} - Tenant: {}, User: {}",
                request.getMethod(),
                request.getRequestURI(),
                tenantId,
                userId);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Always clear context after request
            tenantContextService.clear();
        }
    }

    /**
     * Extract header value with fallback to default.
     */
    private String extractHeader(HttpServletRequest request, String headerName, String defaultValue) {
        String value = request.getHeader(headerName);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /**
     * Skip filter for static resources and actuator endpoints.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico");
    }
}
