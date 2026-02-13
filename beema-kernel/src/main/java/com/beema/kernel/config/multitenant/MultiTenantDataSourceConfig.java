package com.beema.kernel.config.multitenant;

import com.beema.kernel.service.tenant.TenantContextService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-tenant DataSource configuration.
 *
 * When enabled (beema.multi-datasource.enabled=true):
 * - Creates HikariCP pools for each configured datasource
 * - Configures a TenantRoutingDataSource as the primary DataSource
 * - Runs Flyway migrations on ALL configured databases
 *
 * When disabled (default), Spring Boot auto-configuration handles a single datasource.
 */
@Configuration
@ConditionalOnProperty(name = "beema.multi-datasource.enabled", havingValue = "true")
public class MultiTenantDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantDataSourceConfig.class);

    /**
     * Build individual HikariCP DataSource for a datasource definition.
     */
    private HikariDataSource buildDataSource(String name, TenantDatasourceProperties.DatasourceDefinition def) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("Beema-" + name);
        config.setJdbcUrl(def.getUrl());
        config.setUsername(def.getUsername());
        config.setPassword(def.getPassword());
        config.setDriverClassName(def.getDriverClassName());
        config.setMaximumPoolSize(def.getPoolSize());
        config.setMinimumIdle(def.getMinimumIdle());
        config.setConnectionTimeout(def.getConnectionTimeout());
        config.setIdleTimeout(def.getIdleTimeout());
        config.setMaxLifetime(def.getMaxLifetime());
        config.setConnectionTestQuery("SELECT 1");
        config.setLeakDetectionThreshold(10000);

        log.info("Creating HikariCP pool '{}' → {}", name, def.getUrl());
        return new HikariDataSource(config);
    }

    /**
     * Primary routing DataSource that delegates to tenant-specific pools.
     */
    @Bean
    @Primary
    public DataSource dataSource(
            TenantDatasourceProperties properties,
            TenantContextService tenantContextService,
            TenantDatasourceMappingService mappingService) {

        Map<Object, Object> targetDataSources = new HashMap<>();
        DataSource defaultDataSource = null;

        for (Map.Entry<String, TenantDatasourceProperties.DatasourceDefinition> entry :
                properties.getDatasources().entrySet()) {
            String name = entry.getKey();
            HikariDataSource ds = buildDataSource(name, entry.getValue());
            targetDataSources.put(name, ds);

            if (name.equals(properties.getDefaultDatasource())) {
                defaultDataSource = ds;
            }
        }

        if (defaultDataSource == null && !targetDataSources.isEmpty()) {
            // Fall back to first datasource if default name doesn't match
            defaultDataSource = (DataSource) targetDataSources.values().iterator().next();
            log.warn("Default datasource '{}' not found in config, using first available",
                    properties.getDefaultDatasource());
        }

        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(
                tenantContextService, mappingService);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.afterPropertiesSet();

        log.info("Multi-tenant routing DataSource configured with {} datasources, default: '{}'",
                targetDataSources.size(), properties.getDefaultDatasource());

        return routingDataSource;
    }

    /**
     * Run Flyway migrations on ALL configured datasources.
     * Ensures schema parity across all tenant databases.
     */
    @Bean
    public FlywayMultiDatabaseMigrator flywayMultiDatabaseMigrator(
            TenantDatasourceProperties properties) {
        return new FlywayMultiDatabaseMigrator(properties);
    }

    /**
     * Component that runs Flyway on each configured datasource.
     */
    public static class FlywayMultiDatabaseMigrator {

        private static final Logger log = LoggerFactory.getLogger(FlywayMultiDatabaseMigrator.class);

        public FlywayMultiDatabaseMigrator(TenantDatasourceProperties properties) {
            for (Map.Entry<String, TenantDatasourceProperties.DatasourceDefinition> entry :
                    properties.getDatasources().entrySet()) {
                String name = entry.getKey();
                TenantDatasourceProperties.DatasourceDefinition def = entry.getValue();

                log.info("Running Flyway migration on datasource '{}'", name);
                try {
                    Flyway flyway = Flyway.configure()
                            .dataSource(def.getUrl(), def.getUsername(), def.getPassword())
                            .locations("classpath:db/migration")
                            .schemas("public")
                            .baselineOnMigrate(true)
                            .validateOnMigrate(true)
                            .cleanDisabled(true)
                            .load();
                    flyway.migrate();
                    log.info("Flyway migration completed for datasource '{}'", name);
                } catch (Exception e) {
                    log.error("Flyway migration failed for datasource '{}': {}", name, e.getMessage(), e);
                    throw new RuntimeException("Flyway migration failed for " + name, e);
                }
            }
        }
    }
}
