package com.beema.kernel.config;

import com.beema.kernel.service.tenant.TenantContext;
import com.beema.kernel.service.tenant.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@Component
@Order(1)
public class TenantConfig extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantConfig.class);

    private final DataSource dataSource;

    public TenantConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContext tenantContext = resolveTenantContext(request);
            if (tenantContext != null) {
                TenantContextService.setTenantContext(tenantContext);
                MDC.put("tenantId", tenantContext.tenantId().toString());
                setPostgresSessionVariable(tenantContext.tenantId());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextService.clear();
            MDC.remove("tenantId");
        }
    }

    private TenantContext resolveTenantContext(HttpServletRequest request) {
        // Try JWT claims first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantIdClaim = jwt.getClaimAsString(TenantContext.JWT_CLAIM_TENANT_ID);
            if (tenantIdClaim != null) {
                return new TenantContext(
                        UUID.fromString(tenantIdClaim),
                        jwt.getClaimAsString(TenantContext.JWT_CLAIM_TENANT_NAME),
                        jwt.getClaimAsString(TenantContext.JWT_CLAIM_MARKET_CONTEXT)
                );
            }
        }

        // Fall back to header
        String headerTenantId = request.getHeader(TenantContext.HEADER_TENANT_ID);
        if (headerTenantId != null && !headerTenantId.isBlank()) {
            return new TenantContext(UUID.fromString(headerTenantId), null, null);
        }

        return null;
    }

    private void setPostgresSessionVariable(UUID tenantId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SET LOCAL app.current_tenant = ?")) {
            stmt.setString(1, tenantId.toString());
            stmt.execute();
        } catch (SQLException e) {
            log.warn("Failed to set PostgreSQL session variable for tenant {}: {}",
                    tenantId, e.getMessage());
        }
    }
}
