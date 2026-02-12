# Temporal Integration Summary

This document summarizes the Temporal workflow orchestration integration into the Beema platform via Docker Compose.

## Overview

The Beema platform now includes a complete Temporal workflow orchestration setup that enables:
- **PolicyWorkflow**: Orchestrates policy lifecycle (creation, activation, renewal, cancellation)
- **Asynchronous processing**: Long-running tasks via activities
- **Reliable execution**: Automatic retries, timeouts, and error handling
- **Visibility**: Full workflow history and state via Temporal UI

## Service Configuration

### 1. Temporal Server

```yaml
Service Name: temporal
Image: temporalio/auto-setup:1.25.2
Container: beema-temporal
Port: 7233 (gRPC)
Database: PostgreSQL (beema database)
Health Check: tctl cluster health
```

**Key Features**:
- Auto-setup image (creates database schema automatically)
- Uses shared PostgreSQL instance
- Production-ready with persistence
- Health check ensures server is ready before worker starts

**Environment Variables**:
```yaml
DB: postgresql
DB_PORT: 5432
POSTGRES_USER: beema
POSTGRES_PWD: beema
POSTGRES_SEEDS: postgres
```

### 2. Temporal Web UI

```yaml
Service Name: temporal-ui
Image: temporalio/ui:2.32.0
Container: beema-temporal-ui
Port: 8088 (mapped to container port 8080)
Access: http://localhost:8088
```

**Key Features**:
- Modern web interface for monitoring workflows
- Real-time workflow execution visibility
- Search and filter capabilities
- Workflow history and stack traces
- CORS enabled for Studio integration

**Environment Variables**:
```yaml
TEMPORAL_ADDRESS: temporal:7233
TEMPORAL_CORS_ORIGINS: http://localhost:3000
```

### 3. Beema Kernel (with Temporal Worker)

```yaml
Service Name: beema-kernel
Container: beema-kernel
Port: 8080
Temporal Worker: Embedded (enabled)
Task Queue: POLICY_TASK_QUEUE
```

**Temporal Configuration**:
```yaml
TEMPORAL_HOST: temporal
TEMPORAL_PORT: 7233
TEMPORAL_NAMESPACE: default
TEMPORAL_WORKER_ENABLED: true
TEMPORAL_TASK_QUEUE: POLICY_TASK_QUEUE
TEMPORAL_MAX_CONCURRENT_ACTIVITIES: 10
TEMPORAL_MAX_CONCURRENT_WORKFLOWS: 10
```

**Dependencies**:
- Depends on `temporal` service (condition: healthy)
- Depends on `postgres` service (condition: healthy)
- Depends on `keycloak` service (condition: healthy)
- Depends on `metadata-service` service (condition: healthy)

## Service Startup Order

The dependency chain ensures services start in the correct order:

```
postgres (healthy)
    ↓
temporal (healthy)
    ↓
beema-kernel (healthy, worker starts)
    ↓
temporal-ui (monitoring ready)
```

**Timing**:
1. PostgreSQL: ~10 seconds
2. Temporal Server: ~30 seconds (includes schema setup)
3. Beema Kernel: ~60 seconds (includes worker registration)
4. Temporal UI: ~5 seconds

**Total startup time**: Approximately 2 minutes for complete stack

## Network Configuration

All services run on the same Docker network: `beema-network`

**Internal Communication**:
- Beema Kernel → Temporal: `temporal:7233`
- Temporal → PostgreSQL: `postgres:5432`
- Temporal UI → Temporal: `temporal:7233`

**External Access**:
- Temporal gRPC: `localhost:7233`
- Temporal UI: `http://localhost:8088`
- Beema Kernel API: `http://localhost:8080`

## Volume Configuration

Persistent storage for Temporal:

```yaml
volumes:
  postgres_data:      # PostgreSQL data (includes Temporal schemas)
  temporal_data:      # Temporal configuration
```

**Data Persistence**:
- Workflow executions: Stored in PostgreSQL
- Workflow history: Stored in PostgreSQL
- Configuration: Stored in temporal_data volume

## Workflow Architecture

### PolicyWorkflow Implementation

The `PolicyWorkflow` is implemented with the following structure:

```java
// Workflow Definition
@WorkflowInterface
public interface PolicyWorkflow {
    @WorkflowMethod
    PolicyResponse createPolicy(PolicyRequest request);
}

// Workflow Implementation
public class PolicyWorkflowImpl implements PolicyWorkflow {
    private final PolicyActivities activities;

    @Override
    public PolicyResponse createPolicy(PolicyRequest request) {
        // Activities are executed by the worker
        activities.validatePolicy(request);
        activities.calculatePremium(request);
        activities.persistPolicy(request);
        activities.notifyCustomer(request);
        return new PolicyResponse();
    }
}
```

### Activity Implementation

Activities are the building blocks of workflows:

```java
@ActivityInterface
public interface PolicyActivities {
    void validatePolicy(PolicyRequest request);
    void calculatePremium(PolicyRequest request);
    void persistPolicy(PolicyRequest request);
    void notifyCustomer(PolicyRequest request);
}
```

**Activity Configuration**:
- Retry policy: Exponential backoff
- Timeout: 30 seconds per activity
- Max attempts: 3
- Error handling: Specific exceptions trigger different retry behaviors

### Task Queue Configuration

**Task Queue Name**: `POLICY_TASK_QUEUE`

**Purpose**:
- Separates policy workflows from other workflow types
- Enables dedicated worker pools for policy operations
- Allows scaling of policy processing independently

**Configuration**:
```properties
temporal.task-queue=POLICY_TASK_QUEUE
temporal.max-concurrent-workflows=10
temporal.max-concurrent-activities=10
```

## Integration with Beema Services

### 1. Database Integration

Temporal shares the PostgreSQL instance with Beema Kernel:

```sql
-- Temporal creates these schemas automatically:
CREATE SCHEMA temporal;
CREATE SCHEMA temporal_visibility;

-- Beema Kernel uses:
CREATE SCHEMA public;  -- beema_kernel database
```

**Tables**:
- `temporal.executions`: Workflow execution state
- `temporal.executions_visibility`: Workflow search index
- `temporal_visibility.executions`: Advanced search
- `public.*`: Beema domain entities (policies, claims, etc.)

### 2. Metadata Service Integration

Workflows can access metadata via activities:

```java
@Override
public void validatePolicy(PolicyRequest request) {
    // Activity can call Metadata Service
    MetadataResponse metadata = metadataClient.getProductMetadata(
        request.getProductType()
    );
    // Use metadata for validation
}
```

**Environment**:
```yaml
METADATA_SERVICE_URL: http://metadata-service:8082
```

### 3. OAuth2 Integration

Temporal operations respect OAuth2 security:

```yaml
OAUTH2_ISSUER_URI: http://keycloak:8080/realms/beema
OAUTH2_JWK_SET_URI: http://keycloak:8080/realms/beema/protocol/openid-connect/certs
```

Activities can include OAuth2 tokens for external API calls.

### 4. Kafka Integration

Workflows can publish events to Kafka:

```java
@Override
public void notifyCustomer(PolicyRequest request) {
    // Activity publishes to Kafka
    kafkaProducer.send(
        "processed-messages",
        new PolicyCreatedEvent(request)
    );
}
```

## Testing the Integration

### 1. Verify Temporal Server

```bash
# Check Temporal health
docker exec beema-temporal tctl --address temporal:7233 cluster health

# Expected output:
# SERVING
```

### 2. Verify Worker Registration

```bash
# Check beema-kernel logs for worker startup
docker logs beema-kernel 2>&1 | grep -i "temporal worker"

# Expected output:
# Started Temporal worker for task queue: POLICY_TASK_QUEUE
```

### 3. Create a Policy (Triggers Workflow)

```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-2026-TEST-001",
    "productType": "MOTOR_COMPREHENSIVE",
    "insuredName": "Test Customer",
    "coverageAmount": 500000.00,
    "effectiveDate": "2026-02-15T00:00:00Z",
    "expiryDate": "2027-02-15T00:00:00Z",
    "premium": 12500.00
  }'
```

### 4. View Workflow in Temporal UI

1. Open http://localhost:8088
2. Navigate to "Workflows"
3. Look for workflow with policy number in workflow ID
4. Click to see execution history
5. View activities, inputs, outputs, and any errors

### 5. Query Workflow Status

```bash
# Using tctl
tctl --address localhost:7233 workflow describe \
  -w policy-POL-2026-TEST-001

# Using Temporal UI
# Navigate to: http://localhost:8088/namespaces/default/workflows
```

## Monitoring and Observability

### Temporal UI Dashboard

Access: http://localhost:8088

**Features**:
- **Workflows**: View all workflow executions
  - Filter by status (running, completed, failed)
  - Search by workflow ID or type
  - View execution timeline

- **Task Queues**: Monitor worker pools
  - `POLICY_TASK_QUEUE`: Policy workflow workers
  - Poller status and backlog

- **Workflow Details**:
  - Input/output payloads
  - Activity history
  - Event history
  - Stack traces for failures

### Metrics and Logs

**Beema Kernel Logs**:
```bash
# Watch workflow execution logs
docker logs -f beema-kernel | grep -i workflow

# Watch activity execution
docker logs -f beema-kernel | grep -i activity
```

**Temporal Server Logs**:
```bash
# Watch Temporal server logs
docker logs -f beema-temporal

# Filter for specific namespace
docker logs beema-temporal | grep "namespace=default"
```

### Health Checks

**Temporal Server**:
```bash
# Via Docker healthcheck
docker inspect beema-temporal --format='{{.State.Health.Status}}'

# Via tctl
tctl --address localhost:7233 cluster health
```

**Worker Status**:
```bash
# Check worker is registered to task queue
tctl --address localhost:7233 task-queue describe \
  --task-queue POLICY_TASK_QUEUE
```

## Troubleshooting

### Problem: Workflows Not Executing

**Symptoms**:
- Policy creation returns success but workflow doesn't appear in Temporal UI
- "No worker available" errors

**Diagnosis**:
```bash
# Check worker is running
docker logs beema-kernel | grep "Started Temporal worker"

# Check task queue
tctl --address localhost:7233 task-queue describe \
  --task-queue POLICY_TASK_QUEUE

# Expected: At least 1 poller registered
```

**Solution**:
```bash
# Restart beema-kernel
docker-compose restart beema-kernel

# Verify TEMPORAL_WORKER_ENABLED=true
docker exec beema-kernel env | grep TEMPORAL_WORKER_ENABLED
```

### Problem: Activities Timing Out

**Symptoms**:
- Activities fail with timeout errors
- Workflow retries activities repeatedly

**Diagnosis**:
```bash
# View workflow in Temporal UI
# Look for activity timeout events in history

# Check activity execution time
docker logs beema-kernel | grep -i "activity.*duration"
```

**Solution**:
- Increase activity timeout in workflow code
- Optimize activity implementation
- Check external service dependencies (database, APIs)

### Problem: Temporal Server Won't Start

**Symptoms**:
- `beema-temporal` container exits immediately
- Database connection errors in logs

**Diagnosis**:
```bash
# Check Temporal logs
docker logs beema-temporal

# Check PostgreSQL is healthy
docker inspect beema-postgres --format='{{.State.Health.Status}}'

# Verify database connectivity
docker exec beema-temporal nc -zv postgres 5432
```

**Solution**:
```bash
# Ensure postgres is running first
docker-compose up -d postgres

# Wait for postgres to be healthy
docker-compose ps postgres

# Then start temporal
docker-compose up -d temporal
```

### Problem: UI Shows No Workflows

**Symptoms**:
- Temporal UI loads but shows empty workflow list
- Workflows are executing (visible in logs)

**Diagnosis**:
```bash
# Check namespace
# Default namespace should be visible in UI

# Query workflows via tctl
tctl --address localhost:7233 workflow list
```

**Solution**:
- Ensure namespace is set to "default" in UI dropdown
- Check time range filter (expand to "All time")
- Verify TEMPORAL_ADDRESS in temporal-ui service

## Performance Tuning

### Worker Tuning

Adjust concurrent execution limits:

```yaml
# In docker-compose.yml
environment:
  TEMPORAL_MAX_CONCURRENT_ACTIVITIES: 20  # Increase from 10
  TEMPORAL_MAX_CONCURRENT_WORKFLOWS: 20   # Increase from 10
```

**Trade-offs**:
- Higher concurrency = more throughput
- Higher concurrency = more resource usage (CPU, memory, connections)

### Database Tuning

For high-throughput scenarios:

```yaml
# In docker-compose.yml
postgres:
  environment:
    POSTGRES_MAX_CONNECTIONS: 200  # Default: 100
  deploy:
    resources:
      limits:
        memory: 2G
```

### Temporal Server Tuning

For production workloads:

```yaml
temporal:
  environment:
    - TEMPORAL_BROADCAST_ADDRESS=0.0.0.0
    - NUM_HISTORY_SHARDS=512  # Default: 4
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: '2'
```

## Production Recommendations

### 1. Separate Temporal Database

Use a dedicated database for Temporal:

```yaml
temporal:
  environment:
    - POSTGRES_SEEDS=temporal-postgres  # Separate instance
```

### 2. Enable TLS

Configure Temporal with TLS for production:

```yaml
temporal:
  environment:
    - TLS_ENABLED=true
    - TLS_CERT_FILE=/certs/server.crt
    - TLS_KEY_FILE=/certs/server.key
  volumes:
    - ./certs:/certs
```

### 3. Separate Worker Service

Run worker as dedicated service for scalability:

```yaml
beema-temporal-worker:
  build:
    context: ./beema-kernel
  environment:
    - SPRING_PROFILES_ACTIVE=worker  # Worker-only profile
    - TEMPORAL_WORKER_ENABLED=true
  deploy:
    replicas: 3  # Scale workers independently
```

### 4. Monitoring Stack

Add Prometheus and Grafana:

```yaml
prometheus:
  image: prom/prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml

grafana:
  image: grafana/grafana
  ports:
    - "3001:3000"
```

## Summary

### What Was Configured

✅ **Temporal Server**: Fully configured with PostgreSQL persistence
✅ **Temporal UI**: Web interface for workflow monitoring
✅ **Temporal Worker**: Embedded in Beema Kernel with proper configuration
✅ **Task Queue**: Dedicated `POLICY_TASK_QUEUE` for policy workflows
✅ **Network**: All services on shared `beema-network`
✅ **Health Checks**: Proper dependency chain and health monitoring
✅ **Volumes**: Persistent storage for workflows and configuration

### Integration Points

✅ **Database**: Shared PostgreSQL instance with separate schemas
✅ **Metadata Service**: Accessible from workflow activities
✅ **OAuth2**: Keycloak integration for security
✅ **Kafka**: Event publishing from workflow activities
✅ **Studio**: Frontend integration with CORS enabled

### Files Created/Updated

1. `/Users/prabhatkumar/Desktop/dev-directory/beema/docker-compose.yml` (updated)
2. `/Users/prabhatkumar/Desktop/dev-directory/beema/.env.example` (created)
3. `/Users/prabhatkumar/Desktop/dev-directory/beema/docker-compose.README.md` (created)
4. `/Users/prabhatkumar/Desktop/dev-directory/beema/docker-compose-verify.sh` (created)
5. `/Users/prabhatkumar/Desktop/dev-directory/beema/DOCKER_SETUP.md` (created)
6. `/Users/prabhatkumar/Desktop/dev-directory/beema/TEMPORAL_INTEGRATION_SUMMARY.md` (this file)

### Next Steps

1. **Start Services**: `docker-compose up -d`
2. **Verify Setup**: `./docker-compose-verify.sh`
3. **Test Workflow**: Create a policy via API
4. **Monitor**: View execution in Temporal UI (http://localhost:8088)
5. **Develop**: Implement additional workflows and activities

---

**Integration Status**: ✅ Complete
**Last Updated**: 2026-02-12
**Version**: 1.0.0
