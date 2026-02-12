# Policy Workflow Guide

## Overview

The Policy Workflow is a Temporal-based workflow that manages the complete lifecycle of insurance policies from submission through issuance. It provides reliable, retryable operations for policy processing with built-in fault tolerance.

## Architecture

### Components

1. **PolicyWorkflow** - Main workflow interface and implementation
2. **PolicySnapshotActivity** - Retryable activities for external operations
3. **PolicyWorkflowController** - REST API for workflow management
4. **Model Classes** - Request/Response DTOs

### Technology Stack

- Temporal SDK 1.25.2
- Spring Boot 3.4.1
- Java 21
- PostgreSQL (for persistence)

## Policy Lifecycle Diagram

```
┌─────────────┐
│  SUBMITTED  │ ← Initial State
└──────┬──────┘
       │
       │ 1. Validate Policy Data
       │ 2. Retrieve Policy Snapshot (retryable)
       │
       ▼
┌─────────────┐
│   ISSUED    │ ← Final State
└──────┬──────┘
       │
       │ 3. Store Snapshot
       │ 4. Notify Parties
       │
       ▼
┌─────────────┐
│  COMPLETED  │
└─────────────┘
```

## State Transitions

### SUBMITTED → ISSUED

**Triggers**: Automatic after successful validation and snapshot retrieval

**Operations**:
1. Validate policy data against business rules
2. Retrieve policy snapshot from policy system (with retries)
3. Transition to ISSUED state
4. Store policy snapshot to persistent storage
5. Send notifications (email, webhook, etc.)

**Failure Handling**:
- Validation failures result in VALIDATION_FAILED state
- Snapshot retrieval failures are automatically retried up to 5 times with exponential backoff
- Storage failures will cause workflow to fail and can be manually retried

### Manual State Updates

States can be updated manually via signals:
- SUBMITTED → CANCELLED
- ISSUED → CANCELLED
- Any custom state transitions as needed

## API Reference

### Base URL
```
http://localhost:8080/api/v1/workflows/policy
```

### Endpoints

#### 1. Start Policy Workflow

**POST** `/start`

Initiates a new policy lifecycle workflow.

**Request Body**:
```json
{
  "policyId": "POL-12345",
  "initialState": "SUBMITTED",
  "metadata": {
    "premium": 1000.0,
    "coverage": "Full Coverage",
    "effectiveDate": "2026-03-01"
  }
}
```

**Response** (201 Created):
```json
{
  "workflowId": "policy-workflow-POL-12345-a1b2c3d4",
  "policyId": "POL-12345",
  "message": "Workflow started successfully",
  "success": true
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/v1/workflows/policy/start \
  -H "Content-Type: application/json" \
  -d '{
    "policyId": "POL-12345",
    "initialState": "SUBMITTED",
    "metadata": {
      "premium": 1000.0,
      "coverage": "Full Coverage"
    }
  }'
```

#### 2. Update Policy State (Signal)

**POST** `/{workflowId}/signal`

Sends a signal to update the state of a running workflow.

**Request Body**:
```json
{
  "newState": "CANCELLED",
  "reason": "Customer requested cancellation"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "State update signal sent successfully",
  "workflowId": "policy-workflow-POL-12345-a1b2c3d4",
  "newState": "CANCELLED"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/v1/workflows/policy/policy-workflow-POL-12345-a1b2c3d4/signal \
  -H "Content-Type: application/json" \
  -d '{
    "newState": "CANCELLED",
    "reason": "Customer requested"
  }'
```

#### 3. Query Current State

**GET** `/{workflowId}/state`

Queries the current state of a running workflow without blocking.

**Response** (200 OK):
```json
{
  "workflowId": "policy-workflow-POL-12345-a1b2c3d4",
  "currentState": "ISSUED",
  "success": true
}
```

**cURL Example**:
```bash
curl http://localhost:8080/api/v1/workflows/policy/policy-workflow-POL-12345-a1b2c3d4/state
```

#### 4. Get Workflow Result

**GET** `/{workflowId}/result?timeoutSeconds=30`

Retrieves the result of a completed workflow. This is a blocking call that waits for workflow completion.

**Query Parameters**:
- `timeoutSeconds` (optional, default: 30) - Maximum time to wait for result

**Response** (200 OK):
```json
{
  "finalState": "ISSUED",
  "snapshotId": "SNAP-a1b2c3d4",
  "timestamp": "2026-02-12T10:30:00Z",
  "success": true,
  "message": "Policy workflow completed successfully"
}
```

**cURL Example**:
```bash
curl "http://localhost:8080/api/v1/workflows/policy/policy-workflow-POL-12345-a1b2c3d4/result?timeoutSeconds=60"
```

## Retry Configuration

### Activity Retry Options

Activities are configured with automatic retry for transient failures:

```java
RetryOptions retryOptions = RetryOptions.newBuilder()
    .setInitialInterval(Duration.ofSeconds(1))      // First retry after 1s
    .setMaximumInterval(Duration.ofSeconds(30))     // Max backoff of 30s
    .setBackoffCoefficient(2.0)                     // Double interval each retry
    .setMaximumAttempts(5)                          // Up to 5 attempts
    .build();
```

**Retry Schedule Example**:
1. Attempt 1: Immediate
2. Attempt 2: After 1 second
3. Attempt 3: After 2 seconds
4. Attempt 4: After 4 seconds
5. Attempt 5: After 8 seconds

Total max retry time: ~15 seconds

### Activity Timeouts

```yaml
temporal:
  activity:
    schedule-to-close-timeout: PT5M  # Total time including retries
    start-to-close-timeout: PT2M     # Single execution timeout
```

## Error Handling

### Validation Errors

**Cause**: Invalid policy data fails business rule validation

**Result**: Workflow completes with `VALIDATION_FAILED` state

**Resolution**:
1. Review validation errors in workflow result
2. Fix policy data
3. Start new workflow with corrected data

### Activity Failures

**Cause**: Temporary failures in external systems (network, service unavailable)

**Behavior**: Automatic retry with exponential backoff

**Resolution**: Usually self-healing; monitor Temporal UI for persistent failures

### Workflow Timeouts

**Cause**: Workflow exceeds execution timeout (default: 1 hour)

**Result**: Workflow fails with timeout error

**Resolution**:
1. Check for stuck activities in Temporal UI
2. Cancel and restart workflow if necessary
3. Increase timeout if legitimate long-running process

## Monitoring Workflows

### Temporal UI

Access Temporal UI at: `http://localhost:8233`

**Key Metrics**:
- Workflow execution history
- Activity retry attempts
- Pending activities
- Workflow timings

### Application Logs

Check logs for workflow execution details:

```bash
# View workflow logs
tail -f logs/beema-kernel.log | grep PolicyWorkflow

# View activity logs
tail -f logs/beema-kernel.log | grep PolicySnapshotActivity
```

### Prometheus Metrics

Key metrics exposed at `/actuator/prometheus`:
- `temporal_workflow_started_total`
- `temporal_workflow_completed_total`
- `temporal_workflow_failed_total`
- `temporal_activity_execution_total`

## Testing Guide

### Unit Tests

Run workflow unit tests with Temporal test environment:

```bash
cd apps/beema-kernel
mvn test -Dtest=PolicyWorkflowTest
```

**Test Cases**:
- Happy path: SUBMITTED → ISSUED
- Activity retry on failure
- State query during execution
- Signal handling
- Validation failures

### Integration Tests

Run activity integration tests:

```bash
mvn test -Dtest=PolicySnapshotActivityTest
```

**Test Cases**:
- Retrieve policy snapshot
- Store policy snapshot (idempotency)
- Send notifications (idempotency)
- Error handling

### Manual Testing

#### Prerequisites
1. Start Temporal server:
   ```bash
   temporal server start-dev
   ```

2. Start beema-kernel:
   ```bash
   mvn spring-boot:run
   ```

#### Test Workflow Execution

```bash
# 1. Start workflow
WORKFLOW_ID=$(curl -X POST http://localhost:8080/api/v1/workflows/policy/start \
  -H "Content-Type: application/json" \
  -d '{
    "policyId": "POL-TEST-001",
    "initialState": "SUBMITTED",
    "metadata": {"premium": 1000.0}
  }' | jq -r '.workflowId')

echo "Started workflow: $WORKFLOW_ID"

# 2. Query state
curl http://localhost:8080/api/v1/workflows/policy/$WORKFLOW_ID/state | jq

# 3. Get result (blocking)
curl "http://localhost:8080/api/v1/workflows/policy/$WORKFLOW_ID/result?timeoutSeconds=60" | jq
```

#### Test Signal Handling

```bash
# Start workflow
WORKFLOW_ID=$(curl -X POST http://localhost:8080/api/v1/workflows/policy/start \
  -H "Content-Type: application/json" \
  -d '{
    "policyId": "POL-TEST-002",
    "initialState": "SUBMITTED",
    "metadata": {}
  }' | jq -r '.workflowId')

# Send signal to update state
curl -X POST http://localhost:8080/api/v1/workflows/policy/$WORKFLOW_ID/signal \
  -H "Content-Type: application/json" \
  -d '{
    "newState": "CANCELLED",
    "reason": "Test cancellation"
  }' | jq

# Verify state changed
curl http://localhost:8080/api/v1/workflows/policy/$WORKFLOW_ID/state | jq
```

## Configuration

### application.yml

```yaml
temporal:
  service:
    host: localhost
    port: 7233
  namespace: default
  worker:
    enabled: true
    max-concurrent-activities: 10
    max-concurrent-workflows: 10
  task-queues:
    policy: POLICY_TASK_QUEUE
  workflow:
    execution-timeout: PT1H
  activity:
    schedule-to-close-timeout: PT5M
    start-to-close-timeout: PT2M
```

### Environment Variables

Override configuration with environment variables:

```bash
export TEMPORAL_HOST=temporal.production.com
export TEMPORAL_PORT=7233
export TEMPORAL_NAMESPACE=production
export TEMPORAL_WORKER_ENABLED=true
export TEMPORAL_MAX_CONCURRENT_ACTIVITIES=20
export TEMPORAL_MAX_CONCURRENT_WORKFLOWS=20
```

## Failure Scenarios

### Scenario 1: Network Timeout During Snapshot Retrieval

**What Happens**:
1. Activity times out on first attempt
2. Temporal automatically retries with backoff
3. After 5 attempts, workflow fails if still unsuccessful

**Recovery**:
- Check network connectivity
- Verify policy service availability
- Manually retry workflow from Temporal UI or restart via API

### Scenario 2: Database Unavailable During Snapshot Storage

**What Happens**:
1. Activity fails with database connection error
2. Temporal retries activity
3. Workflow fails if database remains unavailable

**Recovery**:
- Restore database connectivity
- Workflow will automatically resume from last successful activity

### Scenario 3: Notification Service Down

**What Happens**:
1. Notification activity fails
2. Temporal retries notification
3. Workflow may complete without successful notification

**Recovery**:
- Fix notification service
- Manually trigger notification for affected policies
- Consider making notifications non-critical (log warning instead of failing)

## Best Practices

### Development

1. **Always test with Temporal test environment** before deploying
2. **Make activities idempotent** - they may be retried
3. **Use workflow logger** (`Workflow.getLogger()`) for deterministic logging
4. **Keep workflow code deterministic** - no random(), System.currentTimeMillis(), etc.
5. **Set appropriate timeouts** based on expected operation duration

### Production

1. **Monitor workflow execution rates** and failure patterns
2. **Set up alerts** for workflow failures
3. **Use appropriate retry policies** for different activity types
4. **Implement circuit breakers** for external service calls
5. **Version workflows** when making breaking changes
6. **Archive completed workflows** periodically to manage storage

### Security

1. **Validate input data** before starting workflows
2. **Sanitize policy data** to prevent injection attacks
3. **Use authentication** for workflow API endpoints
4. **Encrypt sensitive data** in workflow state
5. **Audit workflow executions** for compliance

## Troubleshooting

### Workflow Not Starting

**Check**:
- Temporal server is running: `curl http://localhost:8233`
- Worker is registered: Check logs for "Policy Worker configured successfully"
- Task queue name matches: `POLICY_TASK_QUEUE`

### Activity Stuck in Retry Loop

**Check**:
- Activity logs for error details
- External service availability
- Timeout configuration
- Network connectivity

**Fix**:
- Cancel workflow from Temporal UI if unrecoverable
- Fix underlying issue
- Restart workflow with corrected configuration

### Workflow Result Not Available

**Check**:
- Workflow is complete: Query state endpoint
- Workflow ID is correct
- Timeout is sufficient for workflow completion

**Fix**:
- Increase timeout parameter
- Check workflow execution in Temporal UI
- Verify workflow completed successfully

## Additional Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [Spring Boot Temporal Integration](https://github.com/temporalio/sdk-java)
- [Beema Platform Architecture](../README.md)

## Support

For issues or questions:
- Create an issue in the project repository
- Contact the platform team
- Check Temporal community forums
