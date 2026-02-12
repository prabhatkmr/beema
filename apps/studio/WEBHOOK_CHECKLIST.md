# Webhook Dispatcher - Implementation Checklist

## Installation Steps

### 1. Install Dependencies
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm install
```

Expected new packages:
- `inngest@^3.15.0`
- `axios@^1.6.0`

### 2. Database Setup
Run the migration to create webhook tables:
```bash
# Connect to your PostgreSQL database
psql -U postgres -d beema -f /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio/prisma/migrations/001_create_webhooks.sql
```

Verify tables created:
```sql
\d sys_webhooks
\d sys_webhook_deliveries
```

### 3. Environment Configuration
Ensure `.env.local` has:
```bash
INNGEST_EVENT_KEY=local
INNGEST_SIGNING_KEY=test-signing-key
NEXT_PUBLIC_INNGEST_URL=http://localhost:8288
NEXT_PUBLIC_API_URL=http://localhost:3000
BEEMA_KERNEL_URL=http://localhost:8080
```

## Running the System

### Start Services (2 Terminals)

#### Terminal 1: Inngest Dev Server
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm inngest-dev
```

Expected output:
```
✓ Inngest dev server running at http://localhost:8288
```

#### Terminal 2: Studio App
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm dev
```

Expected output:
```
✓ Ready on http://localhost:3000
```

## Testing

### Manual Testing

#### 1. Create a Test Webhook
```bash
curl -X POST http://localhost:3000/api/webhooks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{
    "webhook_name": "Test Webhook",
    "event_type": "*",
    "url": "https://webhook.site/your-unique-url",
    "secret": "test-secret-key",
    "enabled": true
  }'
```

Expected: `201 Created` with webhook details

#### 2. List Webhooks
```bash
curl http://localhost:3000/api/webhooks \
  -H "X-Tenant-ID: test-tenant"
```

Expected: JSON array of webhooks

#### 3. Trigger a Test Event
Get a test URL from https://webhook.site/, then:
```bash
# Update the webhook URL first, then send test event
curl -X POST http://localhost:3000/api/inngest \
  -H "Content-Type: application/json" \
  -d '{
    "name": "policy/bound",
    "data": {
      "policyNumber": "POL-TEST-001",
      "agreementId": "AGR-TEST-001",
      "marketContext": "retail",
      "premium": 5000,
      "tenantId": "test-tenant"
    },
    "user": {
      "id": "user-test",
      "email": "test@example.com"
    }
  }'
```

#### 4. Verify in Inngest Dashboard
1. Open http://localhost:8288
2. Look for "webhook-dispatcher" function
3. Check function runs
4. Verify event payload and delivery results

#### 5. Check webhook.site
Visit your webhook.site URL to see the delivered webhook

#### 6. View Delivery Logs
```bash
curl http://localhost:3000/api/webhooks/deliveries
```

### Automated Testing
```bash
# Run integration tests
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm test

# Run test script
chmod +x ./scripts/test-webhook-dispatch.sh
./scripts/test-webhook-dispatch.sh
```

## Verification Checklist

### Installation
- [ ] Dependencies installed (`pnpm install` completed)
- [ ] `inngest` and `axios` packages present in node_modules
- [ ] Database migration executed successfully
- [ ] `sys_webhooks` table exists
- [ ] `sys_webhook_deliveries` table exists
- [ ] Environment variables configured

### Runtime
- [ ] Inngest dev server starts on port 8288
- [ ] Studio app starts on port 3000
- [ ] Inngest dashboard accessible at http://localhost:8288
- [ ] Studio app accessible at http://localhost:3000
- [ ] No errors in console logs

### API Endpoints
- [ ] `GET /api/webhooks` returns webhook list
- [ ] `POST /api/webhooks` creates new webhook
- [ ] `PUT /api/webhooks` updates webhook
- [ ] `DELETE /api/webhooks?id=N` deletes webhook
- [ ] `POST /api/webhooks/match` matches webhooks by event
- [ ] `GET /api/webhooks/deliveries` returns delivery logs
- [ ] `POST /api/webhooks/deliveries` records deliveries

### Inngest Integration
- [ ] `/api/inngest` endpoint responds
- [ ] webhook-dispatcher function visible in dashboard
- [ ] Function triggers on event publish
- [ ] Function steps execute in order
- [ ] Delivery results recorded

### Webhook Delivery
- [ ] Webhooks receive events at their URLs
- [ ] HMAC signature included in headers
- [ ] Event type header present
- [ ] Delivery ID header present
- [ ] Custom headers passed through
- [ ] Response captured in delivery logs

### Security
- [ ] HMAC signature generated correctly
- [ ] Signature verifiable by receiver
- [ ] Secrets stored securely
- [ ] HTTPS enforced in production

### Error Handling
- [ ] Invalid signatures rejected
- [ ] Failed deliveries logged
- [ ] Retry logic works
- [ ] Error messages captured
- [ ] Timeout handled gracefully

## File Structure Verification

### Core Files
- [ ] `/lib/inngest/client.ts` - Inngest client
- [ ] `/inngest/webhook-dispatcher.ts` - Dispatcher function
- [ ] `/app/api/inngest/route.ts` - Inngest serve endpoint

### API Routes
- [ ] `/app/api/webhooks/route.ts` - CRUD operations
- [ ] `/app/api/webhooks/match/route.ts` - Webhook matching
- [ ] `/app/api/webhooks/deliveries/route.ts` - Delivery logs

### Database
- [ ] `/prisma/migrations/001_create_webhooks.sql` - Migration

### Configuration
- [ ] `.env.local` - Environment variables
- [ ] `.env.example` - Example config
- [ ] `.env.webhooks.example` - Webhook config
- [ ] `package.json` - Updated with dependencies

### Types & Tests
- [ ] `/types/webhook.ts` - TypeScript types
- [ ] `/__tests__/webhook-dispatcher.test.ts` - Tests
- [ ] `/examples/webhook-receiver.js` - Example receiver

### Documentation
- [ ] `WEBHOOK_DISPATCHER_GUIDE.md` - Full guide
- [ ] `WEBHOOK_SETUP.md` - Quick setup
- [ ] `WEBHOOK_IMPLEMENTATION.md` - Implementation details
- [ ] `WEBHOOK_CHECKLIST.md` - This checklist

### Scripts
- [ ] `/scripts/test-webhook-dispatch.sh` - Test script
- [ ] Script is executable (`chmod +x`)

## Common Issues & Solutions

### Issue: Inngest dev server won't start
**Solution:**
- Check port 8288 is available: `lsof -i :8288`
- Kill conflicting process: `kill -9 <PID>`
- Clear Inngest cache: `rm -rf .inngest`

### Issue: Webhooks not receiving events
**Solution:**
- Verify webhook is enabled in database
- Check event_type matches (use `*` for all)
- Review Inngest dashboard for errors
- Check tenant_id matches

### Issue: HMAC signature verification failing
**Solution:**
- Ensure secret matches in database and receiver
- Verify payload is exact JSON string
- Check header extraction (lowercase headers)
- Test with example receiver

### Issue: Database connection failing
**Solution:**
- Verify DATABASE_URL in .env.local
- Check PostgreSQL is running
- Run migration again
- Verify table permissions

### Issue: Module not found errors
**Solution:**
- Run `pnpm install` again
- Clear node_modules: `rm -rf node_modules && pnpm install`
- Check package.json has correct dependencies
- Restart dev server

## Production Deployment Checklist

### Pre-deployment
- [ ] All tests passing
- [ ] Environment variables configured for production
- [ ] Database migration executed on production DB
- [ ] Secrets rotated and stored securely
- [ ] HTTPS enforced
- [ ] Rate limiting configured

### Deployment
- [ ] Build succeeds: `pnpm build`
- [ ] Production Inngest key configured
- [ ] Inngest signing key configured
- [ ] Health checks passing
- [ ] Monitoring setup
- [ ] Alerts configured

### Post-deployment
- [ ] Verify Inngest connection
- [ ] Test webhook creation
- [ ] Trigger test event
- [ ] Verify delivery logs
- [ ] Monitor error rates
- [ ] Check webhook success rates

## Monitoring Setup

### Metrics to Track
- [ ] Webhook delivery success rate
- [ ] Average delivery latency
- [ ] Failed delivery count
- [ ] Retry count
- [ ] Active webhooks count

### Alerts to Configure
- [ ] Failed delivery rate > 10%
- [ ] Delivery latency > 5 seconds
- [ ] Webhook endpoint down
- [ ] Inngest connection lost
- [ ] Database connection issues

### Dashboards
- [ ] Webhook health dashboard
- [ ] Delivery metrics over time
- [ ] Event type distribution
- [ ] Tenant usage statistics
- [ ] Error rate trends

## Support Resources

### Documentation
- Full Guide: `/WEBHOOK_DISPATCHER_GUIDE.md`
- Quick Setup: `/WEBHOOK_SETUP.md`
- Implementation: `/WEBHOOK_IMPLEMENTATION.md`
- This Checklist: `/WEBHOOK_CHECKLIST.md`

### External Resources
- Inngest Docs: https://www.inngest.com/docs
- Webhook Testing: https://webhook.site/
- HMAC Guide: https://en.wikipedia.org/wiki/HMAC

### Code Examples
- Webhook Receiver: `/examples/webhook-receiver.js`
- Test Script: `/scripts/test-webhook-dispatch.sh`
- Integration Tests: `/__tests__/webhook-dispatcher.test.ts`

## Sign-off

### Implementation Complete
- [ ] All files created
- [ ] Dependencies installed
- [ ] Database migrated
- [ ] Tests passing
- [ ] Documentation complete

### Testing Complete
- [ ] Manual testing done
- [ ] Automated tests passing
- [ ] Integration verified
- [ ] Security verified
- [ ] Performance acceptable

### Ready for Production
- [ ] Code reviewed
- [ ] Security audit passed
- [ ] Performance tested
- [ ] Monitoring configured
- [ ] Documentation approved
- [ ] Team trained

---

**Implementation Date:** 2026-02-12
**Version:** 1.0.0
**Status:** Complete
