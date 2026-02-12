# Inngest Setup Guide

## Quick Start

```bash
# Start Inngest
./scripts/start-inngest.sh

# Verify setup
./scripts/verify-inngest-setup.sh

# Test events
./scripts/test-inngest-events.sh
```

## Architecture

```
beema-kernel → Inngest Dev Server → Studio webhook-dispatcher → User Webhooks
                     ↓
              Dev UI (localhost:8288)
```

### Event Flow

1. **Event Publication**: beema-kernel publishes events to Inngest
2. **Event Storage**: Inngest stores and displays events in Dev UI
3. **Function Trigger**: Inngest triggers webhook-dispatcher function in Studio
4. **Webhook Delivery**: Studio dispatches webhooks to registered endpoints
5. **Delivery Tracking**: Studio tracks delivery status in sys_webhooks table

## Docker Setup

### Service Configuration

```yaml
inngest:
  image: inngest/inngest:v0.38.0
  ports:
    - "8288:8288"
  environment:
    - INNGEST_EVENT_KEY=local
    - INNGEST_SIGNING_KEY=test-signing-key
    - INNGEST_LOG_LEVEL=info
  volumes:
    - inngest_data:/app/.inngest
```

### Networking

All services are on `beema-network`:
- beema-kernel publishes to `http://inngest:8288`
- Studio serves functions at `/api/inngest`
- Inngest calls Studio webhook-dispatcher

### Environment Variables

**beema-kernel**:
```bash
INNGEST_EVENT_KEY=local
INNGEST_SIGNING_KEY=test-signing-key
INNGEST_BASE_URL=http://inngest:8288
```

**Studio**:
```bash
INNGEST_EVENT_KEY=local
INNGEST_SIGNING_KEY=test-signing-key
NEXT_PUBLIC_INNGEST_URL=http://localhost:8288
```

## Development Workflow

### 1. Start Services

```bash
docker-compose up -d
```

Wait for all services to be healthy:
```bash
docker-compose ps
```

### 2. View Events

- Open http://localhost:8288
- Navigate to "Events" tab
- See real-time events from beema-kernel

### 3. Register Webhooks

- Open http://localhost:3000/webhooks
- Create webhook for specific event type
- Test webhook endpoint

### 4. Test End-to-End Flow

```bash
# Publish event
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound

# Check Inngest UI for event
open http://localhost:8288

# Check webhook deliveries in Studio
open http://localhost:3000/webhooks
```

## Using the Inngest Dev UI

### Events Tab

- **View all events**: See every event published to Inngest
- **Event details**: Click on an event to see payload, metadata, and timestamp
- **Event replay**: Replay events to test function behavior
- **Event filtering**: Filter by event name or time range

### Functions Tab

- **Function list**: See all registered functions (webhook-dispatcher)
- **Function runs**: View execution history with status (success/failed)
- **Run details**: Inspect function logs, input, and output
- **Manual trigger**: Manually trigger functions for testing

### Settings

- **Event key**: local (development)
- **Signing key**: test-signing-key (development)
- **Event retention**: Events are retained for 24 hours in dev mode

## Common Operations

### Start Inngest Only

```bash
docker-compose up -d inngest
```

### Restart Inngest

```bash
docker-compose restart inngest
```

### View Inngest Logs

```bash
# Follow logs
docker-compose logs -f inngest

# Last 100 lines
docker-compose logs --tail=100 inngest
```

### Stop Inngest

```bash
docker-compose stop inngest
```

### Clear Inngest Data

```bash
# Stop Inngest
docker-compose stop inngest

# Remove volume (deletes all events)
docker volume rm beema_inngest_data

# Start fresh
docker-compose up -d inngest
```

## Testing

### Manual Event Publishing

```bash
# Publish test event via beema-kernel
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound \
  -H "Content-Type: application/json"

# Publish directly to Inngest (for debugging)
curl -X POST http://localhost:8288/e/local \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test/event",
    "data": {
      "message": "Test event"
    }
  }'
```

### Verify Function Registration

```bash
# Check if webhook-dispatcher is registered
curl http://localhost:8288/v0/functions
```

Expected response should include `webhook-dispatcher` function.

### Test Webhook Delivery

1. Create a webhook in Studio
2. Publish an event
3. Check Inngest UI for function run
4. Check Studio for delivery status

## Troubleshooting

### Inngest Container Won't Start

**Symptoms**:
- Container exits immediately
- Health check fails

**Solutions**:
```bash
# Check logs
docker-compose logs inngest

# Check if port 8288 is in use
lsof -i :8288

# Remove old container and restart
docker-compose down inngest
docker-compose up -d inngest
```

### Events Not Appearing in UI

**Symptoms**:
- Events published but not visible in Inngest UI
- beema-kernel shows success but Inngest is empty

**Solutions**:
```bash
# Verify beema-kernel can reach Inngest
docker-compose exec beema-kernel curl -f http://inngest:8288/health

# Check beema-kernel environment variables
docker-compose exec beema-kernel env | grep INNGEST

# Check beema-kernel logs for Inngest errors
docker-compose logs beema-kernel | grep -i inngest
```

### Functions Not Triggering

**Symptoms**:
- Events appear in Inngest but webhook-dispatcher doesn't run
- No function runs in Inngest UI

**Solutions**:
```bash
# Verify Studio is running
docker-compose ps studio

# Check if Studio Inngest endpoint is accessible
curl http://localhost:3000/api/inngest

# Check Studio logs for Inngest errors
docker-compose logs studio | grep -i inngest

# Verify function registration
curl http://localhost:8288/v0/functions | jq .
```

### Webhooks Not Being Delivered

**Symptoms**:
- Function runs successfully but webhooks not sent
- No entries in sys_webhooks table

**Solutions**:
```bash
# Check sys_webhooks table
docker-compose exec postgres psql -U beema -d beema_kernel -c "SELECT * FROM sys_webhooks LIMIT 10;"

# Check Studio logs for webhook errors
docker-compose logs studio | grep -i webhook

# Verify webhook configuration in Studio
open http://localhost:3000/webhooks
```

### Connection Refused Errors

**Symptoms**:
- beema-kernel or Studio can't connect to Inngest
- "connection refused" errors in logs

**Solutions**:
```bash
# Verify all services are on beema-network
docker network inspect beema-network

# Check Inngest health
curl http://localhost:8288/health

# Restart services in order
docker-compose restart inngest
docker-compose restart beema-kernel
docker-compose restart studio
```

## Advanced Configuration

### Using docker-compose.inngest.yml Override

For development with enhanced debugging:

```bash
# Start with override
docker-compose -f docker-compose.yml -f docker-compose.inngest.yml up -d

# This enables:
# - Debug logging (INNGEST_LOG_LEVEL=debug)
# - Increased event stream size
# - Separate volume for dev data
```

### Custom Event Retention

Edit `.env.inngest`:
```bash
INNGEST_EVENT_RETENTION_HOURS=48  # Keep events for 48 hours
```

### Enable Verbose Logging

Edit `.env.inngest`:
```bash
INNGEST_LOG_LEVEL=debug
INNGEST_DEBUG=true
```

Restart Inngest:
```bash
docker-compose restart inngest
```

## Production Considerations

### Security

1. **Change signing key**:
   - Use strong, randomly generated key
   - Store in secrets manager (not in docker-compose.yml)

2. **Use Inngest Cloud**:
   - For production, use Inngest Cloud instead of dev server
   - Configure proper authentication
   - Enable webhook signature verification

3. **Network security**:
   - Don't expose port 8288 publicly
   - Use internal network for service communication

### Scalability

1. **Event volume**:
   - Dev server is not designed for high volume
   - Use Inngest Cloud for production workloads

2. **Function concurrency**:
   - Scale Studio instances for parallel webhook delivery
   - Configure appropriate concurrency limits

### Monitoring

1. **Event metrics**:
   - Track event publication rate
   - Monitor function execution times
   - Alert on function failures

2. **Logging**:
   - Centralize logs from all services
   - Correlate events with function runs
   - Track webhook delivery success rates

## Migration to Inngest Cloud

When ready for production:

1. **Sign up**: Create account at https://inngest.com
2. **Get event key**: Copy from Inngest dashboard
3. **Update environment**:
   ```bash
   INNGEST_EVENT_KEY=<your-production-key>
   INNGEST_SIGNING_KEY=<your-signing-key>
   INNGEST_BASE_URL=https://api.inngest.com
   ```
4. **Deploy**: Remove inngest service from docker-compose.yml
5. **Verify**: Check Inngest Cloud dashboard for events

## Resources

- **Inngest Documentation**: https://www.inngest.com/docs
- **Inngest Dev Server**: https://www.inngest.com/docs/local-development
- **Event Patterns**: https://www.inngest.com/docs/events
- **Function Configuration**: https://www.inngest.com/docs/functions

## Support

For Inngest-specific issues:

1. Check Inngest Dev UI: http://localhost:8288
2. Review this troubleshooting guide
3. Check Inngest documentation
4. Run verification script: `./scripts/verify-inngest-setup.sh`

---

**Last Updated**: 2026-02-12
**Inngest Version**: v0.38.0
**Beema Platform**: Event-driven webhook infrastructure
