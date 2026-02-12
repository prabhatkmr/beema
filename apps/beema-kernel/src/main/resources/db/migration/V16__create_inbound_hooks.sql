-- ============================================================================
-- V16: Inbound Webhook Hooks
-- Generic webhook registration for external partner integrations.
-- Each hook defines: partner identity, HMAC secret, JEXL transformation script.
-- ============================================================================

CREATE TABLE sys_inbound_hooks (
    hook_id VARCHAR(100) PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    partner_name VARCHAR(255),
    target_object_type VARCHAR(100) NOT NULL,  -- 'AGREEMENT', 'CLAIM', etc.
    signature_secret VARCHAR(500) NOT NULL,
    signature_header VARCHAR(100) DEFAULT 'X-Signature',
    mapping_script TEXT NOT NULL,  -- JEXL transformation script
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE INDEX idx_inbound_hooks_tenant ON sys_inbound_hooks(tenant_id);
CREATE INDEX idx_inbound_hooks_active ON sys_inbound_hooks(is_active);

-- Webhook delivery log for auditing all inbound attempts
CREATE TABLE sys_inbound_hook_log (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hook_id VARCHAR(100) NOT NULL REFERENCES sys_inbound_hooks(hook_id),
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,  -- 'SUCCESS', 'SIGNATURE_FAILED', 'TRANSFORM_FAILED', 'SAVE_FAILED'
    source_ip VARCHAR(45),
    request_headers JSONB,
    request_body JSONB,
    transformed_body JSONB,
    error_message TEXT,
    entity_id VARCHAR(255)  -- ID of created entity (agreement ID, claim ID, etc.)
);

CREATE INDEX idx_inbound_hook_log_hook ON sys_inbound_hook_log(hook_id);
CREATE INDEX idx_inbound_hook_log_received ON sys_inbound_hook_log(received_at);
CREATE INDEX idx_inbound_hook_log_status ON sys_inbound_hook_log(status);
