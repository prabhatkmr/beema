# Beema Temporal Workflow System - Complete Guide

## Overview

The Beema Temporal Workflow system provides a flexible, event-driven workflow orchestration platform for the insurance domain. It implements a **DSL interpreter pattern** where workflow hooks are stored in the database and interpreted at runtime.

### Key Features

- **Database-Driven DSL**: Workflow logic defined as hooks in PostgreSQL
- **JEXL Expression Evaluation**: Conditional logic using Apache Commons JEXL
- **Policy Snapshots**: Immutable point-in-time captures of agreements
- **Webhook Integration**: External system notifications
- **Docker & Kubernetes Ready**: Full deployment configurations included

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Beema Kernel Application                  │
│                                                              │
│  ┌────────────────┐         ┌─────────────────────┐        │
│  │ REST API       │────────▶│ WorkflowService     │        │
│  │ (Controllers)  │         │                     │        │
│  └────────────────┘         └──────────┬──────────┘        │
│                                        │                     │
│                                        ▼                     │
│                            ┌───────────────────────┐        │
│                            │   Temporal Client     │        │
│                            │   (Start Workflows)   │        │
│                            └───────────┬───────────┘        │
└────────────────────────────────────────┼───────────────────┘
                                         │
                                         ▼
                        ┌────────────────────────────────┐
                        │     Temporal Server            │
                        │   (Workflow Orchestration)     │
                        └────────────┬───────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Temporal Worker (Beema Kernel)            │
│                                                              │
│  ┌──────────────────┐         ┌──────────────────────┐     │
│  │ AgreementWorkflow│────────▶│ WorkflowActivities   │     │
│  │  (DSL Interpreter)│         │                      │     │
│  └──────────────────┘         └──────────┬───────────┘     │
│                                           │                  │
│                                           ▼                  │
│                              ┌────────────────────┐         │
│                              │ JexlExpressionEngine│        │
│                              │ (Condition Eval)   │         │
│                              └────────────────────┘         │
└─────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
                        ┌────────────────────────┐
                        │   PostgreSQL           │
                        │  - Workflow Hooks      │
                        │  - Executions          │
                        └────────────────────────┘
```

## Database Schema

### Workflow Hooks Table

Stores workflow hook configurations:

```sql
CREATE TABLE sys_workflow_hooks (
    hook_id BIGSERIAL PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    trigger_condition TEXT NOT NULL,           -- JEXL expression
    action_type VARCHAR(100) NOT NULL,         -- webhook, snapshot, expression
    action_config JSONB NOT NULL,              -- Action configuration
    execution_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

### Workflow Executions Table

Stores execution history:

```sql
CREATE TABLE sys_workflow_executions (
    execution_id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    agreement_id BIGINT,
    input_data JSONB,
    result_data JSONB,
    status VARCHAR(50) NOT NULL,              -- RUNNING, COMPLETED, FAILED
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);
```

## Workflow DSL Examples

### 1. Capture Policy Snapshot on Agreement Creation

```sql
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order
) VALUES (
    'capture_snapshot_on_agreement_created',
    'agreement.created',
    'agreement != null && agreement.agreementType != null',
    'snapshot',
    '{
        "endpoint": "/mock-policy-api/snapshots",
        "method": "POST",
        "timeout": 5000
    }'::jsonb,
    10
);
```

### 2. Webhook for High-Value Agreements

```sql
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order
) VALUES (
    'notify_high_value_agreement',
    'agreement.created',
    'agreement.premiumAmount != null && agreement.premiumAmount > 100000',
    'webhook',
    '{
        "url": "https://webhook.site/high-value-notification",
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "X-Beema-Event": "high-value-agreement"
        }
    }'::jsonb,
    20
);
```

### 3. Expression Evaluation for Risk Score

```sql
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order
) VALUES (
    'evaluate_risk_on_update',
    'agreement.updated',
    'agreement.status == "PENDING_REVIEW"',
    'expression',
    '{
        "expression": "agreement.riskFactors.size() * 10 + (agreement.premiumAmount / 1000)",
        "resultField": "calculatedRiskScore"
    }'::jsonb,
    15
);
```

### 4. London Market Placement Notification

```sql
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order
) VALUES (
    'london_market_placement_notification',
    'agreement.created',
    'agreement.marketType == "LONDON_MARKET" && agreement.placementType == "DIRECT"',
    'webhook',
    '{
        "url": "https://webhook.site/london-market-placement",
        "method": "POST",
        "headers": {
            "X-Market-Type": "LONDON_MARKET"
        }
    }'::jsonb,
    25
);
```

## API Usage

### Start Workflow (Async)

```bash
curl -X POST http://localhost:8080/api/workflows/start \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "agreement.created",
    "agreementData": {
      "agreementId": 12345,
      "agreementType": "POLICY",
      "premiumAmount": 150000,
      "marketType": "LONDON_MARKET",
      "placementType": "DIRECT",
      "lineOfBusiness": "COMMERCIAL"
    }
  }'
```

Response:
```json
{
  "success": true,
  "workflowId": "workflow-agreement.created-12345-1234567890-abc123",
  "eventType": "agreement.created"
}
```

### Start Workflow (Sync - for testing)

```bash
curl -X POST http://localhost:8080/api/workflows/start-sync \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "agreement.created",
    "agreementData": {
      "agreementId": 12345,
      "agreementType": "POLICY",
      "premiumAmount": 150000
    }
  }'
```

Response:
```json
{
  "success": true,
  "workflowId": "workflow-agreement.created-12345-1234567890-abc123",
  "eventType": "agreement.created",
  "result": {
    "status": "COMPLETED",
    "eventType": "agreement.created",
    "totalHooks": 2,
    "successfulActions": 2,
    "actionResults": [
      {
        "hookName": "capture_snapshot_on_agreement_created",
        "actionType": "snapshot",
        "conditionResult": true,
        "status": "SUCCESS",
        "result": {
          "snapshotId": "abc-123",
          "timestamp": "2026-02-12T10:30:00Z"
        }
      }
    ]
  }
}
```

### Get Workflow Status

```bash
curl http://localhost:8080/api/workflows/workflow-agreement.created-12345-1234567890-abc123/status
```

### Trigger Specific Events

```bash
# Agreement Created
curl -X POST http://localhost:8080/api/workflows/agreement/created \
  -H "Content-Type: application/json" \
  -d '{"agreementId": 12345, "agreementType": "POLICY", "premiumAmount": 100000}'

# Agreement Updated
curl -X POST http://localhost:8080/api/workflows/agreement/updated \
  -H "Content-Type: application/json" \
  -d '{"agreementId": 12345, "status": "PENDING_REVIEW"}'

# Agreement Endorsed
curl -X POST http://localhost:8080/api/workflows/agreement/endorsed \
  -H "Content-Type: application/json" \
  -d '{"agreementId": 12345, "endorsementId": 5678}'
```

## Docker Deployment

### Quick Start

```bash
# Start all services (PostgreSQL, Temporal, Temporal UI, Beema Kernel)
docker-compose up -d

# View logs
docker-compose logs -f beema-kernel

# Check service health
curl http://localhost:8080/actuator/health
```

### Access Services

- **Beema Kernel API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui
- **Temporal Web UI**: http://localhost:8088
- **Mock Policy API**: http://localhost:8080/mock-policy-api/snapshots/stats

### Test Workflow Execution

```bash
# 1. Start a workflow
curl -X POST http://localhost:8080/api/workflows/start-sync \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "agreement.created",
    "agreementData": {
      "agreementId": 12345,
      "agreementType": "POLICY",
      "premiumAmount": 150000,
      "marketType": "LONDON_MARKET",
      "placementType": "DIRECT"
    }
  }'

# 2. View execution in Temporal UI
# Open http://localhost:8088 and navigate to Workflows

# 3. Check policy snapshots
curl http://localhost:8080/mock-policy-api/snapshots/stats
```

## Kubernetes Deployment

### Prerequisites

1. Temporal server deployed in `temporal` namespace
2. PostgreSQL database accessible from cluster
3. Database credentials in secret `beema-db-credentials`

### Deploy Temporal Worker

```bash
# Install/upgrade Helm chart
helm upgrade --install beema-kernel ./platform \
  --namespace beema \
  --create-namespace \
  --values ./platform/values.yaml

# Verify deployment
kubectl get pods -n beema -l component=temporal-worker

# View logs
kubectl logs -n beema -l component=temporal-worker -f
```

### Configuration

Update `platform/values.yaml`:

```yaml
temporal:
  enabled: true

  server:
    host: "temporal-frontend.temporal.svc.cluster.local"
    port: 7233
    namespace: "default"

  worker:
    enabled: true
    replicaCount: 2

    config:
      maxConcurrentActivities: 10
      maxConcurrentWorkflows: 10

    autoscaling:
      enabled: true
      minReplicas: 2
      maxReplicas: 10
```

### Scaling Workers

```bash
# Manual scaling
kubectl scale deployment beema-kernel-temporal-worker -n beema --replicas=5

# Auto-scaling is configured via HPA
kubectl get hpa -n beema
```

## JEXL Expression Reference

### Available Variables

In trigger conditions, you have access to:

- `agreement` - The agreement data map
- `agreement.agreementId` - Agreement ID
- `agreement.premiumAmount` - Premium amount
- `agreement.marketType` - Market type (RETAIL, COMMERCIAL, LONDON_MARKET)
- `agreement.lineOfBusiness` - Line of business
- `agreement.status` - Agreement status
- All other fields in agreement data

### Expression Examples

```javascript
// Simple field check
agreement != null && agreement.agreementType != null

// Numeric comparison
agreement.premiumAmount > 100000

// String equality
agreement.status == "PENDING_REVIEW"

// Multiple conditions (AND)
agreement.marketType == "LONDON_MARKET" && agreement.placementType == "DIRECT"

// Multiple conditions (OR)
agreement.lineOfBusiness == "COMMERCIAL" || agreement.premiumAmount > 50000

// Complex calculation
agreement.premiumAmount >= agreement.coverageLimit * 0.1

// Null-safe navigation
agreement.riskFactors != null && agreement.riskFactors.size() > 5
```

## Action Types

### 1. Snapshot Action

Captures policy state via REST API.

```json
{
  "actionType": "snapshot",
  "actionConfig": {
    "endpoint": "/mock-policy-api/snapshots",
    "method": "POST",
    "includeEndorsement": true,
    "timeout": 5000
  }
}
```

### 2. Webhook Action

Calls external HTTP endpoint.

```json
{
  "actionType": "webhook",
  "actionConfig": {
    "url": "https://webhook.site/notification",
    "method": "POST",
    "headers": {
      "Content-Type": "application/json",
      "X-Custom-Header": "value"
    },
    "timeout": 10000
  }
}
```

### 3. Expression Action

Evaluates JEXL expression and stores result.

```json
{
  "actionType": "expression",
  "actionConfig": {
    "expression": "agreement.premiumAmount * 0.15",
    "resultField": "calculatedCommission",
    "description": "Calculate broker commission"
  }
}
```

### 4. Custom Logic Action

Placeholder for future custom business logic.

```json
{
  "actionType": "custom_logic",
  "actionConfig": {
    "logicType": "underwriting_approval",
    "parameters": {
      "threshold": 100000
    }
  }
}
```

## Monitoring & Observability

### Temporal Web UI

Access: http://localhost:8088

Features:
- View all workflow executions
- See workflow history and events
- Debug failed workflows
- Query workflows by status/time

### Application Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Health check
curl http://localhost:8080/actuator/health

# Flyway migrations
curl http://localhost:8080/actuator/flyway
```

### Database Queries

```sql
-- Recent workflow executions
SELECT workflow_id, event_type, status, started_at, completed_at
FROM sys_workflow_executions
ORDER BY started_at DESC
LIMIT 10;

-- Active workflow hooks
SELECT hook_name, event_type, action_type, enabled
FROM sys_workflow_hooks
WHERE enabled = true
ORDER BY execution_order;

-- Failed workflows
SELECT workflow_id, error_message, started_at
FROM sys_workflow_executions
WHERE status = 'FAILED'
ORDER BY started_at DESC;
```

## Testing

### Unit Tests

```bash
# Run workflow tests
./mvnw test -Dtest=AgreementWorkflowTest

# Run service tests
./mvnw test -Dtest=WorkflowServiceIntegrationTest
```

### Integration Tests

```bash
# Run all tests
./mvnw verify

# Run with coverage
./mvnw clean verify jacoco:report
```

## Troubleshooting

### Worker Not Connecting to Temporal

Check configuration:
```bash
# Verify Temporal host/port
kubectl exec -it deployment/beema-kernel-temporal-worker -n beema -- env | grep TEMPORAL

# Check Temporal server health
kubectl exec -it deployment/beema-kernel-temporal-worker -n beema -- \
  curl temporal-frontend.temporal.svc.cluster.local:7233
```

### Workflows Not Executing

1. Check worker is running: `kubectl get pods -n beema -l component=temporal-worker`
2. Check worker logs: `kubectl logs -n beema -l component=temporal-worker -f`
3. Verify hooks in database: `SELECT * FROM sys_workflow_hooks WHERE enabled = true`
4. Check Temporal UI for workflow status

### Database Connection Issues

```bash
# Test database connection
kubectl exec -it deployment/beema-kernel-temporal-worker -n beema -- \
  psql -h beema-postgresql -U beema -d beema_kernel -c "SELECT 1;"
```

## Best Practices

### 1. Workflow Hook Design

- Keep trigger conditions simple and fast to evaluate
- Use execution_order to control hook sequence
- Enable/disable hooks without code changes
- Document hooks with clear descriptions

### 2. Performance

- Use async workflow starts for non-blocking operations
- Set appropriate timeouts for webhook actions
- Monitor worker CPU/memory and scale as needed
- Use HPA for automatic scaling

### 3. Error Handling

- Configure retry policies in action_config
- Monitor failed workflows in Temporal UI
- Set up alerts for workflow failures
- Log all errors with context

### 4. Security

- Use HTTPS for webhook URLs
- Validate all input data
- Restrict JEXL expression capabilities
- Audit workflow executions

## Additional Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [JEXL Syntax Reference](https://commons.apache.org/proper/commons-jexl/reference/syntax.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes HPA](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)

## Support

For issues or questions:
1. Check logs: `docker-compose logs -f` or `kubectl logs -f`
2. Review Temporal UI: http://localhost:8088
3. Query database: Check sys_workflow_executions table
4. Enable debug logging: Set `logging.level.com.beema=DEBUG`
