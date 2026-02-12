# Domain Event Publishing Guide

## Overview

Beema Kernel publishes domain lifecycle events to Inngest for webhook fan-out and integrations. This enables downstream systems to react to policy, claim, and agreement changes in real-time.

## Architecture

```
┌─────────────────┐
│  Beema Kernel   │
│   (Spring Boot) │
└────────┬────────┘
         │ HTTP POST
         │ /e/{event-key}
         ▼
┌─────────────────┐
│     Inngest     │
│  Event Gateway  │
└────────┬────────┘
         │
         ├─► Webhook 1
         ├─► Webhook 2
         └─► Webhook N
```

## Supported Events

### Policy Events

- **`policy/bound`** - Policy issued/bound
  - Data: policyNumber, agreementId, marketContext, premium, inceptionDate, expiryDate
- **`policy/renewed`** - Policy renewed
  - Data: policyNumber, newPolicyNumber, renewalDate
- **`policy/cancelled`** - Policy cancelled (future)
- **`policy/updated`** - Policy details updated (future)

### Claim Events

- **`claim/opened`** - New claim reported
  - Data: claimNumber, claimId, claimAmount, claimType
- **`claim/settled`** - Claim settlement completed
  - Data: claimNumber, settlementAmount, settlementType
- **`claim/updated`** - Claim details updated (future)
- **`claim/rejected`** - Claim rejected (future)

### Agreement Events

- **`agreement/created`** - New agreement created (same as policy/bound)
- **`agreement/updated`** - Agreement modified
  - Data: agreementId, changeType, changes
- **`agreement/expired`** - Agreement expired (future)

## Usage

### 1. Publish from Service

```java
@Service
public class MyService {

    @Autowired
    private DomainEventPublisher eventPublisher;

    public void createPolicy() {
        // ... business logic

        PolicyBoundEvent event = new PolicyBoundEvent(
            policyNumber,
            agreementId,
            marketContext
        );
        event.withUser(userId, email);
        event.withData("premium", premium);

        eventPublisher.publish(event);
    }
}
```

### 2. Publish with Metadata

```java
// Automatically adds tenant and user context
eventPublisher.publishWithMetadata(
    event,
    tenantId,
    userId,
    userEmail
);
```

### 3. Batch Publishing

```java
PolicyBoundEvent event1 = new PolicyBoundEvent(...);
PolicyBoundEvent event2 = new PolicyBoundEvent(...);
ClaimOpenedEvent event3 = new ClaimOpenedEvent(...);

eventPublisher.publishBatch(event1, event2, event3);
```

## Testing

### Test Events via API

```bash
# Test policy bound event
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound

# Test claim opened event
curl -X POST http://localhost:8080/api/v1/events/test/claim-opened

# Test claim settled event
curl -X POST http://localhost:8080/api/v1/events/test/claim-settled

# Test policy renewed event
curl -X POST http://localhost:8080/api/v1/events/test/policy-renewed

# Test batch publishing
curl -X POST http://localhost:8080/api/v1/events/test/batch

# Custom event
curl -X POST http://localhost:8080/api/v1/events/publish \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "custom/event",
    "data": {
      "key": "value",
      "amount": 1000
    }
  }'
```

### Test with Inngest Dev Server

1. Start Inngest Dev Server:
   ```bash
   npx inngest-cli@latest dev
   ```

2. Access Inngest UI: http://localhost:8288

3. Publish test events from Beema Kernel

4. View events in Inngest UI

## Event Structure

All events follow this structure:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "policy/bound",
  "ts": 1706800000000,
  "v": "2024-11-01",
  "data": {
    "policyNumber": "POL-001",
    "agreementId": "agreement-123",
    "marketContext": "RETAIL",
    "premium": 1250.00,
    "inceptionDate": "2026-02-12T00:00:00Z",
    "tenantId": "tenant-123"
  },
  "user": {
    "id": "user-1",
    "email": "user@beema.io"
  }
}
```

## Configuration

### application.yml

```yaml
inngest:
  event-key: ${INNGEST_EVENT_KEY:local}
  signing-key: ${INNGEST_SIGNING_KEY:test-signing-key}
  base-url: ${INNGEST_BASE_URL:http://localhost:8288}
  app-id: beema-kernel

beema:
  events:
    publisher:
      enabled: ${EVENTS_PUBLISHER_ENABLED:true}
      mode: ${EVENTS_PUBLISHER_MODE:inngest}  # inngest, redis, kafka
      async: true
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `INNGEST_EVENT_KEY` | `local` | Event key for Inngest |
| `INNGEST_SIGNING_KEY` | `test-signing-key` | Signing key for webhooks |
| `INNGEST_BASE_URL` | `http://localhost:8288` | Inngest server URL |
| `EVENTS_PUBLISHER_ENABLED` | `true` | Enable/disable publishing |

## Implementation Details

### Async Publishing

Events are published asynchronously using Spring's `@Async` with a dedicated thread pool:

- Core pool size: 5
- Max pool size: 10
- Queue capacity: 100

### Error Handling

Event publishing failures DO NOT fail the main business operation:

- Errors are logged but do not throw exceptions
- Failed events can be retried manually
- Consider implementing a dead-letter queue for failed events

### Performance

- Publishing is non-blocking
- Events are sent via reactive WebClient
- Minimal impact on API response times

## Monitoring

### Logs

Events are logged at INFO level:

```
2026-02-12 10:30:45 INFO  DomainEventPublisher - Publishing event: policy/bound (ID: 550e8400-e29b-41d4-a716-446655440000)
2026-02-12 10:30:45 INFO  DomainEventPublisher - Event published successfully: policy/bound - Response: {"status":"ok"}
```

Failed events are logged at ERROR level:

```
2026-02-12 10:30:45 ERROR DomainEventPublisher - Failed to publish event: policy/bound
```

### Metrics

Future enhancements:
- Event publishing rate
- Event publishing latency
- Failed event count

## Integration Examples

### Webhook Handler (Node.js)

```typescript
import { serve } from "inngest/next";
import { inngest } from "./inngest/client";

export const { GET, POST, PUT } = serve({
  client: inngest,
  functions: [
    inngest.createFunction(
      { id: "handle-policy-bound" },
      { event: "policy/bound" },
      async ({ event, step }) => {
        // Send email notification
        await step.run("send-email", async () => {
          await sendEmail({
            to: event.user.email,
            subject: "Policy Bound",
            body: `Your policy ${event.data.policyNumber} has been issued.`
          });
        });

        // Update CRM
        await step.run("update-crm", async () => {
          await updateCRM({
            policyNumber: event.data.policyNumber,
            status: "bound"
          });
        });
      }
    )
  ]
});
```

## Troubleshooting

### Events Not Appearing in Inngest

1. Check if publishing is enabled:
   ```yaml
   beema.events.publisher.enabled: true
   ```

2. Verify Inngest is running:
   ```bash
   curl http://localhost:8288/health
   ```

3. Check logs for errors:
   ```bash
   grep "DomainEventPublisher" logs/beema-kernel.log
   ```

### Slow Event Publishing

1. Increase thread pool size in `EventConfig.java`
2. Check network latency to Inngest
3. Consider batching events

## Future Enhancements

- [ ] Redis fallback for offline scenarios
- [ ] Event replay mechanism
- [ ] Dead-letter queue for failed events
- [ ] Event schema validation
- [ ] Prometheus metrics
- [ ] Event deduplication
- [ ] Priority-based publishing

## References

- [Inngest Documentation](https://www.inngest.com/docs)
- [Spring Async Documentation](https://spring.io/guides/gs/async-method/)
- [WebClient Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
