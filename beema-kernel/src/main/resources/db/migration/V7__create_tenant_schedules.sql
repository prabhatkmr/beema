-- V7: Tenant Batch Schedules
--
-- Per-tenant batch scheduling metadata for Temporal-based job orchestration.

CREATE TABLE sys_tenant_schedules (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    schedule_id VARCHAR(200) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Flexible job parameters (JSONB)
    -- Example for PARQUET_EXPORT:
    -- {
    --   "exportFormat": "PARQUET",
    --   "agreementTypes": ["AUTO_POLICY", "HOME_POLICY"],
    --   "marketContext": "RETAIL",
    --   "storagePath": "exports/tenant-123/daily"
    -- }
    job_params JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Temporal.io integration
    temporal_schedule_id VARCHAR(500),

    -- Audit fields
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    PRIMARY KEY (id),

    CONSTRAINT uq_tenant_schedule_id UNIQUE (tenant_id, schedule_id),

    CONSTRAINT chk_job_type
        CHECK (job_type IN ('PARQUET_EXPORT', 'DATA_SYNC', 'REPORT_GENERATION', 'CLEANUP', 'CUSTOM'))
);

-- Indexes
CREATE INDEX idx_tenant_schedules_tenant ON sys_tenant_schedules (tenant_id);
CREATE INDEX idx_tenant_schedules_active ON sys_tenant_schedules (tenant_id, is_active);
CREATE INDEX idx_tenant_schedules_job_type ON sys_tenant_schedules (job_type);
CREATE INDEX idx_tenant_schedules_temporal ON sys_tenant_schedules (temporal_schedule_id) WHERE temporal_schedule_id IS NOT NULL;

-- Triggers
CREATE TRIGGER audit_sys_tenant_schedules
    BEFORE UPDATE ON sys_tenant_schedules
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE sys_tenant_schedules IS 'Per-tenant batch job schedule configuration';
COMMENT ON COLUMN sys_tenant_schedules.schedule_id IS 'Human-readable schedule identifier unique per tenant';
COMMENT ON COLUMN sys_tenant_schedules.job_type IS 'Type of batch job (PARQUET_EXPORT, DATA_SYNC, etc.)';
COMMENT ON COLUMN sys_tenant_schedules.cron_expression IS 'Cron expression for scheduling (Spring CronExpression format)';
COMMENT ON COLUMN sys_tenant_schedules.job_params IS 'Flexible JSONB parameters passed to the batch job';
COMMENT ON COLUMN sys_tenant_schedules.temporal_schedule_id IS 'Temporal.io schedule handle ID for runtime management';

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.6.0', 'Tenant batch schedule configuration', CURRENT_USER);
