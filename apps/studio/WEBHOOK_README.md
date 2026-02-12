# Beema Studio - Webhook Dispatcher System

## Overview

A complete, production-ready webhook dispatcher system built with Inngest for the Beema Studio application. This system receives domain events from beema-kernel and automatically delivers them to registered webhook URLs.

## Quick Start

**New to webhooks?** Start here: [`WEBHOOK_QUICKSTART.md`](./WEBHOOK_QUICKSTART.md) (5 minutes)

```bash
# 1. Install dependencies
pnpm install

# 2. Run database migration
psql -U postgres -d beema -f prisma/migrations/001_create_webhooks.sql

# 3. Start Inngest (Terminal 1)
pnpm inngest-dev

# 4. Start Studio (Terminal 2)
pnpm dev
```

## Documentation

### Getting Started
- **[Quick Start Guide](./WEBHOOK_QUICKSTART.md)** - 5-minute setup guide
- **[Setup Guide](./WEBHOOK_SETUP.md)** - Detailed setup instructions
- **[Installation Checklist](./WEBHOOK_CHECKLIST.md)** - Step-by-step verification

### Technical Documentation
- **[Complete Summary](./WEBHOOK_COMPLETE_SUMMARY.md)** - Comprehensive overview
- **[Technical Guide](./WEBHOOK_DISPATCHER_GUIDE.md)** - Full technical documentation
- **[Implementation Details](./WEBHOOK_IMPLEMENTATION.md)** - Implementation notes
- **[Architecture](./WEBHOOK_ARCHITECTURE.md)** - Architecture diagrams and data flow

### Additional Resources
- **[Examples](./examples/webhook-receiver.js)** - Sample webhook receiver
- **[Tests](./scripts/test-webhook-dispatch.sh)** - Test scripts
- **[Integration Tests](./__tests__/webhook-dispatcher.test.ts)** - Automated tests

## Features

- **Event-Driven Architecture** - Built on Inngest for reliability
- **Fan-out Pattern** - Parallel delivery to multiple webhooks
- **HMAC Security** - SHA256 signature verification
- **Automatic Retries** - Configurable retry with backoff
- **Multi-tenant** - Tenant isolation built-in
- **Audit Trail** - Complete delivery history
- **Observability** - Full Inngest dashboard integration

## Architecture

```
beema-kernel → Inngest → webhook-dispatcher → User Webhooks
                              ↓
                       sys_webhooks table
```

### Event Flow
1. Domain event occurs (e.g., Policy Bound)
2. beema-kernel publishes to Inngest
3. webhook-dispatcher triggered
4. Queries sys_webhooks for matches
5. Fans out HTTP POSTs to URLs
6. Records results in sys_webhook_deliveries

## API Endpoints

### Webhook Management
- `GET /api/webhooks` - List webhooks
- `POST /api/webhooks` - Create webhook
- `PUT /api/webhooks` - Update webhook
- `DELETE /api/webhooks?id=N` - Delete webhook

### Webhook Matching & Delivery
- `POST /api/webhooks/match` - Find matching webhooks
- `GET /api/webhooks/deliveries` - Query delivery logs
- `POST /api/webhooks/deliveries` - Record delivery results

### Inngest
- `GET /api/inngest` - Inngest dev UI
- `POST /api/inngest` - Event handler
- `PUT /api/inngest` - Function registration

## Supported Event Types

1. **policy/bound** - Policy is bound
2. **claim/opened** - Claim is opened
3. **claim/settled** - Claim is settled
4. **agreement/updated** - Agreement is updated
5. ***** - Wildcard (all events)

## Testing

### Quick Test
```bash
# 1. Get test URL from https://webhook.site/

# 2. Create webhook
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "webhook_name": "Test",
    "event_type": "*",
    "url": "https://webhook.site/YOUR-ID",
    "secret": "test-secret",
    "enabled": true
  }'

# 3. Trigger event
curl -X POST http://localhost:3000/api/inngest \
  -H "Content-Type: application/json" \
  -d '{
    "name": "policy/bound",
    "data": {
      "policyNumber": "POL-001",
      "tenantId": "test"
    },
    "user": {"id": "user-1", "email": "test@example.com"}
  }'

# 4. Verify at webhook.site and http://localhost:8288
```

### Run Test Script
```bash
./scripts/test-webhook-dispatch.sh
```

### Run Integration Tests
```bash
pnpm test
```

## File Structure

```
/apps/studio
├── inngest/
│   └── webhook-dispatcher.ts       # Main dispatcher function
├── lib/inngest/
│   └── client.ts                   # Inngest client config
├── app/api/
│   ├── inngest/route.ts            # Inngest serve endpoint
│   └── webhooks/
│       ├── route.ts                # CRUD operations
│       ├── match/route.ts          # Webhook matching
│       └── deliveries/route.ts     # Delivery logs
├── prisma/migrations/
│   └── 001_create_webhooks.sql    # Database schema
├── types/
│   └── webhook.ts                  # TypeScript types
├── examples/
│   └── webhook-receiver.js         # Example receiver
├── scripts/
│   └── test-webhook-dispatch.sh   # Test script
├── __tests__/
│   └── webhook-dispatcher.test.ts # Integration tests
└── [Documentation files]
```

## Database Tables

### sys_webhooks
Stores webhook configurations with tenant isolation, event type filtering, and custom headers.

### sys_webhook_deliveries
Audit trail of all webhook deliveries with status, response, and error tracking.

## Security

### HMAC Signature Verification
Every webhook includes:
- `X-Beema-Signature: sha256=<hmac>`
- `X-Beema-Event: <event-type>`
- `X-Beema-Delivery: <uuid>`

Receivers should verify the signature before processing:
```javascript
const crypto = require('crypto');
const signature = req.headers['x-beema-signature'];
const payload = JSON.stringify(req.body);
const expected = crypto.createHmac('sha256', secret)
  .update(payload).digest('hex');
const valid = signature === `sha256=${expected}`;
```

## Monitoring

### Inngest Dashboard
- **Local:** http://localhost:8288
- **Production:** https://inn.gs
- View function runs, event payloads, errors, and retries

### Database Queries
```sql
-- Success rate
SELECT webhook_id,
  COUNT(*) as total,
  SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful
FROM sys_webhook_deliveries GROUP BY webhook_id;

-- Recent failures
SELECT * FROM sys_webhook_deliveries
WHERE status = 'failed'
ORDER BY delivered_at DESC LIMIT 10;
```

## Production Deployment

### Environment Variables
```bash
INNGEST_EVENT_KEY=<production-key>
INNGEST_SIGNING_KEY=<production-signing-key>
NEXT_PUBLIC_INNGEST_URL=https://inn.gs
NEXT_PUBLIC_API_URL=https://api.beema.com
```

### Deployment Checklist
- [ ] Dependencies installed
- [ ] Database migration executed
- [ ] Environment variables configured
- [ ] Secrets stored securely
- [ ] HTTPS enforced
- [ ] Monitoring configured
- [ ] Alerts set up

## Troubleshooting

### Common Issues

**Webhook not firing:**
- Check webhook is enabled in sys_webhooks
- Verify event_type matches
- Check Inngest dashboard for errors
- Review tenant_id matches

**Delivery failing:**
- Test URL with curl
- Check timeout (30s default)
- Verify network connectivity
- Review error_message in deliveries

**Signature verification failing:**
- Ensure secret matches
- Check payload formatting
- Verify header extraction
- Test with example receiver

See [WEBHOOK_CHECKLIST.md](./WEBHOOK_CHECKLIST.md) for full troubleshooting guide.

## Integration with beema-kernel

From your Spring Boot service:
```java
@Autowired
private InngestClient inngestClient;

public void publishEvent(Policy policy) {
    inngestClient.send(
        Event.builder()
            .name("policy/bound")
            .data(Map.of(
                "policyNumber", policy.getPolicyNumber(),
                "tenantId", policy.getTenantId()
            ))
            .user(Map.of(
                "id", currentUser.getId(),
                "email", currentUser.getEmail()
            ))
            .build()
    );
}
```

## Support

### Documentation
- [Quick Start](./WEBHOOK_QUICKSTART.md) - Get started in 5 minutes
- [Complete Summary](./WEBHOOK_COMPLETE_SUMMARY.md) - Full overview
- [Technical Guide](./WEBHOOK_DISPATCHER_GUIDE.md) - Deep dive

### External Resources
- [Inngest Documentation](https://www.inngest.com/docs)
- [Webhook Testing](https://webhook.site/)
- [HMAC Guide](https://en.wikipedia.org/wiki/HMAC)

### Examples
- [Webhook Receiver](./examples/webhook-receiver.js)
- [Test Script](./scripts/test-webhook-dispatch.sh)
- [Integration Tests](./__tests__/webhook-dispatcher.test.ts)

## License

Part of the Beema Unified Platform.

---

**Status:** ✅ Complete and Production Ready
**Version:** 1.0.0
**Last Updated:** February 12, 2026

## What's Next?

1. **Install:** Run `pnpm install`
2. **Migrate:** Run database migration
3. **Start:** Run `pnpm inngest-dev` and `pnpm dev`
4. **Test:** Follow Quick Start guide
5. **Deploy:** See Production Deployment section

For detailed instructions, see [WEBHOOK_QUICKSTART.md](./WEBHOOK_QUICKSTART.md).
