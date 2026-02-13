package com.beema.kernel.config.multitenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for multi-tenant datasource routing.
 *
 * Example YAML:
 * <pre>
 * beema:
 *   multi-datasource:
 *     enabled: true
 *     default-datasource: master
 *     datasources:
 *       master:
 *         url: jdbc:postgresql://localhost:5432/beema_dev
 *         username: beema_admin
 *         password: changeme
 *         pool-size: 20
 *       tenant-vip-1:
 *         url: jdbc:postgresql://vip-db-1:5432/beema_vip1
 *         username: beema_vip
 *         password: secret123
 *         pool-size: 10
 *     tenant-mappings:
 *       vip-tenant-001: tenant-vip-1
 *       vip-tenant-002: tenant-vip-1
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "beema.multi-datasource")
public class TenantDatasourceProperties {

    private boolean enabled = false;
    private String defaultDatasource = "master";
    private Map<String, DatasourceDefinition> datasources = new HashMap<>();
    private Map<String, String> tenantMappings = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultDatasource() {
        return defaultDatasource;
    }

    public void setDefaultDatasource(String defaultDatasource) {
        this.defaultDatasource = defaultDatasource;
    }

    public Map<String, DatasourceDefinition> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DatasourceDefinition> datasources) {
        this.datasources = datasources;
    }

    public Map<String, String> getTenantMappings() {
        return tenantMappings;
    }

    public void setTenantMappings(Map<String, String> tenantMappings) {
        this.tenantMappings = tenantMappings;
    }

    public static class DatasourceDefinition {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
        private int poolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }
    }
}
