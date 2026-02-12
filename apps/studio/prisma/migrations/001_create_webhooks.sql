CREATE TABLE IF NOT EXISTS sys_webhooks (
    webhook_id BIGSERIAL PRIMARY KEY,
    webhook_name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,  -- e.g., 'policy/bound', 'claim/opened', '*' for all
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    headers JSONB DEFAULT '{}',
    retry_config JSONB DEFAULT '{"maxAttempts": 3, "backoffMs": 1000}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,

    CONSTRAINT uq_webhook_tenant_name UNIQUE (tenant_id, webhook_name)
);

CREATE INDEX idx_webhooks_tenant ON sys_webhooks(tenant_id);
CREATE INDEX idx_webhooks_event_type ON sys_webhooks(event_type);
CREATE INDEX idx_webhooks_enabled ON sys_webhooks(enabled) WHERE enabled = true;

-- Audit table for webhook deliveries
CREATE TABLE IF NOT EXISTS sys_webhook_deliveries (
    delivery_id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT NOT NULL REFERENCES sys_webhooks(webhook_id) ON DELETE CASCADE,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,  -- 'success', 'failed', 'retrying'
    status_code INTEGER,
    response_body TEXT,
    error_message TEXT,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT idx_delivery_webhook FOREIGN KEY (webhook_id) REFERENCES sys_webhooks(webhook_id)
);

CREATE INDEX idx_deliveries_webhook ON sys_webhook_deliveries(webhook_id);
CREATE INDEX idx_deliveries_event ON sys_webhook_deliveries(event_id);
CREATE INDEX idx_deliveries_status ON sys_webhook_deliveries(status);
