package com.beema.kernel.service.admin;

import com.beema.kernel.domain.admin.Datasource;
import com.beema.kernel.domain.admin.Region;
import com.beema.kernel.domain.admin.Tenant;
import com.beema.kernel.repository.admin.DatasourceRepository;
import com.beema.kernel.repository.admin.RegionRepository;
import com.beema.kernel.repository.admin.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final TenantRepository tenantRepository;
    private final RegionRepository regionRepository;
    private final DatasourceRepository datasourceRepository;

    public AdminService(TenantRepository tenantRepository,
                        RegionRepository regionRepository,
                        DatasourceRepository datasourceRepository) {
        this.tenantRepository = tenantRepository;
        this.regionRepository = regionRepository;
        this.datasourceRepository = datasourceRepository;
    }

    // =========================================================================
    // Tenant Operations
    // =========================================================================

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.info("Creating tenant: {}", tenant.getTenantId());

        if (tenantRepository.existsByTenantId(tenant.getTenantId())) {
            throw new IllegalArgumentException("Tenant ID already exists: " + tenant.getTenantId());
        }
        if (tenantRepository.existsBySlug(tenant.getSlug())) {
            throw new IllegalArgumentException("Tenant slug already exists: " + tenant.getSlug());
        }

        // Validate region exists
        regionRepository.findByCode(tenant.getRegionCode())
                .orElseThrow(() -> new IllegalArgumentException("Region not found: " + tenant.getRegionCode()));

        tenant.setStatus("PROVISIONING");
        Tenant created = tenantRepository.save(tenant);

        // Auto-activate after provisioning
        created.setStatus("ACTIVE");
        return tenantRepository.save(created);
    }

    @Transactional
    public Tenant updateTenant(UUID id, Tenant updates) {
        log.info("Updating tenant: {}", id);

        Tenant existing = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getTier() != null) existing.setTier(updates.getTier());
        if (updates.getRegionCode() != null) {
            regionRepository.findByCode(updates.getRegionCode())
                    .orElseThrow(() -> new IllegalArgumentException("Region not found: " + updates.getRegionCode()));
            existing.setRegionCode(updates.getRegionCode());
        }
        if (updates.getContactEmail() != null) existing.setContactEmail(updates.getContactEmail());
        if (updates.getConfig() != null && !updates.getConfig().isEmpty()) existing.setConfig(updates.getConfig());
        if (updates.getDatasourceKey() != null) existing.setDatasourceKey(updates.getDatasourceKey());
        if (updates.getUpdatedBy() != null) existing.setUpdatedBy(updates.getUpdatedBy());

        return tenantRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants(String status, String regionCode) {
        if (status != null && regionCode != null) {
            return tenantRepository.findByStatusAndRegionCode(status, regionCode);
        } else if (status != null) {
            return tenantRepository.findByStatus(status);
        } else if (regionCode != null) {
            return tenantRepository.findByRegionCode(regionCode);
        }
        return tenantRepository.findAll();
    }

    @Transactional
    public Tenant activateTenant(UUID id) {
        log.info("Activating tenant: {}", id);
        Tenant tenant = getTenant(id);
        tenant.setStatus("ACTIVE");
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant suspendTenant(UUID id) {
        log.info("Suspending tenant: {}", id);
        Tenant tenant = getTenant(id);
        tenant.setStatus("SUSPENDED");
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant deactivateTenant(UUID id) {
        log.info("Deactivating tenant: {}", id);
        Tenant tenant = getTenant(id);
        tenant.setStatus("DEACTIVATED");
        return tenantRepository.save(tenant);
    }

    // =========================================================================
    // Region Operations
    // =========================================================================

    @Transactional
    public Region createRegion(Region region) {
        log.info("Creating region: {}", region.getCode());

        if (regionRepository.existsByCode(region.getCode())) {
            throw new IllegalArgumentException("Region code already exists: " + region.getCode());
        }

        return regionRepository.save(region);
    }

    @Transactional
    public Region updateRegion(UUID id, Region updates) {
        log.info("Updating region: {}", id);

        Region existing = regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getDataResidencyRules() != null && !updates.getDataResidencyRules().isEmpty()) {
            existing.setDataResidencyRules(updates.getDataResidencyRules());
        }
        if (updates.getIsActive() != null) existing.setIsActive(updates.getIsActive());

        return regionRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Region> listRegions() {
        return regionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Region getRegion(UUID id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Region not found: " + id));
    }

    // =========================================================================
    // Datasource Operations
    // =========================================================================

    @Transactional
    public Datasource createDatasource(Datasource datasource) {
        log.info("Creating datasource: {}", datasource.getName());

        if (datasourceRepository.existsByName(datasource.getName())) {
            throw new IllegalArgumentException("Datasource name already exists: " + datasource.getName());
        }

        return datasourceRepository.save(datasource);
    }

    @Transactional
    public Datasource updateDatasource(UUID id, Datasource updates) {
        log.info("Updating datasource: {}", id);

        Datasource existing = datasourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + id));

        if (updates.getUrl() != null) existing.setUrl(updates.getUrl());
        if (updates.getUsername() != null) existing.setUsername(updates.getUsername());
        if (updates.getPoolSize() != null) existing.setPoolSize(updates.getPoolSize());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
        if (updates.getConfig() != null && !updates.getConfig().isEmpty()) existing.setConfig(updates.getConfig());

        return datasourceRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Datasource> listDatasources() {
        return datasourceRepository.findAll();
    }

    // =========================================================================
    // Dashboard Stats
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.findByStatus("ACTIVE").size();
        long totalRegions = regionRepository.count();
        long activeRegions = regionRepository.findByIsActive(true).size();
        long totalDatasources = datasourceRepository.count();

        stats.put("totalTenants", totalTenants);
        stats.put("activeTenants", activeTenants);
        stats.put("suspendedTenants", tenantRepository.findByStatus("SUSPENDED").size());
        stats.put("totalRegions", totalRegions);
        stats.put("activeRegions", activeRegions);
        stats.put("totalDatasources", totalDatasources);

        // Tier breakdown
        Map<String, Integer> tierBreakdown = new HashMap<>();
        tierBreakdown.put("STANDARD", tenantRepository.findByTier("STANDARD").size());
        tierBreakdown.put("PREMIUM", tenantRepository.findByTier("PREMIUM").size());
        tierBreakdown.put("ENTERPRISE", tenantRepository.findByTier("ENTERPRISE").size());
        stats.put("tierBreakdown", tierBreakdown);

        return stats;
    }
}
