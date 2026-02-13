package com.beema.kernel.config.multitenant;

import com.beema.kernel.service.tenant.TenantContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routing DataSource that selects the target database based on the
 * current tenant context.
 *
 * Flow:
 * 1. Request arrives → TenantFilter extracts tenant ID → sets TenantContext
 * 2. JPA/JDBC needs a connection → calls determineCurrentLookupKey()
 * 3. TenantContextService provides tenant ID
 * 4. TenantDatasourceMappingService resolves tenant → datasource key
 * 5. AbstractRoutingDataSource returns the matching DataSource
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantRoutingDataSource.class);

    private final TenantContextService tenantContextService;
    private final TenantDatasourceMappingService mappingService;

    public TenantRoutingDataSource(
            TenantContextService tenantContextService,
            TenantDatasourceMappingService mappingService) {
        this.tenantContextService = tenantContextService;
        this.mappingService = mappingService;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = tenantContextService.getCurrentTenantId();
        String datasourceKey = mappingService.resolveDatasource(tenantId);
        log.trace("Routing tenant '{}' → datasource '{}'", tenantId, datasourceKey);
        return datasourceKey;
    }
}
