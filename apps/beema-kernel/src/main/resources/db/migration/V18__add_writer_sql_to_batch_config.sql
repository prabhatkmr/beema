-- V18: Add writer_sql column to sys_batch_job_config
-- Allows batch job configs to specify custom SQL for writing processed items

ALTER TABLE sys_batch_job_config ADD COLUMN writer_sql TEXT;

-- Insert sample batch job definitions

-- Close Expired Claims
INSERT INTO sys_batch_job_config (
    job_config_id,
    job_name,
    description,
    reader_sql,
    processor_jexl,
    writer_sql,
    chunk_size,
    enabled,
    tenant_id,
    created_by,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'close-expired-claims',
    'Close claims older than 90 days',
    'SELECT id, claim_number, status, created_at FROM claims WHERE status = ''OPEN'' AND created_at < CURRENT_DATE - INTERVAL ''90 days''',
    'item.status = ''CLOSED''; item.close_reason = ''EXPIRED''; item',
    'UPDATE claims SET status = :status, close_reason = :close_reason, updated_at = CURRENT_TIMESTAMP WHERE id = :id',
    100,
    true,
    'default',
    'admin',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Premium Uplift
INSERT INTO sys_batch_job_config (
    job_config_id,
    job_name,
    description,
    reader_sql,
    processor_jexl,
    writer_sql,
    chunk_size,
    enabled,
    tenant_id,
    created_by,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'premium-uplift-10pct',
    'Apply 10% premium increase to active policies',
    'SELECT id, premium FROM agreements WHERE tenant_id = ''default'' AND status = ''ACTIVE''',
    'item.premium = item.premium * 1.10; item',
    'UPDATE agreements SET premium = :premium, updated_at = CURRENT_TIMESTAMP WHERE id = :id',
    1000,
    true,
    'default',
    'admin',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
