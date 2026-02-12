-- =============================================================================
-- V3__create_agreement_tables.sql
-- Beema Unified Platform - Core Agreement Tables (Bitemporal)
-- =============================================================================
-- Implements the core business tables using the bitemporal pattern:
--   - agreements          : the main policy/contract entity
--   - agreement_parties   : parties involved in the agreement
--   - agreement_coverages : coverage/section details
--
-- BITEMPORAL COMPOSITE PRIMARY KEY: (id, valid_from, transaction_time)
-- This allows:
--   1. Business-time versioning: different validity periods for corrections
--   2. System-time versioning: full audit trail of every database change
--   3. "As-of" queries: what did the record look like at any point in time?
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. agreements
-- ---------------------------------------------------------------------------
CREATE TABLE agreements (
    -- Identity
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    agreement_number        VARCHAR(50)     NOT NULL,
    external_reference      VARCHAR(100),

    -- Classification
    market_context          market_context_type NOT NULL,
    agreement_type_id       UUID            NOT NULL,
    status                  agreement_status_type NOT NULL DEFAULT 'DRAFT',

    -- Multi-tenancy
    tenant_id               UUID            NOT NULL,
    data_residency_region   VARCHAR(10)     NOT NULL DEFAULT 'EU',

    -- Business dates
    inception_date          DATE            NOT NULL,
    expiry_date             DATE            NOT NULL,

    -- Financial
    currency_code           VARCHAR(3)      NOT NULL DEFAULT 'GBP',
    total_premium           NUMERIC(18, 4),
    total_sum_insured       NUMERIC(18, 4),

    -- Flex-schema attributes (validated against metadata_agreement_types.attribute_schema)
    attributes              JSONB           NOT NULL DEFAULT '{}'::JSONB,

    -- Bitemporal columns
    valid_from              TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    valid_to                TIMESTAMPTZ     NOT NULL DEFAULT 'infinity'::TIMESTAMPTZ,
    transaction_time        TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    is_current              BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),

    -- Composite PK: supports bitemporal versioning
    CONSTRAINT pk_agreements PRIMARY KEY (id, valid_from, transaction_time),

    -- Foreign key to agreement type metadata
    CONSTRAINT fk_agreements_type
        FOREIGN KEY (agreement_type_id) REFERENCES metadata_agreement_types(id),

    -- Business rules
    CONSTRAINT chk_agreements_dates CHECK (inception_date <= expiry_date),
    CONSTRAINT chk_agreements_premium CHECK (total_premium IS NULL OR total_premium >= 0),
    CONSTRAINT chk_agreements_sum_insured CHECK (total_sum_insured IS NULL OR total_sum_insured >= 0)
);

COMMENT ON TABLE agreements IS
    'Core agreement/policy entity with bitemporal versioning and JSONB flex-schema attributes.';
COMMENT ON COLUMN agreements.attributes IS
    'Flexible JSON attributes validated at the application layer against metadata_agreement_types.attribute_schema.';
COMMENT ON COLUMN agreements.valid_from IS
    'Business validity start. Part of composite PK for bitemporal versioning.';
COMMENT ON COLUMN agreements.valid_to IS
    'Business validity end. infinity = currently valid.';
COMMENT ON COLUMN agreements.transaction_time IS
    'System time when this version was recorded. Immutable once written.';
COMMENT ON COLUMN agreements.is_current IS
    'TRUE for the latest transaction-time version. Managed by trigger.';

-- Attach standard bitemporal triggers
SELECT attach_bitemporal_triggers('public', 'agreements');

-- ---------------------------------------------------------------------------
-- 2. agreement_parties
-- ---------------------------------------------------------------------------
CREATE TABLE agreement_parties (
    -- Identity
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    agreement_id            UUID            NOT NULL,

    -- Party details
    party_role              party_role_type NOT NULL,
    party_reference         VARCHAR(100)    NOT NULL,
    party_name              VARCHAR(255),
    share_percentage        NUMERIC(7, 4),

    -- Multi-tenancy
    tenant_id               UUID            NOT NULL,

    -- Flex-schema attributes
    attributes              JSONB           NOT NULL DEFAULT '{}'::JSONB,

    -- Bitemporal columns
    valid_from              TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    valid_to                TIMESTAMPTZ     NOT NULL DEFAULT 'infinity'::TIMESTAMPTZ,
    transaction_time        TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    is_current              BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),

    -- Composite PK
    CONSTRAINT pk_agreement_parties PRIMARY KEY (id, valid_from, transaction_time),

    -- Business rules
    CONSTRAINT chk_party_share CHECK (share_percentage IS NULL OR (share_percentage >= 0 AND share_percentage <= 100))
);

COMMENT ON TABLE agreement_parties IS
    'Parties associated with an agreement (policyholder, insurer, broker, etc.) with bitemporal versioning.';
COMMENT ON COLUMN agreement_parties.party_reference IS
    'External reference to the party master system (e.g., party UUID or code).';
COMMENT ON COLUMN agreement_parties.share_percentage IS
    'Participation share as percentage (0-100). Used primarily for London Market syndicate shares.';

-- Attach standard bitemporal triggers
SELECT attach_bitemporal_triggers('public', 'agreement_parties');

-- ---------------------------------------------------------------------------
-- 3. agreement_coverages
-- ---------------------------------------------------------------------------
CREATE TABLE agreement_coverages (
    -- Identity
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    agreement_id            UUID            NOT NULL,

    -- Coverage details
    coverage_code           VARCHAR(100)    NOT NULL,
    coverage_name           VARCHAR(255)    NOT NULL,
    coverage_type           VARCHAR(50),

    -- Financial
    currency_code           VARCHAR(3)      NOT NULL DEFAULT 'GBP',
    premium                 NUMERIC(18, 4),
    sum_insured             NUMERIC(18, 4),
    deductible              NUMERIC(18, 4),
    deductible_type         VARCHAR(20),
    limit_amount            NUMERIC(18, 4),
    rate                    NUMERIC(12, 8),

    -- Multi-tenancy
    tenant_id               UUID            NOT NULL,

    -- Flex-schema attributes
    attributes              JSONB           NOT NULL DEFAULT '{}'::JSONB,

    -- Bitemporal columns
    valid_from              TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    valid_to                TIMESTAMPTZ     NOT NULL DEFAULT 'infinity'::TIMESTAMPTZ,
    transaction_time        TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    is_current              BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),

    -- Composite PK
    CONSTRAINT pk_agreement_coverages PRIMARY KEY (id, valid_from, transaction_time),

    -- Business rules
    CONSTRAINT chk_coverage_premium CHECK (premium IS NULL OR premium >= 0),
    CONSTRAINT chk_coverage_sum_insured CHECK (sum_insured IS NULL OR sum_insured >= 0),
    CONSTRAINT chk_coverage_deductible CHECK (deductible IS NULL OR deductible >= 0),
    CONSTRAINT chk_coverage_limit CHECK (limit_amount IS NULL OR limit_amount >= 0),
    CONSTRAINT chk_coverage_rate CHECK (rate IS NULL OR rate >= 0)
);

COMMENT ON TABLE agreement_coverages IS
    'Coverage sections/lines within an agreement with bitemporal versioning and JSONB attributes.';
COMMENT ON COLUMN agreement_coverages.deductible_type IS
    'Type of deductible: FLAT, PERCENTAGE, AGGREGATE, etc.';
COMMENT ON COLUMN agreement_coverages.rate IS
    'Premium rate (e.g., per-mille or percentage) used to calculate premium.';

-- Attach standard bitemporal triggers
SELECT attach_bitemporal_triggers('public', 'agreement_coverages');
