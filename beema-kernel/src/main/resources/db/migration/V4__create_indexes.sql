-- V4: Performance Indexes
--
-- Strategic indexes for common query patterns in bitemporal systems.

-- ============================================================================
-- AGREEMENTS - Indexes
-- ============================================================================

-- Current version queries (most common use case)
-- Example: SELECT * FROM agreements WHERE id = ? AND is_current = TRUE
CREATE INDEX idx_agreements_current
    ON agreements(id)
    WHERE is_current = TRUE;

-- Current by agreement number (user-facing lookups)
CREATE INDEX idx_agreements_number_current
    ON agreements(agreement_number, tenant_id)
    WHERE is_current = TRUE;

-- Temporal range queries
-- Example: Find all versions valid during a specific period
CREATE INDEX idx_agreements_temporal_range
    ON agreements(id, valid_from, valid_to);

-- Transaction time queries (audit trail)
-- Example: "What did we know on 2024-01-15?"
CREATE INDEX idx_agreements_transaction_time
    ON agreements(id, transaction_time);

-- Tenant isolation (multi-tenancy)
-- Example: Find all current agreements for tenant in a specific market
CREATE INDEX idx_agreements_tenant_market
    ON agreements(tenant_id, market_context)
    WHERE is_current = TRUE;

-- Agreement type queries
-- Example: Find all current auto policies
CREATE INDEX idx_agreements_type_context
    ON agreements(agreement_type_code, market_context)
    WHERE is_current = TRUE;

-- Status queries
-- Example: Find all active agreements
CREATE INDEX idx_agreements_status
    ON agreements(status, tenant_id)
    WHERE is_current = TRUE;

-- JSONB attribute searches (GIN index for containment queries)
-- Example: SELECT * FROM agreements WHERE attributes @> '{"vehicle_make": "Honda"}'
CREATE INDEX idx_agreements_attributes_gin
    ON agreements USING GIN (attributes);

-- JSONB specific path index (for frequently queried attributes)
-- Example: SELECT * FROM agreements WHERE attributes->>'vehicle_vin' = '...'
CREATE INDEX idx_agreements_attributes_vehicle_vin
    ON agreements ((attributes->>'vehicle_vin'))
    WHERE is_current = TRUE AND market_context = 'RETAIL';

-- Data residency (compliance queries)
CREATE INDEX idx_agreements_residency
    ON agreements(data_residency_region, tenant_id)
    WHERE is_current = TRUE;

-- ============================================================================
-- AGREEMENT_PARTIES - Indexes
-- ============================================================================

-- Current version queries
CREATE INDEX idx_agreement_parties_current
    ON agreement_parties(id)
    WHERE is_current = TRUE;

-- Find parties by agreement (most common join)
-- Example: Get all parties for an agreement
CREATE INDEX idx_agreement_parties_agreement
    ON agreement_parties(agreement_id, party_role)
    WHERE is_current = TRUE;

-- Temporal range queries
CREATE INDEX idx_agreement_parties_temporal_range
    ON agreement_parties(id, valid_from, valid_to);

-- Tenant isolation
CREATE INDEX idx_agreement_parties_tenant
    ON agreement_parties(tenant_id)
    WHERE is_current = TRUE;

-- Party role queries
-- Example: Find all brokers
CREATE INDEX idx_agreement_parties_role
    ON agreement_parties(party_role, tenant_id)
    WHERE is_current = TRUE;

-- JSONB party data searches
CREATE INDEX idx_agreement_parties_data_gin
    ON agreement_parties USING GIN (party_data);

-- Email lookups (frequent query for notifications)
CREATE INDEX idx_agreement_parties_email
    ON agreement_parties ((party_data->>'email'))
    WHERE is_current = TRUE;

-- ============================================================================
-- AGREEMENT_COVERAGES - Indexes
-- ============================================================================

-- Current version queries
CREATE INDEX idx_agreement_coverages_current
    ON agreement_coverages(id)
    WHERE is_current = TRUE;

-- Find coverages by agreement
-- Example: Get all coverages for an agreement
CREATE INDEX idx_agreement_coverages_agreement
    ON agreement_coverages(agreement_id, coverage_code)
    WHERE is_current = TRUE;

-- Temporal range queries
CREATE INDEX idx_agreement_coverages_temporal_range
    ON agreement_coverages(id, valid_from, valid_to);

-- Tenant isolation
CREATE INDEX idx_agreement_coverages_tenant
    ON agreement_coverages(tenant_id)
    WHERE is_current = TRUE;

-- Coverage code queries
-- Example: Find all comprehensive coverages
CREATE INDEX idx_agreement_coverages_code
    ON agreement_coverages(coverage_code, tenant_id)
    WHERE is_current = TRUE;

-- Premium queries (for reporting)
-- Example: Total premium by tenant
CREATE INDEX idx_agreement_coverages_premium
    ON agreement_coverages(tenant_id, premium)
    WHERE is_current = TRUE;

-- JSONB coverage attributes
CREATE INDEX idx_agreement_coverages_attributes_gin
    ON agreement_coverages USING GIN (coverage_attributes);

-- ============================================================================
-- METADATA TABLES - Additional Indexes (supplement V2)
-- ============================================================================

-- Composite lookup: type + context + version
CREATE INDEX idx_metadata_types_lookup
    ON metadata_agreement_types(type_code, market_context, schema_version)
    WHERE is_active = TRUE;

-- Active types by context
CREATE INDEX idx_metadata_types_context_active
    ON metadata_agreement_types(market_context)
    WHERE is_active = TRUE;

-- Attribute lookups
CREATE INDEX idx_metadata_attributes_lookup
    ON metadata_attributes(attribute_key, market_context)
    WHERE is_active = TRUE;

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON INDEX idx_agreements_current IS 'Optimizes current version queries (most common)';
COMMENT ON INDEX idx_agreements_temporal_range IS 'Optimizes bitemporal range queries';
COMMENT ON INDEX idx_agreements_attributes_gin IS 'Enables fast JSONB containment queries (@>)';
COMMENT ON INDEX idx_agreement_parties_agreement IS 'Optimizes party lookups by agreement';
COMMENT ON INDEX idx_agreement_coverages_agreement IS 'Optimizes coverage lookups by agreement';

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.3.0', 'Performance indexes for bitemporal queries', CURRENT_USER);
