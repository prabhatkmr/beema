package com.beema.kernel.service.layout;

import com.beema.kernel.domain.layout.Layout;
import com.beema.kernel.repository.layout.LayoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LayoutResolutionService {

    private static final Logger log = LoggerFactory.getLogger(LayoutResolutionService.class);

    private final LayoutRepository layoutRepository;
    private final LayoutSecurityService securityService;

    public LayoutResolutionService(
            LayoutRepository layoutRepository,
            LayoutSecurityService securityService
    ) {
        this.layoutRepository = layoutRepository;
        this.securityService = securityService;
    }

    /**
     * Resolve the best matching layout based on context, user role, and tenant
     *
     * @param context - policy, claim, agreement
     * @param objectType - motor_policy, property_claim, etc.
     * @param marketContext - RETAIL, COMMERCIAL, LONDON_MARKET
     * @param tenantId - Tenant identifier
     * @param userRole - User role for permission filtering
     * @return Layout schema with metadata
     */
    public Map<String, Object> resolveLayout(
            String context,
            String objectType,
            String marketContext,
            String tenantId,
            String userRole
    ) {
        return resolveLayout(context, objectType, marketContext, tenantId, null, userRole, null, null);
    }

    /**
     * Resolve layout with security trimming applied
     */
    public Map<String, Object> resolveLayout(
            String context,
            String objectType,
            String marketContext,
            String tenantId,
            String userId,
            String userRole,
            String userEmail,
            Map<String, Object> dataContext
    ) {
        log.info("Resolving layout: context={}, objectType={}, marketContext={}, tenantId={}, role={}",
            context, objectType, marketContext, tenantId, userRole);

        // Find matching layouts (ordered by priority)
        List<Layout> matchingLayouts = layoutRepository.findMatchingLayouts(
            context,
            objectType,
            marketContext,
            tenantId,
            userRole
        );

        if (matchingLayouts.isEmpty()) {
            log.warn("No layout found for context={}, objectType={}", context, objectType);
            return getDefaultLayout(context, objectType);
        }

        // Return the highest priority layout
        Layout selectedLayout = matchingLayouts.get(0);

        log.info("Selected layout: {} (priority={}, role={}, tenant={})",
            selectedLayout.getLayoutName(),
            selectedLayout.getPriority(),
            selectedLayout.getRole(),
            selectedLayout.getTenantId());

        // Clone the schema to avoid modifying the original
        Map<String, Object> layoutSchema = new HashMap<>(selectedLayout.getLayoutSchema());

        // Apply security trimming if user context is available
        Map<String, Object> trimmedSchema;
        if (userId != null && userEmail != null) {
            LayoutSecurityService.SecurityContext securityContext =
                new LayoutSecurityService.SecurityContext(userId, userRole, userEmail, tenantId);

            trimmedSchema = securityService.applySecurityTrimming(
                layoutSchema,
                securityContext,
                dataContext != null ? dataContext : Map.of()
            );

            // Add metadata
            trimmedSchema.put("_metadata", Map.of(
                "layoutId", selectedLayout.getLayoutId().toString(),
                "layoutName", selectedLayout.getLayoutName(),
                "version", selectedLayout.getVersion(),
                "context", selectedLayout.getContext(),
                "objectType", selectedLayout.getObjectType(),
                "marketContext", selectedLayout.getMarketContext(),
                "securityTrimmed", true
            ));
        } else {
            // No security trimming if user context not provided
            trimmedSchema = layoutSchema;
            trimmedSchema.put("_metadata", Map.of(
                "layoutId", selectedLayout.getLayoutId().toString(),
                "layoutName", selectedLayout.getLayoutName(),
                "version", selectedLayout.getVersion(),
                "context", selectedLayout.getContext(),
                "objectType", selectedLayout.getObjectType(),
                "marketContext", selectedLayout.getMarketContext(),
                "securityTrimmed", false
            ));
        }

        return trimmedSchema;
    }

    /**
     * Get all available layouts for a specific context
     *
     * @param context - policy, claim, agreement (optional)
     * @return List of layout metadata
     */
    public List<Map<String, Object>> getAllLayouts(String context) {
        List<Layout> layouts;

        if (context != null && !context.isBlank()) {
            layouts = layoutRepository.findAllByContext(context);
        } else {
            layouts = layoutRepository.findAllEnabled();
        }

        return layouts.stream()
            .map(layout -> Map.<String, Object>of(
                "layoutId", layout.getLayoutId().toString(),
                "layoutName", layout.getLayoutName(),
                "layoutType", layout.getLayoutType(),
                "context", layout.getContext(),
                "objectType", layout.getObjectType(),
                "marketContext", layout.getMarketContext(),
                "role", layout.getRole() != null ? layout.getRole() : "all",
                "tenant", layout.getTenantId() != null ? layout.getTenantId() : "default",
                "version", layout.getVersion(),
                "priority", layout.getPriority()
            ))
            .toList();
    }

    /**
     * Get default layout when no matching layout is found
     */
    private Map<String, Object> getDefaultLayout(String context, String objectType) {
        return Map.of(
            "title", context + " - " + objectType,
            "sections", List.of(),
            "_metadata", Map.of(
                "default", true,
                "context", context,
                "objectType", objectType,
                "message", "No layout found for this context. Please configure a layout in sys_layouts table."
            )
        );
    }
}
