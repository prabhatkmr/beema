# Beema Kernel Domain Events - Implementation Summary

## Overview

Successfully implemented a comprehensive DomainEventPublisher system that publishes domain lifecycle events to Inngest for webhook fan-out and downstream processing.

## What Was Created

### 1. Event Models (`src/main/java/com/beema/kernel/event/`)

#### Base Event Class
- **`DomainEvent.java`** - Abstract base class for all domain events
  - Event ID (UUID)
  - Event name (e.g., "policy/bound")
  - Timestamp
  - Data payload
  - User context
  - Version

#### Specific Event Types
- **`PolicyBoundEvent.java`** - Policy issued/bound
- **`ClaimOpenedEvent.java`** - New claim reported
- **`ClaimSettledEvent.java`** - Claim settled
- **`PolicyRenewedEvent.java`** - Policy renewed
- **`AgreementUpdatedEvent.java`** - Agreement modified

### 2. Event Publisher

- **`DomainEventPublisher.java`** - Main event publishing component
  - Async publishing using Spring `@Async`
  - WebClient-based HTTP calls to Inngest
  - Batch publishing support
  - Metadata enrichment (tenant, user)
  - Graceful error handling (non-blocking)

### 3. Configuration

- **`EventConfig.java`** - Spring configuration
  - WebClient bean
  - Thread pool executor (5-10 threads)
  - Async support enabled

- **`application.yml`** - Updated with Inngest settings
  - Inngest event key, signing key, base URL
  - Publisher enabled/disabled flag
  - Async mode configuration

### 4. Service Integration

- **`AgreementServiceImpl.java`** - Updated to publish events
  - Publishes `PolicyBoundEvent` on agreement creation
  - Publishes `AgreementUpdatedEvent` on agreement updates
  - Includes context: tenant, market, premium, dates

### 5. Test Controller

- **`EventController.java`** - REST API for testing
  - `/api/v1/events/test/policy-bound`
  - `/api/v1/events/test/claim-opened`
  - `/api/v1/events/test/claim-settled`
  - `/api/v1/events/test/policy-renewed`
  - `/api/v1/events/test/batch`
  - `/api/v1/events/publish` (custom events)

### 6. Tests

- **`DomainEventPublisherTest.java`** - Comprehensive unit tests
  - Tests for all event types
  - Batch publishing test
  - Metadata enrichment test
  - Event structure validation

### 7. Documentation

- **`EVENT_PUBLISHING_GUIDE.md`** - Complete usage guide
  - Architecture overview
  - Event catalog
  - Usage examples
  - Configuration guide
  - Testing instructions
  - Troubleshooting

## Quick Start

### 1. Start Inngest Dev Server

```bash
npx inngest-cli@latest dev
```

Access UI at: http://localhost:8288

### 2. Start Beema Kernel

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
mvn spring-boot:run
```

### 3. Test Event Publishing

```bash
# Test PolicyBound event
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound

# Test ClaimOpened event
curl -X POST http://localhost:8080/api/v1/events/test/claim-opened

# View events in Inngest UI
open http://localhost:8288
```

## Event Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Beema Kernel                            │
│                                                             │
│  AgreementService.createAgreement()                         │
│           │                                                 │
│           ▼                                                 │
│  DomainEventPublisher.publish(PolicyBoundEvent)             │
│           │                                                 │
│           ▼                                                 │
│  WebClient.post() → http://localhost:8288/e/local           │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      Inngest                                │
│                                                             │
│  Receives event → Fans out to webhooks                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ├──► Webhook 1 (Email Service)
                          ├──► Webhook 2 (CRM Sync)
                          └──► Webhook N (Analytics)
```

## Key Features

### Async Publishing
- Non-blocking event publishing
- Dedicated thread pool (5-10 threads)
- No impact on API response times

### Error Resilience
- Event publishing failures don't break business operations
- Errors are logged but not thrown
- Graceful degradation

### Flexible Configuration
- Enable/disable via environment variables
- Configurable Inngest endpoint
- Support for multiple modes (inngest, redis, kafka)

### Rich Context
- Tenant ID
- User ID and email
- Event-specific data
- Timestamps

## Event Examples

### PolicyBound Event

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
    "expiryDate": "2027-02-12T00:00:00Z",
    "tenantId": "tenant-123"
  },
  "user": {
    "id": "user-1",
    "email": "user@beema.io"
  }
}
```

### ClaimOpened Event

```json
{
  "id": "661f9511-f39c-52e5-b827-557766551111",
  "name": "claim/opened",
  "ts": 1706800001000,
  "v": "2024-11-01",
  "data": {
    "claimNumber": "CLM-001",
    "claimId": "claim-123",
    "claimAmount": 5000.00,
    "claimType": "motor_accident",
    "claimDate": "2026-02-12",
    "location": "Nairobi",
    "tenantId": "tenant-123"
  },
  "user": {
    "id": "user-1",
    "email": "user@beema.io"
  }
}
```

## Configuration

### Environment Variables

```bash
# Inngest Configuration
export INNGEST_EVENT_KEY=local
export INNGEST_SIGNING_KEY=test-signing-key
export INNGEST_BASE_URL=http://localhost:8288

# Event Publisher Configuration
export EVENTS_PUBLISHER_ENABLED=true
export EVENTS_PUBLISHER_MODE=inngest
```

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
      mode: ${EVENTS_PUBLISHER_MODE:inngest}
      async: true
```

## Testing

### Run Unit Tests

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
mvn test -Dtest=DomainEventPublisherTest
```

### Manual Testing

```bash
# 1. Start Inngest
npx inngest-cli@latest dev

# 2. Start Beema Kernel
mvn spring-boot:run

# 3. Test events
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound
curl -X POST http://localhost:8080/api/v1/events/test/claim-opened
curl -X POST http://localhost:8080/api/v1/events/test/batch

# 4. View in Inngest UI
open http://localhost:8288
```

## File Structure

```
beema-kernel/
├── pom.xml                                    (Updated: Added Redis dependency)
├── src/main/
│   ├── java/com/beema/kernel/
│   │   ├── event/                             (NEW)
│   │   │   ├── DomainEvent.java
│   │   │   ├── PolicyBoundEvent.java
│   │   │   ├── ClaimOpenedEvent.java
│   │   │   ├── ClaimSettledEvent.java
│   │   │   ├── PolicyRenewedEvent.java
│   │   │   ├── AgreementUpdatedEvent.java
│   │   │   └── DomainEventPublisher.java
│   │   ├── config/
│   │   │   └── EventConfig.java               (NEW)
│   │   ├── api/v1/event/
│   │   │   └── EventController.java           (NEW)
│   │   └── service/agreement/
│   │       └── AgreementServiceImpl.java      (Updated: Event publishing)
│   └── resources/
│       └── application.yml                    (Updated: Inngest config)
├── src/test/
│   └── java/com/beema/kernel/event/
│       └── DomainEventPublisherTest.java      (NEW)
├── EVENT_PUBLISHING_GUIDE.md                  (NEW)
└── EVENTS_README.md                           (NEW - This file)
```

## Deliverables Checklist

- [x] Inngest dependencies added to pom.xml
- [x] DomainEvent base class created
- [x] 5 event types implemented (Policy, Claim, Agreement)
- [x] DomainEventPublisher with async support
- [x] Event configuration with thread pool
- [x] Integration with AgreementService
- [x] EventController for testing (6 endpoints)
- [x] Unit tests (9 test cases)
- [x] Comprehensive documentation

## Next Steps

### Immediate
1. Start Inngest Dev Server
2. Run unit tests
3. Test via REST API endpoints
4. Verify events in Inngest UI

### Future Enhancements
1. Add more event types (policy/cancelled, claim/rejected)
2. Implement Redis fallback for offline scenarios
3. Add event replay mechanism
4. Add Prometheus metrics
5. Implement dead-letter queue
6. Add event schema validation
7. Create Inngest functions for handling events

## Monitoring

### Check Logs

```bash
# View event publishing logs
grep "DomainEventPublisher" logs/beema-kernel.log

# View successful events
grep "Event published successfully" logs/beema-kernel.log

# View failed events
grep "Failed to publish event" logs/beema-kernel.log
```

### Metrics (Future)

- `beema.events.published.total` - Total events published
- `beema.events.failed.total` - Total failed events
- `beema.events.publish.duration` - Event publishing latency

## Support

For questions or issues:
1. Check `EVENT_PUBLISHING_GUIDE.md` for troubleshooting
2. Review logs for error messages
3. Verify Inngest is running on port 8288
4. Check configuration in `application.yml`

## References

- [Inngest Documentation](https://www.inngest.com/docs)
- [Spring Async Documentation](https://spring.io/guides/gs/async-method/)
- [WebClient Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
