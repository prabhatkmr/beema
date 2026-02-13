-- V8: Admin Management Tables
--
-- Global administration tables for tenant, region, and datasource management.
-- These are system-level tables (sys_ prefix) not subject to tenant RLS.

-- ============================================================================
-- SYS_TENANTS: Tenant registry
-- ============================================================================

CREATE TABLE sys_tenants (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tier VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    region_code VARCHAR(20) NOT NULL DEFAULT 'US',
    contact_email VARCHAR(255),

    -- Flexible tenant configuration (JSONB)
    -- Example:
    -- {
    --   "maxUsers": 100,
    --   "features": ["batch_export", "streaming"],
    --   "marketContexts": ["RETAIL", "COMMERCIAL"],
    --   "branding": { "primaryColor": "#1E40AF" }
    -- }
    config JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Optional datasource routing key (references sys_datasources.name)
    datasource_key VARCHAR(100),

    -- Audit fields
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    PRIMARY KEY (id),

    CONSTRAINT uq_tenant_tenant_id UNIQUE (tenant_id),
    CONSTRAINT uq_tenant_slug UNIQUE (slug),

    CONSTRAINT chk_tenant_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PROVISIONING', 'DEACTIVATED')),

    CONSTRAINT chk_tenant_tier
        CHECK (tier IN ('STANDARD', 'PREMIUM', 'ENTERPRISE'))
);

-- Indexes
CREATE INDEX idx_sys_tenants_status ON sys_tenants (status);
CREATE INDEX idx_sys_tenants_region ON sys_tenants (region_code);
CREATE INDEX idx_sys_tenants_tier ON sys_tenants (tier);
CREATE INDEX idx_sys_tenants_config_gin ON sys_tenants USING GIN (config);

-- Triggers
CREATE TRIGGER audit_sys_tenants
    BEFORE UPDATE ON sys_tenants
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE sys_tenants IS 'Global tenant registry for platform administration';
COMMENT ON COLUMN sys_tenants.tenant_id IS 'Business key matching X-Tenant-ID header';
COMMENT ON COLUMN sys_tenants.slug IS 'URL-safe unique identifier for the tenant';
COMMENT ON COLUMN sys_tenants.tier IS 'Service tier (STANDARD, PREMIUM, ENTERPRISE)';
COMMENT ON COLUMN sys_tenants.region_code IS 'Primary data residency region code';
COMMENT ON COLUMN sys_tenants.config IS 'Flexible JSONB tenant configuration';
COMMENT ON COLUMN sys_tenants.datasource_key IS 'Optional dedicated datasource routing key';

-- ============================================================================
-- SYS_REGIONS: Region registry
-- ============================================================================

CREATE TABLE sys_regions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Data residency and compliance rules (JSONB)
    -- Example:
    -- {
    --   "dataRetentionDays": 2555,
    --   "gdprCompliant": true,
    --   "encryptionRequired": true,
    --   "allowedStorageProviders": ["aws", "azure"]
    -- }
    data_residency_rules JSONB NOT NULL DEFAULT '{}'::jsonb,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    PRIMARY KEY (id),

    CONSTRAINT uq_region_code UNIQUE (code)
);

-- Indexes
CREATE INDEX idx_sys_regions_active ON sys_regions (is_active);

-- Triggers
CREATE TRIGGER audit_sys_regions
    BEFORE UPDATE ON sys_regions
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE sys_regions IS 'Region registry for data residency and compliance';
COMMENT ON COLUMN sys_regions.code IS 'Region code (US, EU, UK, APAC)';
COMMENT ON COLUMN sys_regions.data_residency_rules IS 'JSONB compliance and residency configuration';

-- ============================================================================
-- SYS_DATASOURCES: Datasource registry
-- ============================================================================

CREATE TABLE sys_datasources (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    username VARCHAR(100) NOT NULL,
    pool_size INT NOT NULL DEFAULT 20,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Additional connection configuration (JSONB)
    -- Example:
    -- {
    --   "connectionTimeout": 30000,
    --   "idleTimeout": 600000,
    --   "maxLifetime": 1800000,
    --   "leakDetectionThreshold": 10000
    -- }
    config JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    PRIMARY KEY (id),

    CONSTRAINT uq_datasource_name UNIQUE (name),

    CONSTRAINT chk_datasource_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

-- Triggers
CREATE TRIGGER audit_sys_datasources
    BEFORE UPDATE ON sys_datasources
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE sys_datasources IS 'Database connection pool registry for cell-based routing';
COMMENT ON COLUMN sys_datasources.name IS 'Datasource key used in tenant routing (e.g., master, tenant-vip-1)';
COMMENT ON COLUMN sys_datasources.url IS 'JDBC connection URL';
COMMENT ON COLUMN sys_datasources.pool_size IS 'HikariCP maximum pool size';

-- ============================================================================
-- SEED DATA
-- ============================================================================

-- Default regions
INSERT INTO sys_regions (code, name, description, data_residency_rules) VALUES
    ('US', 'United States', 'US data residency zone', '{"gdprCompliant": false, "dataRetentionDays": 2555}'::jsonb),
    ('EU', 'European Union', 'EU data residency zone (GDPR)', '{"gdprCompliant": true, "dataRetentionDays": 1825, "encryptionRequired": true}'::jsonb),
    ('UK', 'United Kingdom', 'UK data residency zone', '{"gdprCompliant": true, "dataRetentionDays": 2190}'::jsonb),
    ('APAC', 'Asia Pacific', 'APAC data residency zone', '{"gdprCompliant": false, "dataRetentionDays": 2555}'::jsonb);

-- Default tenant
INSERT INTO sys_tenants (tenant_id, name, slug, status, tier, region_code, config) VALUES
    ('default', 'Default Tenant', 'default', 'ACTIVE', 'STANDARD', 'US',
     '{"marketContexts": ["RETAIL", "COMMERCIAL", "LONDON_MARKET"]}'::jsonb);

-- Default datasource
INSERT INTO sys_datasources (name, url, username, pool_size, config) VALUES
    ('master', 'jdbc:postgresql://localhost:5432/beema_dev', 'beema_admin', 20,
     '{"connectionTimeout": 30000, "idleTimeout": 600000, "maxLifetime": 1800000}'::jsonb);

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.7.0', 'Admin management tables (tenants, regions, datasources)', CURRENT_USER);
