-- =============================================================================
-- V10: Create Submissions Table (Bitemporal)
-- =============================================================================
-- Stores quote submissions with full bitemporal tracking.
-- Supports DRAFT -> QUOTED -> BOUND -> DECLINED lifecycle.
-- =============================================================================

-- Ensure beema_admin role exists for RLS policies
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'beema_admin') THEN
        CREATE ROLE beema_admin WITH LOGIN PASSWORD 'beema_admin';
        GRANT ALL PRIVILEGES ON DATABASE beema_kernel TO beema_admin;
    END IF;
END
$$;

CREATE TABLE submissions (
    -- Bitemporal composite primary key
    id                  UUID            NOT NULL,
    valid_from          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    valid_to            TIMESTAMPTZ     NOT NULL DEFAULT '9999-12-31T23:59:59Z',
    transaction_time    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    is_current          BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Business fields
    submission_id       UUID            NOT NULL,
    product             VARCHAR(100)    NOT NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'DRAFT',
    tenant_id           VARCHAR(100)    NOT NULL,

    -- JSONB flex-schema fields
    form_data           JSONB           NOT NULL DEFAULT '{}'::jsonb,
    rating_result       JSONB           NOT NULL DEFAULT '{}'::jsonb,

    -- Audit
    created_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version             BIGINT          NOT NULL DEFAULT 1,

    -- Composite primary key for bitemporal
    PRIMARY KEY (id, valid_from, transaction_time)
);

-- =============================================================================
-- Indexes
-- =============================================================================

-- Current submissions by submission_id (most common query)
CREATE INDEX idx_submissions_current
    ON submissions (submission_id, is_current)
    WHERE is_current = TRUE;

-- Tenant isolation
CREATE INDEX idx_submissions_tenant
    ON submissions (tenant_id, is_current)
    WHERE is_current = TRUE;

-- Product filter
CREATE INDEX idx_submissions_product
    ON submissions (product, tenant_id, is_current)
    WHERE is_current = TRUE;

-- Status filter
CREATE INDEX idx_submissions_status
    ON submissions (status, tenant_id, is_current)
    WHERE is_current = TRUE;

-- Temporal range queries
CREATE INDEX idx_submissions_temporal_range
    ON submissions (id, valid_from, valid_to);

-- Transaction time for audit trail
CREATE INDEX idx_submissions_transaction_time
    ON submissions (id, transaction_time DESC);

-- GIN index on form_data for JSONB containment queries
CREATE INDEX idx_submissions_form_data_gin
    ON submissions USING GIN (form_data);

-- GIN index on rating_result for JSONB queries
CREATE INDEX idx_submissions_rating_result_gin
    ON submissions USING GIN (rating_result);

-- =============================================================================
-- Row-Level Security
-- =============================================================================

ALTER TABLE submissions ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_submissions ON submissions
    USING (tenant_id = current_setting('app.current_tenant', TRUE))
    WITH CHECK (tenant_id = current_setting('app.current_tenant', TRUE));

CREATE POLICY admin_all_submissions ON submissions
    FOR ALL
    TO beema_admin
    USING (TRUE)
    WITH CHECK (TRUE);

-- =============================================================================
-- Comments
-- =============================================================================

COMMENT ON TABLE submissions IS 'Bitemporal quote submissions with flex-schema JSONB storage';
COMMENT ON COLUMN submissions.submission_id IS 'Workflow ID - links to Temporal workflow execution';
COMMENT ON COLUMN submissions.form_data IS 'User-submitted form data (JSONB flex-schema)';
COMMENT ON COLUMN submissions.rating_result IS 'Rating engine output: premium, tax, total (JSONB)';
