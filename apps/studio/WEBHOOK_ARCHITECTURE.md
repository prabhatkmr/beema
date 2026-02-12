# Webhook Dispatcher Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Beema Webhook Dispatcher                          │
│                     Event-Driven Webhook Delivery System                 │
└─────────────────────────────────────────────────────────────────────────┘
```

## Component Architecture

```
┌───────────────────┐
│   beema-kernel    │  Domain Events Published
│   (Java/Spring)   │  ────────────────────────┐
└───────────────────┘                          │
                                               ▼
┌───────────────────┐                  ┌──────────────┐
│  External Events  │  ───────────────>│   Inngest    │  Event Bus
│  (API/Webhooks)   │                  │  Event Queue │
└───────────────────┘                  └──────┬───────┘
                                              │
                                              │ Triggers
                                              ▼
                                   ┌──────────────────────┐
                                   │  webhook-dispatcher  │  Inngest Function
                                   │    (TypeScript)      │
                                   └──────────┬───────────┘
                                              │
                         ┌────────────────────┼────────────────────┐
                         │                    │                    │
                         ▼                    ▼                    ▼
                    Step 1: Fetch      Step 2: Fan-out      Step 3: Record
                  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                  │  Query       │    │  Parallel    │    │  Delivery    │
                  │  sys_webhooks│    │  HTTP POSTs  │    │  Results     │
                  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
                         │                   │                    │
                         │                   │                    │
                         ▼                   ▼                    ▼
                  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                  │  PostgreSQL  │    │  User        │    │  sys_webhook │
                  │  Database    │    │  Webhooks    │    │  _deliveries │
                  └──────────────┘    └──────────────┘    └──────────────┘
```

## Data Flow

### 1. Event Publishing

```
┌─────────────┐     POST /events     ┌─────────────┐
│ beema-kernel│ ──────────────────> │   Inngest   │
│             │                      │  Event API  │
└─────────────┘                      └─────────────┘

Event Payload:
{
  name: "policy/bound",
  data: {
    policyNumber: "POL-001",
    agreementId: "AGR-001",
    tenantId: "tenant-1",
    ...
  },
  user: { id, email },
  timestamp: "2026-02-12T10:00:00Z"
}
```

### 2. Webhook Matching

```
┌─────────────────┐     SQL Query      ┌─────────────────┐
│ webhook-        │ ─────────────────> │  sys_webhooks   │
│ dispatcher      │                     │  (PostgreSQL)   │
└─────────────────┘                     └─────────────────┘

Query:
SELECT * FROM sys_webhooks
WHERE enabled = true
  AND tenant_id = 'tenant-1'
  AND (event_type = 'policy/bound' OR event_type = '*')

Returns: [webhook1, webhook2, ...]
```

### 3. Fan-out Delivery

```
┌─────────────────────────────────────────────────────────────┐
│                    Parallel Webhook Delivery                 │
└─────────────────────────────────────────────────────────────┘

webhook-dispatcher
        │
        ├──────────────────┬─────────────────┬───────────────────┐
        │                  │                 │                   │
        ▼                  ▼                 ▼                   ▼
   ┌────────┐        ┌────────┐       ┌────────┐         ┌────────┐
   │ Slack  │        │ Email  │       │ Custom │         │ Webhook│
   │ Webhook│        │ Service│       │  API   │         │ .site  │
   └────┬───┘        └────┬───┘       └────┬───┘         └────┬───┘
        │                 │                │                  │
   200 OK            200 OK           500 Error           200 OK
        │                 │                │                  │
        └─────────────────┴────────────────┴──────────────────┘
                              │
                              ▼
                    Record in sys_webhook_deliveries
```

### 4. Delivery Recording

```
┌─────────────────┐     INSERT     ┌─────────────────────┐
│ webhook-        │ ─────────────> │ sys_webhook_        │
│ dispatcher      │                │ deliveries          │
└─────────────────┘                └─────────────────────┘

Record:
{
  webhook_id: 1,
  event_id: "evt_123",
  event_type: "policy/bound",
  status: "success" | "failed",
  status_code: 200,
  response_body: "...",
  attempt_number: 1,
  delivered_at: "2026-02-12T10:00:05Z"
}
```

## Database Schema

### sys_webhooks

```
┌──────────────────────────────────────────────────────────────┐
│                        sys_webhooks                           │
├──────────────────┬───────────────────────────────────────────┤
│ webhook_id       │ BIGSERIAL PRIMARY KEY                     │
│ webhook_name     │ VARCHAR(255) NOT NULL                     │
│ tenant_id        │ VARCHAR(100) NOT NULL                     │
│ event_type       │ VARCHAR(255) NOT NULL                     │
│ url              │ VARCHAR(500) NOT NULL                     │
│ secret           │ VARCHAR(500) NOT NULL                     │
│ enabled          │ BOOLEAN DEFAULT true                      │
│ headers          │ JSONB DEFAULT '{}'                        │
│ retry_config     │ JSONB DEFAULT '{"maxAttempts":3,...}'     │
│ created_at       │ TIMESTAMPTZ DEFAULT now()                 │
│ updated_at       │ TIMESTAMPTZ DEFAULT now()                 │
│ created_by       │ VARCHAR(100) NOT NULL                     │
└──────────────────┴───────────────────────────────────────────┘

Indexes:
  - idx_webhooks_tenant (tenant_id)
  - idx_webhooks_event_type (event_type)
  - idx_webhooks_enabled (enabled) WHERE enabled = true
  - uq_webhook_tenant_name (tenant_id, webhook_name) UNIQUE
```

### sys_webhook_deliveries

```
┌──────────────────────────────────────────────────────────────┐
│                   sys_webhook_deliveries                      │
├──────────────────┬───────────────────────────────────────────┤
│ delivery_id      │ BIGSERIAL PRIMARY KEY                     │
│ webhook_id       │ BIGINT NOT NULL FK -> sys_webhooks        │
│ event_id         │ VARCHAR(100) NOT NULL                     │
│ event_type       │ VARCHAR(255) NOT NULL                     │
│ status           │ VARCHAR(50) NOT NULL                      │
│ status_code      │ INTEGER                                   │
│ response_body    │ TEXT                                      │
│ error_message    │ TEXT                                      │
│ attempt_number   │ INTEGER DEFAULT 1                         │
│ delivered_at     │ TIMESTAMPTZ DEFAULT now()                 │
└──────────────────┴───────────────────────────────────────────┘

Indexes:
  - idx_deliveries_webhook (webhook_id)
  - idx_deliveries_event (event_id)
  - idx_deliveries_status (status)
```

## API Architecture

### Studio API Routes

```
/api
  ├── /inngest (Inngest serve endpoint)
  │   ├── GET    - Inngest development UI
  │   ├── POST   - Inngest event handler
  │   └── PUT    - Inngest function registration
  │
  ├── /webhooks (Webhook CRUD)
  │   ├── GET    - List webhooks
  │   ├── POST   - Create webhook
  │   ├── PUT    - Update webhook
  │   └── DELETE - Delete webhook
  │
  ├── /webhooks/match (Webhook matching)
  │   └── POST   - Find webhooks for event type
  │
  └── /webhooks/deliveries (Delivery logs)
      ├── GET    - Query delivery history
      └── POST   - Record delivery results
```

### Request/Response Flow

```
Client Request
      │
      ▼
┌────────────────┐
│  Next.js App   │
│  (Studio)      │
└────────┬───────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌────────────────┐  ┌─────────────────┐
│  API Routes    │  │  Inngest        │
│  (REST)        │  │  Serve Route    │
└────────┬───────┘  └────────┬────────┘
         │                   │
         ▼                   ▼
┌────────────────┐  ┌─────────────────┐
│  PostgreSQL    │  │  Inngest        │
│  Database      │  │  Platform       │
└────────────────┘  └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  webhook-       │
                    │  dispatcher     │
                    └─────────────────┘
```

## Security Architecture

### HMAC Signature Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   HMAC Signature Generation                  │
└─────────────────────────────────────────────────────────────┘

Step 1: Prepare Payload
{
  event: "policy/bound",
  data: { ... },
  user: { ... },
  timestamp: "..."
}
      │
      ▼
Step 2: JSON Stringify
"{"event":"policy/bound","data":{...},...}"
      │
      ▼
Step 3: HMAC-SHA256 with Secret
crypto.createHmac('sha256', webhook.secret)
      │
      ▼
Step 4: Hex Digest
"sha256=a1b2c3d4e5f6..."
      │
      ▼
Step 5: Add to Headers
X-Beema-Signature: sha256=a1b2c3d4e5f6...
      │
      ▼
Step 6: Send to Webhook URL
```

### Verification Flow (Receiver Side)

```
┌─────────────────────────────────────────────────────────────┐
│                   Signature Verification                     │
└─────────────────────────────────────────────────────────────┘

Receive Request
      │
      ▼
Extract Signature from Header
X-Beema-Signature: sha256=a1b2c3d4e5f6...
      │
      ▼
Get Request Body (JSON String)
"{"event":"policy/bound",...}"
      │
      ▼
Compute Expected Signature
crypto.createHmac('sha256', stored_secret)
      │
      ▼
Compare Signatures (Constant-Time)
received_sig === expected_sig
      │
      ├─── YES ──> Process Webhook
      │
      └─── NO  ──> Reject (401 Unauthorized)
```

## Event Flow Timeline

```
T+0ms   Event Occurs (Policy Bound)
        │
        ▼
T+10ms  beema-kernel publishes to Inngest
        │
        ▼
T+20ms  Inngest receives event
        │
        ▼
T+25ms  webhook-dispatcher function triggered
        │
        ▼
T+30ms  Step 1: Query sys_webhooks
        │       (matches 3 webhooks)
        ▼
T+35ms  Step 2: Fan-out to webhooks (parallel)
        │
        ├────────────┬────────────┬────────────┐
        │            │            │            │
T+100ms ▼            ▼            ▼            ▼
        Webhook 1    Webhook 2    Webhook 3    Webhook 4
        (200 OK)     (200 OK)     (500 Error)  (200 OK)
        50ms         80ms         30ms         60ms
        │            │            │            │
        └────────────┴────────────┴────────────┘
                         │
T+150ms                  ▼
        Step 3: Record deliveries
        │
        ▼
T+160ms Function completes
```

## Error Handling & Retry Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Retry Strategy                            │
└─────────────────────────────────────────────────────────────┘

Attempt 1 (T+0s)
      │
      ├─── Success ──> Record & Exit
      │
      └─── Failure
            │
            ▼
Attempt 2 (T+1s) [Backoff: 1000ms]
      │
      ├─── Success ──> Record & Exit
      │
      └─── Failure
            │
            ▼
Attempt 3 (T+3s) [Backoff: 2000ms]
      │
      ├─── Success ──> Record & Exit
      │
      └─── Failure
            │
            ▼
Max Attempts Reached
      │
      ▼
Record as Failed & Exit
```

## Scalability Architecture

### Horizontal Scaling

```
┌─────────────────────────────────────────────────────────────┐
│                     Inngest Platform                         │
│                   (Handles Scaling)                          │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Distributes Work
                         │
      ┌──────────────────┼──────────────────┐
      │                  │                  │
      ▼                  ▼                  ▼
┌──────────┐      ┌──────────┐      ┌──────────┐
│ Instance │      │ Instance │      │ Instance │
│    1     │      │    2     │      │    3     │
└──────────┘      └──────────┘      └──────────┘

Each instance handles webhook-dispatcher function independently
```

### Load Distribution

```
Events: 1000/sec
        │
        ▼
Inngest Event Queue
        │
        ├─────────────────┬─────────────────┬──────────────┐
        │                 │                 │              │
        ▼                 ▼                 ▼              ▼
   250 events/sec   250 events/sec   250 events/sec   250 events/sec
   Instance 1       Instance 2       Instance 3       Instance 4
```

## Monitoring Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Observability Stack                      │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Inngest    │     │  PostgreSQL  │     │   Studio     │
│  Dashboard   │     │   Queries    │     │     Logs     │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │  Monitoring  │
                    │  Dashboard   │
                    └──────────────┘

Metrics:
  - Event throughput
  - Webhook success rate
  - Average latency
  - Error rate
  - Retry count
```

## Integration Points

### beema-kernel Integration

```
┌─────────────────┐
│  beema-kernel   │
│                 │
│  EventPublisher │────┐
│                 │    │
└─────────────────┘    │
                       │ inngest.send({...})
                       │
                       ▼
                ┌──────────────┐
                │   Inngest    │
                └──────────────┘
```

### External System Integration

```
┌─────────────────────────────────────────────────────────────┐
│              External Systems (Webhook Receivers)            │
└─────────────────────────────────────────────────────────────┘

Slack            Salesforce       Custom API       Email Service
  │                  │                 │                 │
  └──────────────────┼─────────────────┼─────────────────┘
                     │
                     ▼
          All receive webhooks from dispatcher
```

## Deployment Architecture

### Development

```
localhost:8288           localhost:3000           localhost:5432
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│   Inngest    │        │    Studio    │        │  PostgreSQL  │
│  Dev Server  │        │   Next.js    │        │   Database   │
└──────────────┘        └──────────────┘        └──────────────┘
```

### Production

```
inn.gs                  beema.com               AWS RDS
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│   Inngest    │        │    Studio    │        │  PostgreSQL  │
│  Cloud       │ <───>  │   (K8s Pod)  │ <───>  │   Cluster    │
└──────────────┘        └──────────────┘        └──────────────┘
```

## Summary

The webhook dispatcher is a robust, scalable event-driven system that:

1. **Receives** domain events from beema-kernel via Inngest
2. **Matches** webhooks based on event type and tenant
3. **Delivers** webhooks in parallel with HMAC signatures
4. **Records** delivery results for audit and monitoring
5. **Retries** failed deliveries automatically
6. **Scales** horizontally via Inngest platform

All components are designed for reliability, security, and observability.
