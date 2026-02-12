# Webhook Dispatcher Guide

## Overview
The webhook-dispatcher Inngest function receives domain events from beema-kernel and fans out HTTP requests to registered webhook URLs.

## Architecture

```
beema-kernel → Inngest → webhook-dispatcher → User Webhooks
                ↓
         sys_webhooks table
```

## Event Flow

1. Domain event occurs (e.g., Policy Bound)
2. beema-kernel publishes event to Inngest
3. webhook-dispatcher function triggered
4. Queries sys_webhooks table for matching webhooks
5. Fans out HTTP POST requests to all matching URLs
6. Records delivery results in sys_webhook_deliveries

## Webhook Signature

All webhooks include HMAC signature for verification:

```
X-Beema-Signature: sha256=<hmac>
X-Beema-Event: policy/bound
X-Beema-Delivery: <uuid>
```

Verify signature:
```javascript
const crypto = require('crypto');

function verifySignature(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');

  return `sha256=${expectedSignature}` === signature;
}
```

## Configuration

### Register Webhook
```sql
INSERT INTO sys_webhooks (
  webhook_name, tenant_id, event_type, url, secret, enabled, created_by
) VALUES (
  'Slack Notifications',
  'tenant-1',
  'claim/opened',
  'https://hooks.slack.com/services/...',
  'your-secret-key',
  true,
  'system'
);
```

### Wildcard Events
Use `*` to receive all events:
```sql
event_type = '*'
```

### Custom Headers
Add custom headers to webhook requests:
```sql
UPDATE sys_webhooks
SET headers = '{"Authorization": "Bearer token", "X-Custom": "value"}'
WHERE webhook_id = 1;
```

### Retry Configuration
Configure retry behavior:
```sql
UPDATE sys_webhooks
SET retry_config = '{"maxAttempts": 5, "backoffMs": 2000}'
WHERE webhook_id = 1;
```

## Testing

### 1. Start Inngest Dev Server
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm inngest-dev
```

This will start the Inngest dev server at http://localhost:8288

### 2. Start Studio App
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm dev
```

### 3. Publish Test Event
```bash
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound
```

### 4. View Function Runs
Open http://localhost:8288 to see the webhook-dispatcher function runs

### 5. Run Test Script
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
./scripts/test-webhook-dispatch.sh
```

## API Endpoints

### List Webhooks
```bash
curl -X GET http://localhost:3000/api/webhooks \
  -H "X-Tenant-ID: tenant-1"
```

### Create Webhook
```bash
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-1" \
  -d '{
    "webhook_name": "Test Webhook",
    "event_type": "policy/bound",
    "url": "https://webhook.site/your-unique-url",
    "secret": "your-secret-key",
    "enabled": true,
    "headers": {"X-Custom": "value"},
    "retry_config": {"maxAttempts": 3, "backoffMs": 1000}
  }'
```

### Update Webhook
```bash
curl -X PUT http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "webhook_id": 1,
    "enabled": false
  }'
```

### Delete Webhook
```bash
curl -X DELETE "http://localhost:3000/api/webhooks?id=1"
```

### View Delivery Logs
```bash
curl -X GET "http://localhost:3000/api/webhooks/deliveries?webhook_id=1"
```

## Monitoring

### View Function Runs
Visit http://localhost:8288 to see:
- All function runs
- Event payload
- Execution steps
- Errors and retries

### Check Delivery Logs
```sql
SELECT * FROM sys_webhook_deliveries
WHERE status = 'failed'
ORDER BY delivered_at DESC
LIMIT 10;
```

### Monitor Webhook Health
```sql
SELECT
  w.webhook_id,
  w.webhook_name,
  w.url,
  COUNT(d.delivery_id) as total_deliveries,
  SUM(CASE WHEN d.status = 'success' THEN 1 ELSE 0 END) as successful,
  SUM(CASE WHEN d.status = 'failed' THEN 1 ELSE 0 END) as failed
FROM sys_webhooks w
LEFT JOIN sys_webhook_deliveries d ON w.webhook_id = d.webhook_id
WHERE w.enabled = true
GROUP BY w.webhook_id, w.webhook_name, w.url;
```

## Event Types

The webhook dispatcher supports the following event types:

### policy/bound
Triggered when a policy is bound.
```typescript
{
  event: 'policy/bound',
  data: {
    policyNumber: string;
    agreementId: string;
    marketContext: string;
    premium?: number;
    productType?: string;
    effectiveDate?: string;
    tenantId?: string;
  },
  user: {
    id: string;
    email: string;
  },
  timestamp: string;
}
```

### claim/opened
Triggered when a claim is opened.
```typescript
{
  event: 'claim/opened',
  data: {
    claimNumber: string;
    claimId: string;
    claimAmount: number;
    claimType: string;
    policyNumber?: string;
    tenantId?: string;
  },
  user: {
    id: string;
    email: string;
  },
  timestamp: string;
}
```

### agreement/updated
Triggered when an agreement is updated.
```typescript
{
  event: 'agreement/updated',
  data: {
    agreementId: string;
    changeType: string;
    changes: Record<string, any>;
    tenantId?: string;
  },
  user: {
    id: string;
    email: string;
  },
  timestamp: string;
}
```

### claim/settled
Triggered when a claim is settled.
```typescript
{
  event: 'claim/settled',
  data: {
    claimNumber: string;
    settlementAmount: number;
    settlementType: string;
    tenantId?: string;
  },
  user: {
    id: string;
    email: string;
  },
  timestamp: string;
}
```

## Security

### HMAC Signature Verification
All webhook deliveries include an HMAC signature in the `X-Beema-Signature` header. Recipients should verify this signature before processing the webhook payload.

Example verification (Node.js):
```javascript
const crypto = require('crypto');

function verifyWebhook(req, secret) {
  const signature = req.headers['x-beema-signature'];
  const payload = JSON.stringify(req.body);

  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');

  return signature === `sha256=${expectedSignature}`;
}

// Express middleware example
app.post('/webhook', (req, res) => {
  if (!verifyWebhook(req, process.env.WEBHOOK_SECRET)) {
    return res.status(401).json({ error: 'Invalid signature' });
  }

  // Process webhook
  console.log('Received event:', req.body);
  res.json({ success: true });
});
```

### Secret Management
- Store webhook secrets securely
- Rotate secrets regularly
- Use different secrets for each webhook
- Never commit secrets to version control

## Troubleshooting

### Webhook not receiving events
1. Check webhook is enabled: `SELECT * FROM sys_webhooks WHERE webhook_id = 1;`
2. Verify event_type matches: Use `*` for all events
3. Check Inngest dev server: http://localhost:8288
4. Review delivery logs: `SELECT * FROM sys_webhook_deliveries WHERE webhook_id = 1;`

### Webhook deliveries failing
1. Check URL is accessible
2. Verify timeout settings (default 30s)
3. Review error messages in sys_webhook_deliveries
4. Test webhook URL manually with curl

### High retry count
1. Check webhook endpoint response time
2. Review retry_config settings
3. Consider increasing timeout
4. Monitor webhook endpoint health

## Production Deployment

### Environment Variables
Ensure these environment variables are set:
```bash
INNGEST_EVENT_KEY=<production-event-key>
INNGEST_SIGNING_KEY=<production-signing-key>
NEXT_PUBLIC_INNGEST_URL=https://inn.gs
NEXT_PUBLIC_API_URL=https://api.beema.com
```

### Database Setup
Run the migration script:
```bash
psql -U postgres -d beema -f prisma/migrations/001_create_webhooks.sql
```

### Monitoring
- Set up alerts for failed deliveries
- Monitor webhook delivery latency
- Track webhook success rates
- Set up dashboard for webhook health

## Files Created

### Core Files
- `/lib/inngest/client.ts` - Inngest client configuration
- `/inngest/webhook-dispatcher.ts` - Main webhook dispatcher function
- `/app/api/inngest/route.ts` - Inngest serve endpoint

### API Routes
- `/app/api/webhooks/route.ts` - Webhook CRUD operations
- `/app/api/webhooks/match/route.ts` - Webhook matching endpoint
- `/app/api/webhooks/deliveries/route.ts` - Delivery logging endpoint

### Database
- `/prisma/migrations/001_create_webhooks.sql` - Database schema

### Testing & Documentation
- `/scripts/test-webhook-dispatch.sh` - Test script
- `/WEBHOOK_DISPATCHER_GUIDE.md` - This guide

## Next Steps

1. Install dependencies: `pnpm install`
2. Run database migration
3. Start Inngest dev server: `pnpm inngest-dev`
4. Start Studio app: `pnpm dev`
5. Test with sample events
6. Configure production webhooks
7. Set up monitoring and alerts
