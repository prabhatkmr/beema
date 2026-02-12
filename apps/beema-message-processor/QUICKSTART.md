# Quick Start Guide - Beema Message Processor

This guide will help you get the message processor up and running in under 5 minutes.

## Prerequisites

- Docker & Docker Compose installed
- Java 21 installed
- Maven 3.9+ installed

## Step 1: Build the Project

```bash
cd apps/beema-message-processor
mvn clean package -DskipTests
```

This creates a fat JAR at `target/beema-message-processor-0.1.0-SNAPSHOT.jar`.

## Step 2: Start Infrastructure

From the project root:

```bash
cd ../..  # Navigate to project root
docker-compose up -d postgres zookeeper kafka
```

Wait 30 seconds for services to be healthy:

```bash
docker-compose ps
```

## Step 3: Initialize Database

Run the Flyway migration to create `sys_message_hooks` table:

```bash
cd apps/beema-message-processor
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5433/beema_kernel \
  -Dflyway.user=beema \
  -Dflyway.password=beema
```

You should see:
```
Successfully validated 1 migration
Creating Schema History table...
Current version of schema: << Empty Schema >>
Migrating schema to version "11 - create message hooks"
Successfully applied 1 migration
```

## Step 4: Verify Sample Hooks

Connect to the database and verify sample hooks were created:

```bash
docker exec -it beema-postgres psql -U beema -d beema_kernel -c \
  "SELECT hook_id, hook_name, message_type, source_system FROM sys_message_hooks;"
```

You should see 6 sample hooks:
- `legacy_policy_transform` (policy_created, legacy_system)
- `partner_claim_transform` (claim_submitted, partner_api)
- `payment_gateway_transform` (payment_received, payment_gateway)
- `web_quote_transform` (quote_requested, web_portal)
- `london_market_slip_transform` (slip_created, london_market)
- `commercial_endorsement_transform` (endorsement_issued, commercial_system)

## Step 5: Start the Flink Job

Run the message processor locally:

```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=beema_kernel
export DB_USERNAME=beema
export DB_PASSWORD=beema

mvn exec:java -Dexec.mainClass=com.beema.processor.MessageProcessorJob
```

You should see:
```
INFO  MessageProcessorJob - Starting Beema Message Processor Job...
INFO  MessageProcessorJob - Configuration loaded: job='beema-message-processor'...
INFO  JexlTransformService - JexlTransformService initialized with sandboxed permissions
INFO  MessageProcessorJob - Executing Flink job: 'beema-message-processor'
```

## Step 6: Test the Pipeline

### Open a new terminal and produce a test message:

```bash
docker exec -it beema-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic raw-messages
```

Paste this test message (from sample-messages/policy-created-legacy.json):

```json
{"messageId":"msg-test-001","messageType":"policy_created","sourceSystem":"legacy_system","payload":{"policyRef":"pol-12345","customer":{"firstName":"John","lastName":"Doe"},"policy":{"premium":1000.00,"currency":"GBP","effectiveDate":"2026-01-01","expiryDate":"2027-01-01","productCode":"HOME-001","status":"ACTIVE"}},"timestamp":"2026-02-12T10:30:00Z"}
```

Press CTRL+D to send.

### In another terminal, consume from the processed-messages topic:

```bash
docker exec -it beema-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic processed-messages \
  --from-beginning \
  --property print.key=true
```

You should see the transformed message:

```json
{
  "messageId": "msg-test-001",
  "messageType": "policy_created",
  "sourceSystem": "legacy_system",
  "transformedData": {
    "policy_number": "POL-12345",
    "policy_holder_name": "John Doe",
    "premium_amount": 1000.0,
    "currency": "GBP",
    "effective_date": "2026-01-01",
    "expiry_date": "2027-01-01",
    "product_code": "HOME-001",
    "status": "ACTIVE"
  },
  "processedAt": "2026-02-12T...",
  "hookName": "legacy_policy_transform",
  "hookId": 1
}
```

## Step 7: Test Other Message Types

### Test Claim Submission (Partner API):

```bash
docker exec -it beema-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic raw-messages
```

Paste:
```json
{"messageId":"msg-test-002","messageType":"claim_submitted","sourceSystem":"partner_api","payload":{"claimId":"CLM-98765","policyNumber":"POL-12345","claim":{"amount":5000.00,"submittedDate":"2026-02-10","incidentDate":"2026-02-05","type":"property_damage","description":"Water damage in kitchen"},"claimant":{"name":"John Doe"}},"timestamp":"2026-02-10T14:22:00Z"}
```

### Test London Market Slip:

```json
{"messageId":"msg-test-003","messageType":"slip_created","sourceSystem":"london_market","payload":{"slipNumber":"SLIP-2026-00123","umr":"B1234XYZ2026001","slip":{"leadUnderwriter":"Lloyd's Syndicate 1234","totalPremium":500000.00,"totalLine":100,"currency":"GBP","inceptionDate":"2026-03-01","expiryDate":"2027-03-01","riskCode":"MARINE-HULL","status":"QUOTED"},"broker":{"name":"ABC Brokers Ltd"}},"timestamp":"2026-02-11T09:15:00Z"}
```

## Step 8: Monitor with Flink Web UI

Access the Flink Web UI at: http://localhost:8081

You'll see:
- Job status and uptime
- Task parallelism
- Checkpoints
- Kafka consumer lag
- Throughput metrics

## Cleanup

Stop all services:

```bash
cd ../..  # Project root
docker-compose down -v
```

## Next Steps

1. **Add custom hooks**: See README.md "Adding New Message Hooks" section
2. **Test JEXL expressions**: Run unit tests with `mvn test`
3. **Deploy to Kubernetes**: See README.md "Deploying to Kubernetes" section
4. **Monitor in production**: Configure Prometheus/Grafana for Flink metrics

## Troubleshooting

### "No hook found" in logs
- Check the message_type and source_system match exactly
- Verify hook is enabled: `enabled = true`
- Query: `SELECT * FROM sys_message_hooks WHERE message_type = 'your_type';`

### Kafka connection refused
- Verify Kafka is running: `docker ps | grep kafka`
- Check bootstrap servers env var: `echo $KAFKA_BOOTSTRAP_SERVERS`
- Test: `docker exec beema-kafka kafka-broker-api-versions --bootstrap-server localhost:9092`

### Database connection error
- Verify postgres is running: `docker ps | grep postgres`
- Test connection: `docker exec beema-postgres pg_isready -U beema`

### JEXL evaluation error
- Check expression syntax in the sys_message_hooks table
- Test expressions: Run `JexlTransformServiceTest.java`
- Review logs for detailed error messages

## Example Hooks for Different Contexts

### Retail Insurance
```sql
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping)
VALUES (
  'retail_motor_transform',
  'motor_quote_requested',
  'web_portal',
  'message.vehicle.value * message.rating.factor',
  '{"quote_ref": {"jexl": "message.quoteRef"}, ...}'::jsonb
);
```

### Commercial Insurance
```sql
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping)
VALUES (
  'commercial_property_transform',
  'property_policy_created',
  'commercial_system',
  'message.buildings.sumInsured + message.contents.sumInsured',
  '{"total_si": {"jexl": "message.buildings.sumInsured + message.contents.sumInsured"}, ...}'::jsonb
);
```

### London Market
```sql
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping)
VALUES (
  'london_market_bordereaux_transform',
  'bordereaux_received',
  'london_market',
  'message.premiums.total',
  '{"umr": {"jexl": "message.umr"}, ...}'::jsonb
);
```

Happy streaming!
