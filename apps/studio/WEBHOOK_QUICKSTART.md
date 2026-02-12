# Webhook Dispatcher - Quick Start

## 1. Installation (5 minutes)

```bash
# Navigate to studio
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio

# Install dependencies
pnpm install

# Run database migration
psql -U postgres -d beema -f prisma/migrations/001_create_webhooks.sql
```

## 2. Start Services (2 terminals)

### Terminal 1: Inngest Dev Server
```bash
pnpm inngest-dev
# Opens http://localhost:8288
```

### Terminal 2: Studio App
```bash
pnpm dev
# Opens http://localhost:3000
```

## 3. Test Webhook (5 minutes)

### Step 1: Get Test URL
Visit https://webhook.site/ and copy your unique URL

### Step 2: Create Webhook
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

### Step 3: Trigger Event
```bash
curl -X POST http://localhost:3000/api/inngest \
  -H "Content-Type: application/json" \
  -d '{
    "name": "policy/bound",
    "data": {
      "policyNumber": "POL-001",
      "agreementId": "AGR-001",
      "marketContext": "retail",
      "tenantId": "test-tenant"
    },
    "user": {
      "id": "user-123",
      "email": "test@example.com"
    }
  }'
```

### Step 4: Verify
1. Check webhook.site - you should see the webhook delivery
2. Check Inngest dashboard: http://localhost:8288
3. Check delivery logs:
```bash
curl http://localhost:3000/api/webhooks/deliveries
```

## 4. What Was Created

### Core Files
- `/lib/inngest/client.ts` - Inngest client
- `/inngest/webhook-dispatcher.ts` - Main dispatcher function
- `/app/api/inngest/route.ts` - Inngest endpoint
- `/app/api/webhooks/route.ts` - Webhook CRUD API
- `/app/api/webhooks/match/route.ts` - Webhook matching
- `/app/api/webhooks/deliveries/route.ts` - Delivery logs
- `/prisma/migrations/001_create_webhooks.sql` - Database schema

### Documentation
- `WEBHOOK_COMPLETE_SUMMARY.md` - Complete overview
- `WEBHOOK_SETUP.md` - Detailed setup guide
- `WEBHOOK_DISPATCHER_GUIDE.md` - Technical guide
- `WEBHOOK_ARCHITECTURE.md` - Architecture diagrams
- `WEBHOOK_CHECKLIST.md` - Installation checklist

## 5. Event Types

- `policy/bound` - Policy is bound
- `claim/opened` - Claim is opened
- `claim/settled` - Claim is settled
- `agreement/updated` - Agreement is updated
- `*` - All events (wildcard)

## 6. API Endpoints

```bash
# List webhooks
curl http://localhost:3000/api/webhooks

# Create webhook
curl -X POST http://localhost:3000/api/webhooks -d '{...}'

# Update webhook
curl -X PUT http://localhost:3000/api/webhooks -d '{...}'

# Delete webhook
curl -X DELETE http://localhost:3000/api/webhooks?id=1

# View deliveries
curl http://localhost:3000/api/webhooks/deliveries
```

## 7. Webhook Payload Format

Every webhook receives:
```json
{
  "event": "policy/bound",
  "data": {
    "policyNumber": "POL-001",
    "agreementId": "AGR-001",
    "marketContext": "retail",
    "tenantId": "tenant-1"
  },
  "user": {
    "id": "user-123",
    "email": "john@example.com"
  },
  "timestamp": "2026-02-12T10:00:00Z"
}
```

With headers:
```
X-Beema-Signature: sha256=<hmac-hash>
X-Beema-Event: policy/bound
X-Beema-Delivery: <uuid>
```

## 8. Monitoring

- **Inngest Dashboard:** http://localhost:8288
- **Delivery Logs:** `SELECT * FROM sys_webhook_deliveries;`
- **Success Rate:** See `WEBHOOK_COMPLETE_SUMMARY.md` for SQL queries

## 9. Troubleshooting

### Webhook not firing?
- Check webhook is enabled: `SELECT * FROM sys_webhooks;`
- Verify event_type matches
- Check Inngest dashboard for errors

### Delivery failing?
- Test URL with curl
- Check timeout settings (30s)
- Review error_message in delivery logs

## 10. Next Steps

1. Read `WEBHOOK_COMPLETE_SUMMARY.md` for full details
2. Configure production webhooks
3. Set up monitoring and alerts
4. Integrate with beema-kernel

---

**Status:** ✅ Complete and working
**Time to Setup:** ~10 minutes
**Documentation:** 8 comprehensive guides available
