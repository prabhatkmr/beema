#!/bin/bash

# Check webhook deliveries from database
# Usage: ./scripts/check-webhook-deliveries.sh [webhook_id]

POSTGRES_USER="${POSTGRES_USER:-beema}"
POSTGRES_DB="${POSTGRES_DB:-beema_kernel}"
WEBHOOK_ID=$1

if [ -z "$WEBHOOK_ID" ]; then
    echo "Checking all recent webhook deliveries..."
    echo ""

    docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
        SELECT
            d.delivery_id,
            w.webhook_name,
            d.event_type,
            d.status,
            d.status_code,
            d.attempt_number,
            d.delivered_at,
            LEFT(d.error_message, 50) as error_preview
        FROM sys_webhook_deliveries d
        JOIN sys_webhooks w ON d.webhook_id = w.webhook_id
        ORDER BY d.delivered_at DESC
        LIMIT 20;
    "
else
    echo "Checking deliveries for webhook ID: $WEBHOOK_ID"
    echo ""

    docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
        SELECT
            delivery_id,
            event_id,
            event_type,
            status,
            status_code,
            response_body,
            error_message,
            attempt_number,
            delivered_at
        FROM sys_webhook_deliveries
        WHERE webhook_id = $WEBHOOK_ID
        ORDER BY delivered_at DESC
        LIMIT 10;
    "
fi

echo ""
echo "To see full response body for a specific delivery:"
echo "docker exec beema-postgres psql -U $POSTGRES_USER -d $POSTGRES_DB -c \\"
echo "  \"SELECT response_body FROM sys_webhook_deliveries WHERE delivery_id = <ID>;\""
