-- V19: Create policies table with bitemporal SCD Type 2 support
-- Supports full audit history, point-in-time queries, and version lineage

CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_number VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,

    -- Bitemporal fields
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    transaction_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_current BOOLEAN NOT NULL DEFAULT true,

    -- Policy details
    status VARCHAR(50) NOT NULL,
    premium DECIMAL(15,2),
    inception_date TIMESTAMPTZ,
    expiry_date TIMESTAMPTZ,
    coverage_details JSONB,

    -- Multi-tenancy
    tenant_id VARCHAR(100),

    -- Audit
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Composite unique constraint: one version per policy number
    CONSTRAINT uk_policy_version UNIQUE (policy_number, version)
);

-- Fast lookup for current version of a policy
CREATE INDEX idx_policies_current ON policies(policy_number, is_current) WHERE is_current = true;

-- Bitemporal range queries
CREATE INDEX idx_policies_valid_period ON policies(policy_number, valid_from, valid_to);

-- Tenant isolation
CREATE INDEX idx_policies_tenant ON policies(tenant_id);

-- Compound temporal index for as-of queries
CREATE INDEX idx_policies_temporal ON policies(policy_number, valid_from, valid_to, transaction_time);
