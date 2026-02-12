# Webhook Verification Scripts

This directory contains end-to-end verification scripts for the Beema Metadata Webhooks system.

## Quick Start

Run the complete automated verification:

```bash
./scripts/verify-webhooks-e2e.sh
```

## Scripts Overview

### Main Verification Scripts

1. **verify-webhooks-e2e.sh** (Main automated test)
   - Checks all prerequisites (services running)
   - Inserts test webhook into database
   - Creates test agreement via API
   - Verifies event propagation through Inngest
   - Checks webhook delivery status
   - Provides interactive cleanup

2. **test-agreement-creation.sh**
   - Standalone script to create test agreements
   - Useful for triggering webhook events
   - Generates unique agreement numbers

3. **check-webhook-deliveries.sh**
   - Query webhook delivery status from database
   - Can check all deliveries or specific webhook ID
   - Usage: `./scripts/check-webhook-deliveries.sh [webhook_id]`

4. **cleanup-test-webhooks.sh**
   - Removes all test webhooks and deliveries
   - Cleans up data created during testing

### SQL Scripts

Located in `scripts/sql/`:

1. **insert-test-webhook.sql**
   - Directly inserts test webhook via SQL
   - Useful for manual database setup
   - Usage: `docker exec -i beema-postgres psql -U beema -d beema_kernel < scripts/sql/insert-test-webhook.sql`

## Complete Documentation

See `/Users/prabhatkumar/Desktop/dev-directory/beema/WEBHOOK_VERIFICATION_GUIDE.md` for:
- Detailed manual verification steps
- Troubleshooting guide
- Expected success outputs
- Service health checks

## Prerequisites

Before running verification scripts:

1. **Services must be running:**
   - beema-kernel (port 8080)
   - PostgreSQL (beema-postgres container)
   - Inngest Dev Server (port 8288)
   - Studio (port 3000, optional)

2. **Check services:**
   ```bash
   docker-compose ps
   curl http://localhost:8080/actuator/health
   curl http://localhost:8288/health
   ```

3. **Start missing services:**
   ```bash
   docker-compose up -d
   cd apps/beema-kernel && mvn spring-boot:run
   npx inngest-cli@latest dev
   ```

## Verification Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Step 1: Insert Test Webhook                                │
│ ↓                                                           │
│ sys_webhooks table                                          │
│   - webhook_name: test-webhook-<timestamp>                 │
│   - event_type: agreement/created                          │
│   - url: https://httpbin.org/post                          │
│   - enabled: true                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 2: Create Agreement via API                           │
│ ↓                                                           │
│ POST /api/v1/agreements                                     │
│   - agreementNumber: AGR-TEST-<timestamp>                  │
│   - Event published: agreement/created                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 3: Inngest Processes Event                            │
│ ↓                                                           │
│ Event received → webhook-dispatcher function runs           │
│   - Fetches webhooks from sys_webhooks                     │
│   - Dispatches HTTP POST to each URL                       │
│   - Records delivery in sys_webhook_deliveries             │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 4: Verify Delivery                                    │
│ ↓                                                           │
│ Check sys_webhook_deliveries:                               │
│   - status: success                                        │
│   - status_code: 200                                       │
│   - response_body: httpbin.org response                    │
└─────────────────────────────────────────────────────────────┘
```

## Example Usage

### Full automated test:
```bash
./scripts/verify-webhooks-e2e.sh
```

### Manual step-by-step:
```bash
# 1. Insert webhook
docker exec -i beema-postgres psql -U beema -d beema_kernel < scripts/sql/insert-test-webhook.sql

# 2. Create agreement
./scripts/test-agreement-creation.sh

# 3. Wait for processing
sleep 5

# 4. Check deliveries
./scripts/check-webhook-deliveries.sh

# 5. Cleanup
./scripts/cleanup-test-webhooks.sh
```

### Check Inngest UI:
```bash
# Open in browser
open http://localhost:8288

# Navigate to:
# - Events tab: See agreement/created events
# - Functions tab: See webhook-dispatcher runs
```

## Troubleshooting

### Script fails at prerequisites
- Start missing services with `docker-compose up -d`
- Check service health endpoints

### Webhook not firing
- Verify webhook is enabled in database
- Check beema-kernel logs for event publication
- Verify Inngest is receiving events

### Delivery failed
- Check error message in sys_webhook_deliveries
- Verify target URL is reachable
- Check Inngest function logs

See WEBHOOK_VERIFICATION_GUIDE.md for detailed troubleshooting steps.

## Environment Variables

Customize service URLs if needed:

```bash
export BEEMA_KERNEL_URL="http://localhost:8080"
export STUDIO_URL="http://localhost:3000"
export INNGEST_URL="http://localhost:8288"
export POSTGRES_HOST="localhost"
export POSTGRES_PORT="5433"
export POSTGRES_DB="beema_kernel"
export POSTGRES_USER="beema"
```

## File Structure

```
beema/
├── scripts/
│   ├── verify-webhooks-e2e.sh           # Main automated test
│   ├── test-agreement-creation.sh       # Agreement creation helper
│   ├── check-webhook-deliveries.sh      # Database query helper
│   ├── cleanup-test-webhooks.sh         # Cleanup helper
│   ├── sql/
│   │   └── insert-test-webhook.sql      # SQL webhook insertion
│   └── README-WEBHOOK-VERIFICATION.md   # This file
└── WEBHOOK_VERIFICATION_GUIDE.md        # Complete manual guide
```

## Support

For issues or questions:
- Check WEBHOOK_VERIFICATION_GUIDE.md
- Review Inngest Dev UI logs
- Check database tables: sys_webhooks, sys_webhook_deliveries
- Review beema-kernel application logs
