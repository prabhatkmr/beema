package com.beema.kernel.api.v1.admin;

import com.beema.kernel.api.v1.admin.dto.*;
import com.beema.kernel.domain.admin.Datasource;
import com.beema.kernel.domain.admin.Region;
import com.beema.kernel.domain.admin.Tenant;
import com.beema.kernel.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "Global platform administration (tenants, regions, datasources)")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // =========================================================================
    // Dashboard
    // =========================================================================

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Dashboard stats", description = "Get system overview statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // =========================================================================
    // Tenants
    // =========================================================================

    @PostMapping("/tenants")
    @Operation(summary = "Create tenant", description = "Register a new tenant")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantRequest request) {
        log.info("Creating tenant: {}", request.tenantId());

        Tenant tenant = new Tenant();
        tenant.setTenantId(request.tenantId());
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        if (request.tier() != null) tenant.setTier(request.tier());
        if (request.regionCode() != null) tenant.setRegionCode(request.regionCode());
        if (request.contactEmail() != null) tenant.setContactEmail(request.contactEmail());
        if (request.config() != null) tenant.setConfig(request.config());
        if (request.datasourceKey() != null) tenant.setDatasourceKey(request.datasourceKey());
        tenant.setCreatedBy(request.createdBy() != null ? request.createdBy() : "admin");
        tenant.setUpdatedBy(request.createdBy() != null ? request.createdBy() : "admin");

        Tenant created = adminService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(created));
    }

    @GetMapping("/tenants")
    @Operation(summary = "List tenants", description = "List all tenants with optional filters")
    public ResponseEntity<List<TenantResponse>> listTenants(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region
    ) {
        List<TenantResponse> tenants = adminService.listTenants(status, region).stream()
                .map(TenantResponse::from)
                .toList();
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/tenants/{id}")
    @Operation(summary = "Get tenant", description = "Get tenant by ID")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(TenantResponse.from(adminService.getTenant(id)));
    }

    @PutMapping("/tenants/{id}")
    @Operation(summary = "Update tenant", description = "Update tenant details")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request
    ) {
        log.info("Updating tenant: {}", id);

        Tenant updates = new Tenant();
        updates.setName(request.name());
        if (request.tier() != null) updates.setTier(request.tier());
        if (request.regionCode() != null) updates.setRegionCode(request.regionCode());
        if (request.contactEmail() != null) updates.setContactEmail(request.contactEmail());
        if (request.config() != null) updates.setConfig(request.config());
        if (request.datasourceKey() != null) updates.setDatasourceKey(request.datasourceKey());
        updates.setUpdatedBy(request.createdBy() != null ? request.createdBy() : "admin");

        Tenant updated = adminService.updateTenant(id, updates);
        return ResponseEntity.ok(TenantResponse.from(updated));
    }

    @PostMapping("/tenants/{id}/activate")
    @Operation(summary = "Activate tenant", description = "Set tenant status to ACTIVE")
    public ResponseEntity<TenantResponse> activateTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(TenantResponse.from(adminService.activateTenant(id)));
    }

    @PostMapping("/tenants/{id}/suspend")
    @Operation(summary = "Suspend tenant", description = "Set tenant status to SUSPENDED")
    public ResponseEntity<TenantResponse> suspendTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(TenantResponse.from(adminService.suspendTenant(id)));
    }

    @PostMapping("/tenants/{id}/deactivate")
    @Operation(summary = "Deactivate tenant", description = "Set tenant status to DEACTIVATED")
    public ResponseEntity<TenantResponse> deactivateTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(TenantResponse.from(adminService.deactivateTenant(id)));
    }

    // =========================================================================
    // Regions
    // =========================================================================

    @PostMapping("/regions")
    @Operation(summary = "Create region", description = "Register a new region")
    public ResponseEntity<RegionResponse> createRegion(@Valid @RequestBody RegionRequest request) {
        log.info("Creating region: {}", request.code());

        Region region = new Region();
        region.setCode(request.code());
        region.setName(request.name());
        if (request.description() != null) region.setDescription(request.description());
        if (request.dataResidencyRules() != null) region.setDataResidencyRules(request.dataResidencyRules());
        if (request.isActive() != null) region.setIsActive(request.isActive());

        Region created = adminService.createRegion(region);
        return ResponseEntity.status(HttpStatus.CREATED).body(RegionResponse.from(created));
    }

    @GetMapping("/regions")
    @Operation(summary = "List regions", description = "List all regions")
    public ResponseEntity<List<RegionResponse>> listRegions() {
        List<RegionResponse> regions = adminService.listRegions().stream()
                .map(RegionResponse::from)
                .toList();
        return ResponseEntity.ok(regions);
    }

    @PutMapping("/regions/{id}")
    @Operation(summary = "Update region", description = "Update region details")
    public ResponseEntity<RegionResponse> updateRegion(
            @PathVariable UUID id,
            @Valid @RequestBody RegionRequest request
    ) {
        log.info("Updating region: {}", id);

        Region updates = new Region();
        if (request.name() != null) updates.setName(request.name());
        if (request.description() != null) updates.setDescription(request.description());
        if (request.dataResidencyRules() != null) updates.setDataResidencyRules(request.dataResidencyRules());
        if (request.isActive() != null) updates.setIsActive(request.isActive());

        Region updated = adminService.updateRegion(id, updates);
        return ResponseEntity.ok(RegionResponse.from(updated));
    }

    // =========================================================================
    // Datasources
    // =========================================================================

    @PostMapping("/datasources")
    @Operation(summary = "Create datasource", description = "Register a new datasource connection")
    public ResponseEntity<DatasourceResponse> createDatasource(@Valid @RequestBody DatasourceRequest request) {
        log.info("Creating datasource: {}", request.name());

        Datasource ds = new Datasource();
        ds.setName(request.name());
        ds.setUrl(request.url());
        ds.setUsername(request.username());
        if (request.poolSize() != null) ds.setPoolSize(request.poolSize());
        if (request.status() != null) ds.setStatus(request.status());
        if (request.config() != null) ds.setConfig(request.config());

        Datasource created = adminService.createDatasource(ds);
        return ResponseEntity.status(HttpStatus.CREATED).body(DatasourceResponse.from(created));
    }

    @GetMapping("/datasources")
    @Operation(summary = "List datasources", description = "List all registered datasources")
    public ResponseEntity<List<DatasourceResponse>> listDatasources() {
        List<DatasourceResponse> datasources = adminService.listDatasources().stream()
                .map(DatasourceResponse::from)
                .toList();
        return ResponseEntity.ok(datasources);
    }

    @PutMapping("/datasources/{id}")
    @Operation(summary = "Update datasource", description = "Update datasource configuration")
    public ResponseEntity<DatasourceResponse> updateDatasource(
            @PathVariable UUID id,
            @Valid @RequestBody DatasourceRequest request
    ) {
        log.info("Updating datasource: {}", id);

        Datasource updates = new Datasource();
        if (request.url() != null) updates.setUrl(request.url());
        if (request.username() != null) updates.setUsername(request.username());
        if (request.poolSize() != null) updates.setPoolSize(request.poolSize());
        if (request.status() != null) updates.setStatus(request.status());
        if (request.config() != null) updates.setConfig(request.config());

        Datasource updated = adminService.updateDatasource(id, updates);
        return ResponseEntity.ok(DatasourceResponse.from(updated));
    }
}
