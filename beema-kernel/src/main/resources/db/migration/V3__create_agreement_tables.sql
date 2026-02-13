-- V3: Agreement Core Tables
--
-- Creates bitemporal tables for agreements, parties, and coverages.

-- ============================================================================
-- AGREEMENTS - Master agreement table
-- ============================================================================

CREATE TABLE agreements (
    -- Composite Primary Key (bitemporal)
    id UUID NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    transaction_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Temporal columns
    valid_to TIMESTAMPTZ NOT NULL DEFAULT get_default_valid_to(),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,

    -- Business fields
    agreement_number VARCHAR(100) NOT NULL,
    agreement_type_code VARCHAR(100) NOT NULL,
    market_context VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    -- Flex-schema attributes (JSONB)
    -- Example for AUTO_POLICY:
    -- {
    --   "vehicle_vin": "1HGCM82633A123456",
    --   "vehicle_year": 2024,
    --   "vehicle_make": "Honda",
    --   "driver_age": 35,
    --   "coverage_type": "COMPREHENSIVE"
    -- }
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Multi-tenancy
    tenant_id VARCHAR(100) NOT NULL,

    -- Data residency (for compliance)
    data_residency_region VARCHAR(50),

    -- Audit fields
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- Constraints
    PRIMARY KEY (id, valid_from, transaction_time),

    CONSTRAINT chk_agreement_market_context
        CHECK (market_context IN ('RETAIL', 'COMMERCIAL', 'LONDON_MARKET')),

    CONSTRAINT chk_agreement_status
        CHECK (status IN ('DRAFT', 'QUOTED', 'BOUND', 'ACTIVE', 'CANCELLED', 'EXPIRED', 'RENEWED'))
);

-- Triggers
CREATE TRIGGER validate_agreement_temporal_range
    BEFORE INSERT OR UPDATE ON agreements
    FOR EACH ROW EXECUTE FUNCTION validate_temporal_range();

CREATE TRIGGER audit_agreements
    BEFORE INSERT OR UPDATE ON agreements
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE agreements IS 'Bitemporal master agreement table supporting all market contexts';
COMMENT ON COLUMN agreements.id IS 'Business identifier (immutable across versions)';
COMMENT ON COLUMN agreements.valid_from IS 'Start of validity period (valid time)';
COMMENT ON COLUMN agreements.valid_to IS 'End of validity period (valid time)';
COMMENT ON COLUMN agreements.transaction_time IS 'When this version was recorded (transaction time)';
COMMENT ON COLUMN agreements.is_current IS 'Flag for current version (latest transaction_time)';
COMMENT ON COLUMN agreements.agreement_number IS 'User-visible agreement number (e.g., POL-2024-001234)';
COMMENT ON COLUMN agreements.attributes IS 'Flex-schema JSONB for market-specific attributes';
COMMENT ON COLUMN agreements.tenant_id IS 'Multi-tenancy identifier for row-level security';
COMMENT ON COLUMN agreements.data_residency_region IS 'Geographic region for data compliance (US, EU, APAC)';

-- ============================================================================
-- AGREEMENT_PARTIES - Bitemporal party relationships
-- ============================================================================

CREATE TABLE agreement_parties (
    -- Composite Primary Key (bitemporal)
    id UUID NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    transaction_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Temporal columns
    valid_to TIMESTAMPTZ NOT NULL DEFAULT get_default_valid_to(),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,

    -- Relationships
    agreement_id UUID NOT NULL,  -- Links to agreements.id (business id, not composite PK)

    -- Party details
    party_role VARCHAR(50) NOT NULL,  -- INSURED, INSURER, BROKER, REINSURER, etc.
    party_type VARCHAR(50) NOT NULL,  -- INDIVIDUAL, ORGANIZATION

    -- Flex-schema party attributes
    -- Example for INDIVIDUAL:
    -- {
    --   "first_name": "John",
    --   "last_name": "Doe",
    --   "date_of_birth": "1988-05-15",
    --   "email": "john.doe@example.com"
    -- }
    party_data JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Multi-tenancy
    tenant_id VARCHAR(100) NOT NULL,

    -- Audit fields
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- Constraints
    PRIMARY KEY (id, valid_from, transaction_time),

    CONSTRAINT chk_party_role
        CHECK (party_role IN ('INSURED', 'INSURER', 'BROKER', 'REINSURER', 'CEDING_COMPANY', 'NAMED_INSURED', 'ADDITIONAL_INSURED')),

    CONSTRAINT chk_party_type
        CHECK (party_type IN ('INDIVIDUAL', 'ORGANIZATION'))
);

-- Triggers
CREATE TRIGGER validate_agreement_parties_temporal_range
    BEFORE INSERT OR UPDATE ON agreement_parties
    FOR EACH ROW EXECUTE FUNCTION validate_temporal_range();

CREATE TRIGGER audit_agreement_parties
    BEFORE INSERT OR UPDATE ON agreement_parties
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE agreement_parties IS 'Bitemporal party relationships (insured, insurer, broker, etc.)';
COMMENT ON COLUMN agreement_parties.agreement_id IS 'Business ID of parent agreement';
COMMENT ON COLUMN agreement_parties.party_role IS 'Role of party in the agreement';
COMMENT ON COLUMN agreement_parties.party_data IS 'Flex-schema JSONB for party-specific attributes';

-- ============================================================================
-- AGREEMENT_COVERAGES - Bitemporal coverage/risk items
-- ============================================================================

CREATE TABLE agreement_coverages (
    -- Composite Primary Key (bitemporal)
    id UUID NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    transaction_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Temporal columns
    valid_to TIMESTAMPTZ NOT NULL DEFAULT get_default_valid_to(),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,

    -- Relationships
    agreement_id UUID NOT NULL,  -- Links to agreements.id

    -- Coverage details
    coverage_code VARCHAR(100) NOT NULL,  -- e.g., "COLLISION", "COMPREHENSIVE", "LIABILITY"
    coverage_name VARCHAR(200) NOT NULL,

    -- Financial fields
    coverage_limit DECIMAL(19, 4),
    deductible DECIMAL(19, 4),
    premium DECIMAL(19, 4),
    currency VARCHAR(3) DEFAULT 'USD',

    -- Flex-schema coverage attributes
    -- Example for AUTO_COLLISION:
    -- {
    --   "coverage_applies_to": "OWNED_VEHICLE",
    --   "rental_reimbursement": true,
    --   "towing_coverage": 100.00
    -- }
    coverage_attributes JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Multi-tenancy
    tenant_id VARCHAR(100) NOT NULL,

    -- Audit fields
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- Constraints
    PRIMARY KEY (id, valid_from, transaction_time),

    CONSTRAINT chk_coverage_currency
        CHECK (currency IN ('USD', 'GBP', 'EUR', 'JPY', 'AUD', 'CAD'))
);

-- Triggers
CREATE TRIGGER validate_agreement_coverages_temporal_range
    BEFORE INSERT OR UPDATE ON agreement_coverages
    FOR EACH ROW EXECUTE FUNCTION validate_temporal_range();

CREATE TRIGGER audit_agreement_coverages
    BEFORE INSERT OR UPDATE ON agreement_coverages
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE agreement_coverages IS 'Bitemporal coverage items with financial details';
COMMENT ON COLUMN agreement_coverages.agreement_id IS 'Business ID of parent agreement';
COMMENT ON COLUMN agreement_coverages.coverage_limit IS 'Maximum payout amount';
COMMENT ON COLUMN agreement_coverages.deductible IS 'Amount insured pays before coverage applies';
COMMENT ON COLUMN agreement_coverages.premium IS 'Cost of this coverage';
COMMENT ON COLUMN agreement_coverages.coverage_attributes IS 'Flex-schema JSONB for coverage-specific details';

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.2.0', 'Agreement core tables (agreements, parties, coverages)', CURRENT_USER);
