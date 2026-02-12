package com.beema.kernel.config;

import com.beema.kernel.service.tenant.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logging configuration with MDC (Mapped Diagnostic Context).
 *
 * Adds to every log statement:
 * - requestId: Unique request identifier
 * - tenantId: Current tenant
 * - userId: Current user
 *
 * Example log output:
 * [requestId=abc-123 tenantId=tenant-456 userId=user-789] Creating agreement...
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // After TenantFilter
public class LoggingConfig extends OncePerRequestFilter {

    private final TenantContextService tenantContextService;

    public LoggingConfig(TenantContextService tenantContextService) {
        this.tenantContextService = tenantContextService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Generate unique request ID
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("requestId", requestId);

            // Add tenant context to MDC
            if (tenantContextService.hasContext()) {
                MDC.put("tenantId", tenantContextService.getCurrentTenantId());
                MDC.put("userId", tenantContextService.getCurrentUserId());
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Always clear MDC after request
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs");
    }
}
