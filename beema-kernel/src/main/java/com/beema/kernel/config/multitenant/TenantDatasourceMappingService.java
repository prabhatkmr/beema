package com.beema.kernel.config.multitenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps tenant IDs to datasource keys.
 *
 * Provides an in-memory cache of tenant → datasource mappings
 * loaded from configuration. Supports dynamic updates for
 * zero-downtime tenant migration.
 */
@Service
public class TenantDatasourceMappingService {

    private static final Logger log = LoggerFactory.getLogger(TenantDatasourceMappingService.class);

    private final ConcurrentHashMap<String, String> tenantToDatasource = new ConcurrentHashMap<>();
    private final String defaultDatasource;

    public TenantDatasourceMappingService(TenantDatasourceProperties properties) {
        this.defaultDatasource = properties.getDefaultDatasource();

        // Load initial mappings from config
        if (properties.getTenantMappings() != null) {
            properties.getTenantMappings().forEach((tenantId, dsKey) -> {
                tenantToDatasource.put(tenantId, dsKey);
                log.info("Tenant '{}' → datasource '{}'", tenantId, dsKey);
            });
        }

        log.info("Tenant datasource mapping initialized with {} mappings, default: '{}'",
                tenantToDatasource.size(), defaultDatasource);
    }

    /**
     * Resolve datasource key for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return the datasource key, or the default datasource if unmapped
     */
    public String resolveDatasource(String tenantId) {
        if (tenantId == null) {
            return defaultDatasource;
        }
        return tenantToDatasource.getOrDefault(tenantId, defaultDatasource);
    }

    /**
     * Add or update a tenant → datasource mapping at runtime.
     * Enables zero-downtime tenant migration.
     *
     * @param tenantId the tenant identifier
     * @param datasourceKey the target datasource key
     */
    public void addMapping(String tenantId, String datasourceKey) {
        String previous = tenantToDatasource.put(tenantId, datasourceKey);
        if (previous != null) {
            log.info("Updated tenant mapping: '{}' → '{}' (was '{}')", tenantId, datasourceKey, previous);
        } else {
            log.info("Added tenant mapping: '{}' → '{}'", tenantId, datasourceKey);
        }
    }

    /**
     * Remove a tenant mapping. Tenant will fall back to default datasource.
     *
     * @param tenantId the tenant identifier
     */
    public void removeMapping(String tenantId) {
        String removed = tenantToDatasource.remove(tenantId);
        if (removed != null) {
            log.info("Removed tenant mapping: '{}' (was '{}')", tenantId, removed);
        }
    }

    /**
     * Get a snapshot of all current tenant mappings.
     */
    public Map<String, String> getAllMappings() {
        return Map.copyOf(tenantToDatasource);
    }

    /**
     * Get the default datasource key.
     */
    public String getDefaultDatasource() {
        return defaultDatasource;
    }
}
