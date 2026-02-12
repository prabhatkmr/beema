# Message Hooks Control Stream

## Overview

The metadata-service implements a **Control Stream** that enables dynamic, real-time updates to message transformation hooks in the Flink processing pipeline. When message hooks are created, updated, or deleted via the REST API, changes are automatically broadcast to all Flink task instances.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Message Hooks Control Stream                  │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│  REST API        │      │  PostgreSQL      │      │  Kafka Topic     │
│  /message-hooks  │─────▶│  sys_message_    │─────▶│  message-hooks-  │
│                  │      │  hooks           │      │  control         │
└──────────────────┘      └──────────────────┘      └──────────────────┘
                                   │                          │
                                   │ NOTIFY                   │
                                   ▼                          ▼
                          ┌──────────────────┐      ┌──────────────────┐
                          │  MessageHook     │      │  Flink Broadcast │
                          │  EventPublisher  │      │  State Pattern   │
                          └──────────────────┘      └──────────────────┘
```

## Components

### 1. Database Table: `sys_message_hooks`

Stores JEXL transformation scripts for message processing:

```sql
CREATE TABLE sys_message_hooks (
    id UUID PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL UNIQUE,
    message_type VARCHAR(100) NOT NULL,
    script TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INT NOT NULL DEFAULT 0,
    description TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);
```

### 2. PostgreSQL Trigger

Automatically sends NOTIFY events when hooks change:

```sql
CREATE TRIGGER message_hook_change_trigger
    AFTER INSERT OR UPDATE OR DELETE ON sys_message_hooks
    FOR EACH ROW
    EXECUTE FUNCTION notify_message_hook_change();
```

### 3. MessageHookEventPublisher

Listens to PostgreSQL NOTIFY events and publishes to Kafka:

- Subscribes to PostgreSQL channel: `message_hook_changed`
- Parses notification payload
- Publishes `MessageHookMetadata` to Kafka topic: `message-hooks-control`

### 4. Kafka Control Topic

**Topic**: `message-hooks-control`
- **Partitions**: 1 (broadcast stream)
- **Replication Factor**: 1
- **Retention**: Infinite (`-1`)
- **Cleanup Policy**: `compact` (log compaction)

**Message Format**:
```json
{
  "hookId": "550e8400-e29b-41d4-a716-446655440000",
  "messageType": "SUBMISSION",
  "script": "{ 'premium': premium * 1.1, 'sumInsured': sumInsured }",
  "enabled": true,
  "operation": "UPDATE",
  "updatedAt": "2026-02-12T15:30:00Z"
}
```

### 5. Flink Broadcast Stream

The `beema-message-processor` consumes the control topic and updates broadcast state:

```java
BroadcastStream<MessageHookMetadata> broadcastStream =
    hookMetadataStream.broadcast(HOOK_DESCRIPTOR);

DataStream<TransformedMessage> transformedStream = parsedStream
    .connect(broadcastStream)
    .process(new JexlMessageTransformer(jexlService));
```

## REST API Endpoints

Base URL: `http://localhost:8082/api/v1/message-hooks`

### Create Hook

```bash
POST /api/v1/message-hooks
Content-Type: application/json

{
  "hookName": "premium-adjustment-hook",
  "messageType": "SUBMISSION",
  "script": "{ 'premium': premium * 1.1, 'sumInsured': sumInsured }",
  "enabled": true,
  "priority": 0,
  "description": "Adjusts premium by 10%"
}
```

### Update Hook

```bash
PUT /api/v1/message-hooks/{id}
Content-Type: application/json

{
  "hookName": "premium-adjustment-hook",
  "messageType": "SUBMISSION",
  "script": "{ 'premium': premium * 1.15, 'sumInsured': sumInsured }",
  "enabled": true,
  "priority": 0,
  "description": "Adjusts premium by 15%"
}
```

### Delete Hook

```bash
DELETE /api/v1/message-hooks/{id}
```

### Get All Hooks

```bash
GET /api/v1/message-hooks
```

### Get Hooks by Message Type

```bash
GET /api/v1/message-hooks/message-type/SUBMISSION?enabledOnly=true
```

### Republish Hook (Manual Sync)

```bash
POST /api/v1/message-hooks/{id}/republish
```

### Republish All Hooks (Bootstrap)

```bash
POST /api/v1/message-hooks/republish-all
```

## How It Works

### 1. Create/Update Hook

```
User → POST /message-hooks
     → MessageHookController
     → MessageHookService.createHook()
     → MessageHookRepository.save()
     → PostgreSQL INSERT
     → TRIGGER: notify_message_hook_change()
     → NOTIFY 'message_hook_changed'
     → MessageHookEventPublisher (listening)
     → KafkaTemplate.send("message-hooks-control", metadata)
     → Kafka Topic: message-hooks-control
     → Flink Broadcast Stream (all parallel instances)
     → Broadcast State Updated
     → Next messages use new hook automatically
```

### 2. Delete Hook

```
User → DELETE /message-hooks/{id}
     → MessageHookController
     → MessageHookService.deleteHook()
     → MessageHookRepository.delete()
     → PostgreSQL DELETE
     → TRIGGER: notify_message_hook_change()
     → NOTIFY 'message_hook_changed' (operation=DELETE)
     → MessageHookEventPublisher
     → KafkaTemplate.send("message-hooks-control", metadata)
     → Flink Broadcast Stream
     → Broadcast State: state.remove(messageType)
     → Hook disabled for all future messages
```

## Testing the Control Stream

### 1. Create a Hook

```bash
curl -X POST http://localhost:8082/api/v1/message-hooks \
  -H "Content-Type: application/json" \
  -d '{
    "hookName": "test-premium-hook",
    "messageType": "SUBMISSION",
    "script": "{ \"premium\": premium * 1.1, \"sumInsured\": sumInsured }",
    "enabled": true,
    "priority": 0,
    "description": "Test hook for premium adjustment"
  }'
```

### 2. Verify Kafka Message

```bash
docker exec -it beema-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic message-hooks-control \
  --from-beginning
```

Expected output:
```json
{
  "hookId": "...",
  "messageType": "SUBMISSION",
  "script": "{ \"premium\": premium * 1.1, \"sumInsured\": sumInsured }",
  "enabled": true,
  "operation": "INSERT",
  "updatedAt": "2026-02-12T15:30:00Z"
}
```

### 3. Verify Flink State

Check Flink Web UI: `http://localhost:8081`
- Navigate to Running Jobs → beema-message-processor
- Check Task Managers → State
- Verify broadcast state contains the new hook

### 4. Send Test Message

```bash
curl -X POST http://localhost:8080/api/v1/messages \
  -H "Content-Type: application/json" \
  -d '{
    "messageType": "SUBMISSION",
    "sourceSystem": "QUOTATION_ENGINE",
    "payload": {
      "premium": 1000,
      "sumInsured": 50000
    }
  }'
```

Expected transformation:
```json
{
  "premium": 1100,
  "sumInsured": 50000
}
```

## Configuration

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3

beema:
  kafka:
    control-topic: ${KAFKA_CONTROL_TOPIC:message-hooks-control}
```

### docker-compose.yml

```yaml
metadata-service:
  environment:
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    KAFKA_CONTROL_TOPIC: message-hooks-control
  depends_on:
    - kafka
```

## Advantages

1. **Zero Downtime**: Update transformation logic without restarting Flink jobs
2. **Instant Propagation**: Changes propagate to all Flink instances in real-time
3. **Consistency**: Broadcast state ensures all parallel instances use the same logic
4. **Auditing**: All changes tracked in sys_message_hooks table
5. **Recovery**: Republish-all endpoint can bootstrap Flink state after restart

## Monitoring

- **PostgreSQL NOTIFY**: Check `pg_stat_activity` for LISTEN connections
- **Kafka Lag**: Monitor `message-hooks-control` consumer group lag
- **Flink Checkpoints**: Verify broadcast state checkpointing
- **Application Logs**: MessageHookEventPublisher logs all published events

## Troubleshooting

### Hook Not Applied

1. Check if hook is enabled: `GET /api/v1/message-hooks/{id}`
2. Verify Kafka message: Consumer from `message-hooks-control` topic
3. Check Flink logs: Look for "Broadcast state updated" messages
4. Manually republish: `POST /api/v1/message-hooks/{id}/republish`

### PostgreSQL NOTIFY Not Working

1. Check listener status: `MessageHookEventPublisher` logs on startup
2. Verify trigger exists: `SELECT * FROM pg_trigger WHERE tgname = 'message_hook_change_trigger'`
3. Test manually: `UPDATE sys_message_hooks SET enabled = true WHERE id = '...'`
4. Check for errors in metadata-service logs

### Kafka Connection Issues

1. Verify Kafka is running: `docker ps | grep kafka`
2. Check bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` environment variable
3. Test producer: Use `kafka-console-producer` to send test messages
4. Check connectivity: `telnet kafka 29092`

## Future Enhancements

- [ ] Add JEXL syntax validation in REST API
- [ ] Support versioning for hooks (rollback capability)
- [ ] Add metrics for hook execution performance
- [ ] Implement A/B testing for hooks (percentage-based rollout)
- [ ] Add webhook notifications for hook changes
- [ ] Support composite hooks (multiple scripts per message type)
