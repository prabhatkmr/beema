-- =============================================================================
-- V4__create_indexes.sql
-- Beema Unified Platform - Performance Indexes
-- =============================================================================
-- Creates indexes optimized for the primary query patterns:
--   1. Current-state queries (is_current = TRUE partial index)
--   2. Point-in-time queries (temporal range lookups)
--   3. Tenant isolation (tenant_id prefix on most indexes)
--   4. JSONB attribute searches (GIN indexes)
--   5. Agreement number / reference lookups
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Agreements - Current State Queries
-- ---------------------------------------------------------------------------
-- Most queries fetch only the current version of an agreement.
-- Partial index on is_current = TRUE dramatically reduces index size.
CREATE INDEX idx_agreements_current
    ON agreements(id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_tenant_current
    ON agreements(tenant_id, id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_number_current
    ON agreements(agreement_number)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_market_current
    ON agreements(tenant_id, market_context)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_status_current
    ON agreements(tenant_id, status)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_type_current
    ON agreements(agreement_type_id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_ext_ref
    ON agreements(external_reference)
    WHERE external_reference IS NOT NULL AND is_current = TRUE;

-- ---------------------------------------------------------------------------
-- 2. Agreements - Temporal Range Queries
-- ---------------------------------------------------------------------------
-- "As-of" queries: find the version valid at a specific business time and/or
-- system time. These use range scans over (valid_from, valid_to).
CREATE INDEX idx_agreements_temporal
    ON agreements(id, valid_from, valid_to);

CREATE INDEX idx_agreements_txn_time
    ON agreements(id, transaction_time DESC);

-- Business date range queries (inception/expiry)
CREATE INDEX idx_agreements_inception
    ON agreements(tenant_id, inception_date)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_expiry
    ON agreements(tenant_id, expiry_date)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreements_date_range
    ON agreements(tenant_id, inception_date, expiry_date)
    WHERE is_current = TRUE;

-- ---------------------------------------------------------------------------
-- 3. Agreements - JSONB GIN Index
-- ---------------------------------------------------------------------------
-- Supports @>, ?, ?|, ?& operators on the attributes JSONB column.
-- Uses jsonb_path_ops for smaller index size (supports @> containment only).
CREATE INDEX idx_agreements_attributes
    ON agreements USING GIN (attributes jsonb_path_ops)
    WHERE is_current = TRUE;

-- ---------------------------------------------------------------------------
-- 4. Agreement Parties - Indexes
-- ---------------------------------------------------------------------------
CREATE INDEX idx_agreement_parties_current
    ON agreement_parties(id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_parties_agreement
    ON agreement_parties(agreement_id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_parties_tenant
    ON agreement_parties(tenant_id, agreement_id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_parties_role
    ON agreement_parties(agreement_id, party_role)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_parties_reference
    ON agreement_parties(party_reference)
    WHERE is_current = TRUE;

-- Temporal queries
CREATE INDEX idx_agreement_parties_temporal
    ON agreement_parties(id, valid_from, valid_to);

CREATE INDEX idx_agreement_parties_txn_time
    ON agreement_parties(id, transaction_time DESC);

-- JSONB GIN index
CREATE INDEX idx_agreement_parties_attributes
    ON agreement_parties USING GIN (attributes jsonb_path_ops)
    WHERE is_current = TRUE;

-- ---------------------------------------------------------------------------
-- 5. Agreement Coverages - Indexes
-- ---------------------------------------------------------------------------
CREATE INDEX idx_agreement_coverages_current
    ON agreement_coverages(id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_coverages_agreement
    ON agreement_coverages(agreement_id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_coverages_tenant
    ON agreement_coverages(tenant_id, agreement_id)
    WHERE is_current = TRUE;

CREATE INDEX idx_agreement_coverages_code
    ON agreement_coverages(agreement_id, coverage_code)
    WHERE is_current = TRUE;

-- Temporal queries
CREATE INDEX idx_agreement_coverages_temporal
    ON agreement_coverages(id, valid_from, valid_to);

CREATE INDEX idx_agreement_coverages_txn_time
    ON agreement_coverages(id, transaction_time DESC);

-- JSONB GIN index
CREATE INDEX idx_agreement_coverages_attributes
    ON agreement_coverages USING GIN (attributes jsonb_path_ops)
    WHERE is_current = TRUE;

-- ---------------------------------------------------------------------------
-- 6. Composite indexes for common join patterns
-- ---------------------------------------------------------------------------
-- Agreement + parties join (current versions only)
CREATE INDEX idx_agreements_tenant_market_status
    ON agreements(tenant_id, market_context, status)
    WHERE is_current = TRUE;

-- Data residency filtering
CREATE INDEX idx_agreements_residency
    ON agreements(data_residency_region, tenant_id)
    WHERE is_current = TRUE;
