package com.beema.kernel.api.v1.layout;

import com.beema.kernel.service.layout.LayoutResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/layouts")
@Tag(name = "Layouts", description = "Server-driven UI layout resolution")
public class LayoutController {

    private final LayoutResolutionService layoutResolutionService;

    public LayoutController(LayoutResolutionService layoutResolutionService) {
        this.layoutResolutionService = layoutResolutionService;
    }

    /**
     * Get layout for a specific context and object type
     *
     * @param context - policy, claim, agreement
     * @param objectType - motor_policy, property_claim, etc.
     * @param marketContext - RETAIL, COMMERCIAL, LONDON_MARKET (default: RETAIL)
     * @param tenantId - Tenant identifier (default: default)
     * @param userId - User identifier for security trimming
     * @param userRole - User role for permission filtering (default: user)
     * @param userEmail - User email for security trimming
     * @param dataContext - Optional data context for expression evaluation
     * @return Layout JSON schema
     */
    @GetMapping("/{context}/{objectType}")
    @Operation(
        summary = "Resolve layout by context and object type",
        description = "Returns the best matching layout based on context, object type, market context, tenant, and user role"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Layout resolved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<Map<String, Object>> getLayout(
            @Parameter(description = "Context: policy, claim, agreement", required = true)
            @PathVariable String context,

            @Parameter(description = "Object type: motor_policy, property_claim, etc.", required = true)
            @PathVariable String objectType,

            @Parameter(description = "Market context: RETAIL, COMMERCIAL, LONDON_MARKET")
            @RequestParam(required = false, defaultValue = "RETAIL") String marketContext,

            @Parameter(description = "Tenant identifier for tenant-specific layouts")
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = "default") String tenantId,

            @Parameter(description = "User identifier for security trimming")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "anonymous") String userId,

            @Parameter(description = "User role for permission filtering")
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "user") String userRole,

            @Parameter(description = "User email for security trimming")
            @RequestHeader(value = "X-User-Email", required = false, defaultValue = "user@example.com") String userEmail,

            @Parameter(description = "Optional data context for JEXL expression evaluation")
            @RequestBody(required = false) Map<String, Object> dataContext
    ) {
        Map<String, Object> layout = layoutResolutionService.resolveLayout(
            context,
            objectType,
            marketContext,
            tenantId,
            userId,
            userRole,
            userEmail,
            dataContext
        );

        return ResponseEntity.ok(layout);
    }

    /**
     * Get all available layouts (for admin/debugging)
     *
     * @param context - Optional filter by context (policy, claim, agreement)
     * @return List of layout metadata
     */
    @GetMapping("/all")
    @Operation(
        summary = "List all layouts",
        description = "Returns metadata for all enabled layouts, optionally filtered by context"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Layouts retrieved successfully")
    })
    public ResponseEntity<Map<String, Object>> getAllLayouts(
            @Parameter(description = "Optional filter by context")
            @RequestParam(required = false) String context
    ) {
        List<Map<String, Object>> layouts = layoutResolutionService.getAllLayouts(context);

        return ResponseEntity.ok(Map.of(
            "count", layouts.size(),
            "layouts", layouts
        ));
    }

    /**
     * Health check endpoint for layout service
     *
     * @return Service health status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Layout service health check",
        description = "Returns health status of the layout resolution service"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "layout-resolution",
            "message", "Layout resolution service is operational"
        ));
    }
}
