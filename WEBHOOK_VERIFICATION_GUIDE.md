# Webhook Verification Guide

## Quick End-to-End Test

Run the automated verification script:

```bash
./scripts/verify-webhooks-e2e.sh
```

This script will:
1. ✓ Check all prerequisites (services running)
2. ✓ Insert test webhook into sys_webhooks
3. ✓ Create test agreement via API
4. ✓ Verify event was published
5. ✓ Check webhook delivery status

## Manual Step-by-Step Verification

### Step 1: Insert Test Webhook

```bash
# Option A: Use SQL script
docker exec -i beema-postgres psql -U beema -d beema_kernel < scripts/sql/insert-test-webhook.sql

# Option B: Direct SQL
docker exec beema-postgres psql -U beema -d beema_kernel -c "
INSERT INTO sys_webhooks (
    webhook_name, tenant_id, event_type, url, secret, enabled, created_by
) VALUES (
    'httpbin-test', 'default', 'agreement/created',
    'https://httpbin.org/post', 'whsec_test_secret', true, 'manual'
) ON CONFLICT (tenant_id, webhook_name) DO UPDATE
SET enabled = true;
"
```

### Step 2: Trigger Agreement Creation

```bash
# Option A: Use test script
./scripts/test-agreement-creation.sh

# Option B: Direct curl
curl -X POST http://localhost:8080/api/v1/agreements \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "agreementNumber": "AGR-001",
    "agreementType": "motor_comprehensive",
    "marketContext": "RETAIL",
    "productType": "MOTOR_COMPREHENSIVE",
    "effectiveDate": "2026-02-12T00:00:00Z",
    "policyNumber": "POL-001",
    "premium": 1250.00
  }'

# Option C: Use test event endpoint (simpler)
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound
```

### Step 3: Verify in Inngest Dashboard

1. Open Inngest Dev UI:
   ```
   http://localhost:8288
   ```

2. Navigate to **Events** tab
   - Look for event name: `agreement/created` or `policy/bound`
   - Check event data payload

3. Navigate to **Functions** tab
   - Look for function: `webhook-dispatcher`
   - Click on recent run
   - Check execution logs
   - Verify HTTP 200 response from httpbin.org

4. Check function output for:
   ```json
   {
     "message": "Webhooks dispatched",
     "eventType": "agreement/created",
     "webhooksCount": 1,
     "successCount": 1,
     "failureCount": 0
   }
   ```

### Step 4: Verify Delivery in Database

```bash
# Check recent deliveries
./scripts/check-webhook-deliveries.sh

# Or query directly
docker exec beema-postgres psql -U beema -d beema_kernel -c "
SELECT
    d.delivery_id,
    w.webhook_name,
    d.event_type,
    d.status,
    d.status_code,
    d.delivered_at
FROM sys_webhook_deliveries d
JOIN sys_webhooks w ON d.webhook_id = w.webhook_id
ORDER BY d.delivered_at DESC
LIMIT 10;
"
```

Expected result:
- status: `success`
- status_code: `200`
- delivered_at: recent timestamp

## Troubleshooting

### Webhook Not Firing

1. **Check webhook exists and is enabled:**
   ```bash
   docker exec beema-postgres psql -U beema -d beema_kernel -c \
     "SELECT * FROM sys_webhooks WHERE event_type = 'agreement/created';"
   ```

2. **Check event was published:**
   ```bash
   docker logs beema-kernel | grep "Publishing event"
   ```

3. **Check Inngest received event:**
   - Open http://localhost:8288
   - Look in Events tab

4. **Check webhook-dispatcher function is registered:**
   - Open http://localhost:8288/functions
   - Should see `webhook-dispatcher`

### Delivery Failed

1. **Check error message:**
   ```bash
   docker exec beema-postgres psql -U beema -d beema_kernel -c \
     "SELECT error_message FROM sys_webhook_deliveries ORDER BY delivered_at DESC LIMIT 5;"
   ```

2. **Check URL is reachable:**
   ```bash
   curl -X POST https://httpbin.org/post -d '{"test": true}'
   ```

3. **Check Inngest logs:**
   ```bash
   docker logs beema-inngest
   ```

### Services Not Running

```bash
# Check all services
docker-compose ps

# Start missing services
docker-compose up -d

# Check beema-kernel
curl http://localhost:8080/actuator/health

# Check Inngest
curl http://localhost:8288/health
```

## Expected Success Output

When everything works correctly, you should see:

1. **In beema-kernel logs:**
   ```
   INFO  Publishing event: agreement/created (ID: xxx)
   INFO  Event published successfully: agreement/created
   ```

2. **In Inngest UI:**
   - Event appears in Events tab
   - webhook-dispatcher function executes
   - Function output shows successCount: 1

3. **In database:**
   ```sql
   status = 'success'
   status_code = 200
   ```

4. **In httpbin.org response (visible in Inngest logs):**
   ```json
   {
     "url": "https://httpbin.org/post",
     "headers": {
       "X-Beema-Signature": "sha256=...",
       "X-Beema-Event": "agreement/created"
     }
   }
   ```
