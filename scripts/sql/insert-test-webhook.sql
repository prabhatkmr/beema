-- Insert test webhook for manual verification
-- Usage: docker exec beema-postgres psql -U beema -d beema_kernel -f /path/to/insert-test-webhook.sql

DO $$
DECLARE
    v_webhook_id BIGINT;
BEGIN
    -- Insert test webhook
    INSERT INTO sys_webhooks (
        webhook_name,
        tenant_id,
        event_type,
        url,
        secret,
        enabled,
        headers,
        retry_config,
        created_by,
        created_at,
        updated_at
    ) VALUES (
        'httpbin-test-webhook',
        'default',
        'agreement/created',
        'https://httpbin.org/post',
        'whsec_test_12345678901234567890',
        true,
        '{"X-Test-Header": "Beema-Verification", "X-Environment": "local"}'::jsonb,
        '{"maxAttempts": 3, "backoffMs": 1000}'::jsonb,
        'sql-script',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (tenant_id, webhook_name) DO UPDATE
    SET
        url = EXCLUDED.url,
        enabled = EXCLUDED.enabled,
        updated_at = CURRENT_TIMESTAMP
    RETURNING webhook_id INTO v_webhook_id;

    RAISE NOTICE 'Test webhook created/updated with ID: %', v_webhook_id;
    RAISE NOTICE 'Event type: agreement/created';
    RAISE NOTICE 'URL: https://httpbin.org/post';
    RAISE NOTICE 'Status: enabled';
END $$;

-- Verify insertion
SELECT
    webhook_id,
    webhook_name,
    event_type,
    url,
    enabled,
    created_at
FROM sys_webhooks
WHERE webhook_name = 'httpbin-test-webhook';
