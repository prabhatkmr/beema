# Webhook Dispatcher - Quick Setup Guide

## Installation

1. Install dependencies:
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm install
```

2. Run database migration:
```bash
# Using beema-kernel database connection
psql -U postgres -d beema -f prisma/migrations/001_create_webhooks.sql
```

3. Verify environment variables in `.env.local`:
```bash
INNGEST_EVENT_KEY=local
INNGEST_SIGNING_KEY=test-signing-key
NEXT_PUBLIC_INNGEST_URL=http://localhost:8288
NEXT_PUBLIC_API_URL=http://localhost:3000
```

## Running the Application

### Terminal 1: Start Inngest Dev Server
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm inngest-dev
```

Open http://localhost:8288 to view the Inngest dashboard.

### Terminal 2: Start Studio App
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm dev
```

Open http://localhost:3000 to view the Studio app.

## Testing the Webhook Dispatcher

### 1. Create a Test Webhook

Use webhook.site to get a test URL:
1. Visit https://webhook.site/
2. Copy your unique URL
3. Create a webhook:

```bash
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{
    "webhook_name": "Test Webhook",
    "event_type": "*",
    "url": "https://webhook.site/YOUR-UNIQUE-ID",
    "secret": "test-secret-key",
    "enabled": true
  }'
```

### 2. Publish a Test Event

From beema-kernel or directly to Inngest:

```bash
curl -X POST http://localhost:3000/api/inngest \
  -H "Content-Type: application/json" \
  -d '{
    "event": "policy/bound",
    "data": {
      "policyNumber": "POL-001",
      "agreementId": "AGR-001",
      "marketContext": "retail",
      "premium": 5000,
      "tenantId": "test-tenant"
    },
    "user": {
      "id": "user-123",
      "email": "test@example.com"
    }
  }'
```

### 3. Verify Delivery

1. Check Inngest dashboard: http://localhost:8288
2. Check webhook.site to see the delivery
3. View delivery logs:

```bash
curl http://localhost:3000/api/webhooks/deliveries
```

## Example Use Cases

### Slack Notifications
```bash
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "webhook_name": "Slack - Claims Opened",
    "event_type": "claim/opened",
    "url": "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK",
    "secret": "your-secret",
    "enabled": true
  }'
```

### Email Notifications
```bash
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "webhook_name": "Email - Policy Bound",
    "event_type": "policy/bound",
    "url": "https://api.sendgrid.com/v3/mail/send",
    "secret": "your-sendgrid-api-key",
    "enabled": true,
    "headers": {
      "Authorization": "Bearer YOUR_SENDGRID_API_KEY"
    }
  }'
```

### Custom Integration
```bash
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "webhook_name": "Custom Integration",
    "event_type": "*",
    "url": "https://your-api.com/webhooks",
    "secret": "your-secret",
    "enabled": true,
    "headers": {
      "X-API-Key": "your-api-key"
    },
    "retry_config": {
      "maxAttempts": 5,
      "backoffMs": 2000
    }
  }'
```

## Architecture Flow

```
┌─────────────────┐
│  Domain Event   │ (e.g., Policy Bound)
│  (beema-kernel) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Inngest     │ Event Bus
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   webhook-      │ Fan-out Function
│   dispatcher    │
└────────┬────────┘
         │
         ├──────────────┬──────────────┬──────────────┐
         ▼              ▼              ▼              ▼
    [Slack]       [Email]        [Custom API]   [Webhook.site]
```

## Event Types

- `policy/bound` - Policy is bound
- `claim/opened` - Claim is opened
- `claim/settled` - Claim is settled
- `agreement/updated` - Agreement is updated
- `*` - All events (wildcard)

## Webhook Payload Example

```json
{
  "event": "policy/bound",
  "data": {
    "policyNumber": "POL-001",
    "agreementId": "AGR-001",
    "marketContext": "retail",
    "premium": 5000,
    "productType": "auto",
    "effectiveDate": "2026-02-12",
    "tenantId": "tenant-1"
  },
  "user": {
    "id": "user-123",
    "email": "john.doe@example.com"
  },
  "timestamp": "2026-02-12T10:30:00Z"
}
```

## Security Headers

Every webhook delivery includes:
- `X-Beema-Signature: sha256=<hmac>` - HMAC signature for verification
- `X-Beema-Event: policy/bound` - Event type
- `X-Beema-Delivery: <uuid>` - Unique delivery ID

## Monitoring

### View All Webhooks
```bash
curl http://localhost:3000/api/webhooks
```

### View Delivery Logs
```bash
curl http://localhost:3000/api/webhooks/deliveries
```

### View Failed Deliveries
```sql
SELECT * FROM sys_webhook_deliveries
WHERE status = 'failed'
ORDER BY delivered_at DESC;
```

### Inngest Dashboard
Open http://localhost:8288 to see:
- Function runs
- Event history
- Errors and retries
- Execution timeline

## Troubleshooting

### Webhook not firing
1. Check webhook is enabled in database
2. Verify event_type matches (use `*` for all events)
3. Check Inngest dashboard for errors
4. Review Studio logs

### Delivery failing
1. Test webhook URL with curl
2. Check timeout settings (default 30s)
3. Verify HMAC signature is correct
4. Review error logs in sys_webhook_deliveries

### Inngest not connecting
1. Ensure Inngest dev server is running
2. Check port 8288 is available
3. Verify INNGEST_EVENT_KEY in .env.local
4. Restart both Inngest and Studio

## Production Checklist

- [ ] Run database migration
- [ ] Set production INNGEST_EVENT_KEY
- [ ] Set production INNGEST_SIGNING_KEY
- [ ] Update NEXT_PUBLIC_INNGEST_URL to https://inn.gs
- [ ] Configure webhook secrets securely
- [ ] Set up monitoring and alerts
- [ ] Test webhook delivery
- [ ] Configure retry policies
- [ ] Set up delivery logs retention
- [ ] Document webhook endpoints

## Support

For more details, see:
- Full documentation: `/WEBHOOK_DISPATCHER_GUIDE.md`
- Inngest docs: https://www.inngest.com/docs
- Test script: `/scripts/test-webhook-dispatch.sh`
