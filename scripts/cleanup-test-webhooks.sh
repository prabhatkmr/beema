#!/bin/bash

echo "Cleaning up test webhooks and deliveries..."

POSTGRES_USER="${POSTGRES_USER:-beema}"
POSTGRES_DB="${POSTGRES_DB:-beema_kernel}"

# Delete test webhook deliveries
docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
DELETE FROM sys_webhook_deliveries
WHERE webhook_id IN (
    SELECT webhook_id FROM sys_webhooks
    WHERE webhook_name LIKE 'test-%' OR webhook_name LIKE '%httpbin%'
);
"

# Delete test webhooks
docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
DELETE FROM sys_webhooks
WHERE webhook_name LIKE 'test-%' OR webhook_name LIKE '%httpbin%';
"

echo "✓ Cleanup complete"
