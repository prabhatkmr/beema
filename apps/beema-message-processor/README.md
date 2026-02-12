# Beema Message Processor

A Flink streaming job that transforms external messages to Beema's internal format using JEXL-based transformation hooks.

## Architecture Overview

```
┌─────────────┐      ┌──────────────────────┐      ┌──────────────────┐
│   Kafka     │─────▶│  Message Processor   │─────▶│     Kafka        │
│ raw-messages│      │  (Flink + JEXL)      │      │processed-messages│
└─────────────┘      └──────────────────────┘      └──────────────────┘
                              │
                              │ queries
                              ▼
                     ┌──────────────────┐
                     │   PostgreSQL     │
                     │ sys_message_hooks│
                     └──────────────────┘
```

### Components

1. **Kafka Source**: Reads raw messages from `raw-messages` topic
2. **MessageTransformer**: Applies JEXL transformation hooks from database
3. **JexlTransformService**: Evaluates JEXL expressions (reuses beema-kernel pattern)
4. **MessageHookRepository**: Fetches transformation rules from `sys_message_hooks` table
5. **Kafka Sink**: Writes transformed messages to `processed-messages` topic

## Message Transformation Flow

1. External system publishes a raw message to Kafka `raw-messages` topic:
   ```json
   {
     "messageId": "msg-001",
     "messageType": "policy_created",
     "sourceSystem": "legacy_system",
     "payload": {
       "policyRef": "pol-12345",
       "customer": {
         "firstName": "John",
         "lastName": "Doe"
       },
       "policy": {
         "premium": 1000.00,
         "currency": "GBP"
       }
     },
     "timestamp": "2026-02-12T10:30:00Z"
   }
   ```

2. Processor looks up matching hook in `sys_message_hooks` table:
   - Matches by `message_type` and `source_system`
   - Selects hook with highest priority (lowest priority value)

3. JEXL transformations are applied using `field_mapping`:
   ```json
   {
     "policy_number": {"jexl": "message.policyRef.toUpperCase()"},
     "policy_holder_name": {"jexl": "message.customer.firstName + ' ' + message.customer.lastName"},
     "premium_amount": {"jexl": "message.policy.premium * 1.05"}
   }
   ```

4. Transformed message is written to `processed-messages` topic:
   ```json
   {
     "messageId": "msg-001",
     "messageType": "policy_created",
     "sourceSystem": "legacy_system",
     "transformedData": {
       "policy_number": "POL-12345",
       "policy_holder_name": "John Doe",
       "premium_amount": 1050.00
     },
     "processedAt": "2026-02-12T10:30:01Z",
     "hookName": "legacy_policy_transform",
     "hookId": 1
   }
   ```

## Adding New Message Hooks

### Database Schema

The `sys_message_hooks` table stores transformation rules:

```sql
CREATE TABLE sys_message_hooks (
    hook_id BIGSERIAL PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL,
    message_type VARCHAR(100) NOT NULL,       -- e.g., 'policy_created'
    source_system VARCHAR(100) NOT NULL,      -- e.g., 'legacy_system'
    jexl_transform TEXT NOT NULL,             -- Main JEXL validation expression
    field_mapping JSONB NOT NULL,             -- Field-level JEXL mappings
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INT NOT NULL DEFAULT 100,        -- Lower = higher priority
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Example: Adding a New Hook

```sql
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    jexl_transform,
    field_mapping,
    description
) VALUES (
    'partner_claim_transform',
    'claim_submitted',
    'partner_api',
    'message.claim.amount * 1.0',
    '{
        "claim_number": {"jexl": "message.claimId"},
        "policy_number": {"jexl": "message.policyNumber"},
        "claim_amount": {"jexl": "message.claim.amount"},
        "claim_date": {"jexl": "message.claim.submittedDate"},
        "status": {"jexl": \"''SUBMITTED''\""}
    }'::jsonb,
    'Transforms partner API claim submissions to Beema internal format'
);
```

## JEXL Expression Examples

### Basic Arithmetic
```javascript
message.policy.premium * 1.05                    // Add 5% to premium
message.coverage.sumInsured * message.risk.ratingFactor
```

### String Operations
```javascript
message.policyRef.toUpperCase()                  // Uppercase transformation
message.customer.firstName + ' ' + message.customer.lastName
message.customer.email.toLowerCase()
```

### Conditional Logic
```javascript
message.payment.status == 'SUCCESS' ? 'COMPLETED' : 'PENDING'
message.policy.currency != null ? message.policy.currency : 'GBP'
```

### Complex Calculations (London Market)
```javascript
message.slip.totalPremium * (message.slip.totalLine / 100.0)  // Calculate net premium
```

### Null Safety
JEXL is configured in silent mode, so null values propagate safely:
```javascript
message.customer.firstName + ' ' + message.customer.lastName  // Works even if lastName is null
```

## Running Locally with Docker Compose

### Prerequisites
- Docker and Docker Compose
- Maven 3.9+
- Java 21+

### Build and Run

1. Build the project:
   ```bash
   cd apps/beema-message-processor
   mvn clean package -DskipTests
   ```

2. Start the infrastructure:
   ```bash
   cd ../../  # Back to project root
   docker-compose up -d postgres kafka zookeeper
   ```

3. Run database migrations:
   ```bash
   cd apps/beema-message-processor
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5433/beema_kernel \
                       -Dflyway.user=beema \
                       -Dflyway.password=beema
   ```

4. Start the Flink job locally:
   ```bash
   mvn exec:java -Dexec.mainClass=com.beema.processor.MessageProcessorJob
   ```

### Testing the Pipeline

1. Produce a test message to Kafka:
   ```bash
   docker exec -it beema-kafka kafka-console-producer \
     --bootstrap-server localhost:9092 \
     --topic raw-messages
   ```

   Paste this message:
   ```json
   {
     "messageId": "test-001",
     "messageType": "policy_created",
     "sourceSystem": "legacy_system",
     "payload": {
       "policyRef": "pol-99999",
       "customer": {"firstName": "Jane", "lastName": "Smith"},
       "policy": {"premium": 2000.00, "currency": "GBP"}
     },
     "timestamp": "2026-02-12T10:00:00Z"
   }
   ```

2. Consume from processed topic:
   ```bash
   docker exec -it beema-kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic processed-messages \
     --from-beginning
   ```

## Deploying to Kubernetes

### Build Docker Image

```bash
cd apps/beema-message-processor
docker build -t beema-message-processor:latest .
```

### Deploy with Helm

1. Create ConfigMap with environment variables:
   ```yaml
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: message-processor-config
   data:
     KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
     DB_HOST: "postgres-service"
     DB_PORT: "5432"
     DB_NAME: "beema_kernel"
     FLINK_PARALLELISM: "2"
   ```

2. Deploy Flink job:
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: beema-message-processor
   spec:
     replicas: 1
     selector:
       matchLabels:
         app: message-processor
     template:
       metadata:
         labels:
           app: message-processor
       spec:
         containers:
         - name: flink-job
           image: beema-message-processor:latest
           envFrom:
           - configMapRef:
               name: message-processor-config
           env:
           - name: DB_USERNAME
             valueFrom:
               secretKeyRef:
                 name: postgres-secret
                 key: username
           - name: DB_PASSWORD
             valueFrom:
               secretKeyRef:
                 name: postgres-secret
                 key: password
           ports:
           - containerPort: 8081
   ```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker addresses |
| `KAFKA_GROUP_ID` | `beema-message-processor` | Consumer group ID |
| `KAFKA_SOURCE_TOPIC` | `raw-messages` | Input topic name |
| `KAFKA_SINK_TOPIC` | `processed-messages` | Output topic name |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5433` | PostgreSQL port |
| `DB_NAME` | `beema_kernel` | Database name |
| `DB_USERNAME` | `beema` | Database username |
| `DB_PASSWORD` | `beema` | Database password |
| `FLINK_JOB_NAME` | `beema-message-processor` | Job name |
| `FLINK_PARALLELISM` | `1` | Job parallelism |
| `FLINK_CHECKPOINT_INTERVAL` | `60000` | Checkpoint interval (ms) |

## Testing

### Unit Tests

Run unit tests for JEXL transformations:
```bash
mvn test
```

### Integration Tests

Integration tests use Testcontainers for embedded Kafka and PostgreSQL:
```bash
mvn verify
```

## Monitoring

### Flink Web UI

Access at `http://localhost:8081` when running locally.

Metrics available:
- Messages processed per second
- Transformation success/failure rate
- Kafka consumer lag
- Checkpoint duration

### Logs

Structured JSON logging is available for production:
```bash
docker logs beema-message-processor
```

## Unified Platform Support

This service supports all Beema contexts:

- **Retail Insurance**: Standard policy and claim transformations
- **Commercial Insurance**: Endorsements, renewals, multi-line policies
- **London Market**: Slip creation, syndicate subscriptions, UMR handling

Sample hooks for all contexts are included in the migration file `V11__create_message_hooks.sql`.

## Troubleshooting

### No hook found for message
- Check `sys_message_hooks` table has matching `message_type` and `source_system`
- Verify hook is enabled: `enabled = true`
- Check logs: `docker logs beema-message-processor | grep "No hook found"`

### JEXL evaluation error
- Validate JEXL syntax: Use `JexlTransformService.isValidJexlSyntax()`
- Check for null values in payload
- Review sandbox restrictions (no java.io, java.net, etc.)

### Kafka connection issues
- Verify Kafka is running: `docker ps | grep kafka`
- Check bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS` environment variable
- Test connectivity: `telnet kafka 9092`

## Development

### Project Structure

```
apps/beema-message-processor/
├── pom.xml                         # Maven dependencies
├── package.json                    # Turborepo integration
├── Dockerfile                      # Multi-stage Docker build
├── README.md                       # This file
└── src/
    ├── main/
    │   ├── java/com/beema/processor/
    │   │   ├── MessageProcessorJob.java       # Main Flink job
    │   │   ├── config/
    │   │   │   ├── KafkaConfig.java
    │   │   │   ├── FlinkConfig.java
    │   │   │   └── DatabaseConfig.java
    │   │   ├── model/
    │   │   │   ├── RawMessage.java
    │   │   │   ├── ProcessedMessage.java
    │   │   │   └── MessageHook.java
    │   │   ├── processor/
    │   │   │   ├── MessageTransformer.java    # Flink MapFunction
    │   │   │   └── HookApplier.java           # Hook application logic
    │   │   ├── repository/
    │   │   │   └── MessageHookRepository.java # Database access
    │   │   └── service/
    │   │       └── JexlTransformService.java  # JEXL evaluation
    │   └── resources/
    │       ├── application.yml
    │       ├── logback.xml
    │       └── db/migration/
    │           └── V11__create_message_hooks.sql
    └── test/
        └── java/com/beema/processor/
            ├── processor/
            │   └── HookApplierTest.java
            └── service/
                └── JexlTransformServiceTest.java
```

## References

- [Apache Flink Documentation](https://flink.apache.org/)
- [Apache JEXL Documentation](https://commons.apache.org/proper/commons-jexl/)
- [Kafka Connector for Flink](https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/datastream/kafka/)
- [Beema Kernel - JexlExpressionEngine](../beema-kernel/src/main/java/com/beema/kernel/service/expression/JexlExpressionEngine.java)
