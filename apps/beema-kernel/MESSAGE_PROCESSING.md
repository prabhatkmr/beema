# Message Processing Architecture

## Overview

The Beema Kernel message processing system provides a flexible, JEXL-based pipeline for transforming messages from various source systems. The architecture supports three processing stages:

1. **Pre-processing**: Data validation, normalization, and enrichment
2. **Transformation**: Field mapping and conversion
3. **Post-processing**: Calculated fields, audit logging, and notifications

## Table of Contents

- [Architecture](#architecture)
- [Hook Execution Lifecycle](#hook-execution-lifecycle)
- [JEXL Script Reference](#jexl-script-reference)
- [Error Handling Strategies](#error-handling-strategies)
- [Retry Configuration](#retry-configuration)
- [Performance Considerations](#performance-considerations)
- [Adding New Message Types](#adding-new-message-types)
- [Common Scenarios](#common-scenarios)

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Message Source Systems                    │
│              (Retail, Commercial, London Market)             │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    MessageHookController                     │
│                      (REST API Layer)                        │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  MessageProcessingPipeline                   │
│                     (Builder Pattern)                        │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  MessageProcessingService                    │
│            (Orchestrates Hook Execution)                     │
└─────────┬──────────────────┬──────────────────┬─────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
│  Pre-processing  │ │Transformation│ │ Post-processing  │
│  JEXL Scripts    │ │ JEXL Scripts │ │  JEXL Scripts    │
└──────────────────┘ └──────────────┘ └──────────────────┘
          │                  │                  │
          └──────────────────┴──────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   JexlExpressionEngine                       │
│              (Sandboxed JEXL Execution)                      │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              MessageProcessingExecution                      │
│                  (Audit Trail)                               │
└─────────────────────────────────────────────────────────────┘
```

### Database Schema

#### sys_message_hooks

Stores message transformation hook configurations.

| Column                   | Type         | Description                                    |
|--------------------------|--------------|------------------------------------------------|
| hook_id                  | BIGSERIAL    | Primary key                                    |
| hook_name                | VARCHAR(255) | Unique hook identifier                         |
| message_type             | VARCHAR(100) | Message type (e.g., policy.created)            |
| source_system            | VARCHAR(100) | Source system identifier                       |
| target_system            | VARCHAR(100) | Target system identifier (optional)            |
| preprocessing_jexl       | TEXT         | Pre-processing JEXL script                     |
| preprocessing_order      | INTEGER      | Pre-processing execution order                 |
| transformation_jexl      | TEXT         | Transformation JEXL script (required)          |
| transformation_order     | INTEGER      | Transformation execution order                 |
| postprocessing_jexl      | TEXT         | Post-processing JEXL script                    |
| postprocessing_order     | INTEGER      | Post-processing execution order                |
| error_handling_strategy  | VARCHAR(50)  | fail_fast, log_continue, or retry              |
| retry_config             | JSONB        | Retry policy configuration                     |
| metadata                 | JSONB        | Additional metadata                            |
| enabled                  | BOOLEAN      | Hook enabled flag                              |

#### sys_message_processing_executions

Audit trail for message processing executions.

| Column             | Type         | Description                           |
|--------------------|--------------|---------------------------------------|
| execution_id       | BIGSERIAL    | Primary key                           |
| hook_id            | BIGINT       | Foreign key to sys_message_hooks      |
| message_type       | VARCHAR(100) | Message type                          |
| source_system      | VARCHAR(100) | Source system                         |
| processing_stage   | VARCHAR(50)  | preprocessing, transformation, postprocessing |
| input_data         | JSONB        | Input message data                    |
| output_data        | JSONB        | Output result data                    |
| status             | VARCHAR(50)  | SUCCESS, FAILED, RETRYING             |
| error_message      | TEXT         | Error message if failed               |
| execution_time_ms  | INTEGER      | Execution time in milliseconds        |
| attempt_number     | INTEGER      | Attempt number for retries            |

## Hook Execution Lifecycle

### 1. Message Receipt

```java
POST /api/v1/message-hooks/process
{
  "messageType": "policy.created",
  "sourceSystem": "retail_system",
  "message": {
    "policyNumber": "ABC123",
    "customerName": "John Doe",
    "premiumAmount": 1000
  }
}
```

### 2. Hook Resolution

The system queries `sys_message_hooks` for enabled hooks matching:
- `message_type = "policy.created"`
- `source_system = "retail_system"`
- `enabled = true`

Hooks are sorted by execution order.

### 3. Pre-processing Stage

Executes `preprocessing_jexl` scripts in order of `preprocessing_order`.

**Purpose**: Validate, normalize, and enrich message data.

**Context Variables**:
- `message`: The input message (mutable)
- `context`: Execution context metadata

**Example**:
```javascript
// Validate required fields
if (message.policyNumber == null) {
    throw "Missing required field: policyNumber";
}

// Normalize data
message.policyNumber = message.policyNumber.trim().toUpperCase();
message.customerName = message.customerName.trim();

// Enrich with lookup data
var productDb = {
    "HOME-001": {"name": "Home Insurance", "basePremium": 500}
};
message.productDetails = productDb[message.productCode];
```

### 4. Transformation Stage

Executes `transformation_jexl` scripts in order of `transformation_order`.

**Purpose**: Map and transform message fields to target structure.

**Context Variables**:
- `message`: The input message (immutable at this stage)
- `result`: The transformation result (mutable)
- `context`: Execution context metadata

**Example**:
```javascript
{
    "agreementId": message.policyNumber,
    "agreementType": "POLICY",
    "marketContext": "RETAIL",
    "customer": {
        "name": message.customerName,
        "email": message.email
    },
    "premium": {
        "amount": message.premiumAmount,
        "currency": "GBP"
    }
}
```

### 5. Post-processing Stage

Executes `postprocessing_jexl` scripts in order of `postprocessing_order`.

**Purpose**: Add calculated fields, audit metadata, and trigger notifications.

**Context Variables**:
- `message`: The original input message
- `result`: The transformation result (mutable)
- `context`: Execution context metadata

**Example**:
```javascript
// Add audit metadata
result.processedAt = new("java.time.Instant").now().toString();
result.processedBy = "beema-kernel";

// Calculate derived fields
if (result.premium.frequency == "MONTHLY") {
    result.premium.annualAmount = result.premium.amount * 12;
}

// Add notifications
if (result.premium.amount > 100000) {
    result.notifications = [{
        "type": "high_value_policy",
        "recipient": "underwriting_team",
        "priority": "high"
    }];
}
```

### 6. Execution Recording

Each stage execution is recorded in `sys_message_processing_executions` with:
- Input/output data
- Execution time
- Success/failure status
- Error messages and stack traces

## JEXL Script Reference

### Available Variables

#### Pre-processing Context
- `message`: Input message (Map<String, Object>)
- `context`: Execution context
  - `messageType`: String
  - `sourceSystem`: String
  - `targetSystem`: String
  - `receivedAt`: Instant
  - `attemptNumber`: Integer

#### Transformation Context
- `message`: Input message (read-only)
- `context`: Execution context

#### Post-processing Context
- `message`: Original input message
- `result`: Transformation result (Map<String, Object>)
- `context`: Execution context

### Allowed Java Classes

The JEXL sandbox allows:
- `java.time.Instant` - Timestamps
- `java.math.BigDecimal` - Decimal calculations
- `Math` - Mathematical operations
- String, Number, Boolean - Primitives
- Map, List, Set - Collections

**Blocked for security**:
- `System`, `Runtime`, `Process`
- File I/O classes
- Network classes
- Reflection APIs

### Common Operations

#### String Manipulation
```javascript
// Trim and uppercase
message.policyNumber = message.policyNumber.trim().toUpperCase();

// Lowercase email
message.email = message.email.toLowerCase();

// Concatenation
result.fullName = message.firstName + " " + message.lastName;
```

#### Null Handling
```javascript
// Null-safe access
message.email = message.email != null ? message.email.trim() : null;

// Default values
result.currency = message.currency != null ? message.currency : "GBP";
```

#### Conditional Logic
```javascript
if (message.premiumAmount > 100000) {
    result.requiresApproval = true;
    result.approvalLevel = "SENIOR_UNDERWRITER";
} else {
    result.requiresApproval = false;
    result.approvalLevel = "STANDARD";
}
```

#### Loops and Iteration
```javascript
// Iterate over array
var riskFactors = message.riskFactors;
var totalRisk = 0;
for (var factor : riskFactors) {
    totalRisk = totalRisk + factor.score;
}
result.totalRiskScore = totalRisk;
```

#### Throwing Errors
```javascript
// Validation error
if (message.policyNumber == null) {
    throw "Missing required field: policyNumber";
}

// Business rule violation
if (message.premiumAmount < 0) {
    throw "Premium amount cannot be negative";
}
```

## Error Handling Strategies

### fail_fast

Stops processing immediately on first error. Recommended for critical validations.

```sql
error_handling_strategy = 'fail_fast'
```

**Use cases**:
- Required field validation
- Business rule violations
- Data integrity checks

**Behavior**:
- Pipeline stops at first error
- No subsequent stages execute
- Error returned to caller

### log_continue

Logs error but continues processing. Useful for non-critical enrichment.

```sql
error_handling_strategy = 'log_continue'
```

**Use cases**:
- Optional data enrichment
- Non-critical lookups
- Audit logging

**Behavior**:
- Error logged to execution history
- Processing continues to next stage
- Partial success possible

### retry

Retries failed operations with exponential backoff.

```sql
error_handling_strategy = 'retry'
retry_config = '{
    "maxAttempts": 3,
    "backoffMs": 1000,
    "backoffMultiplier": 2.0
}'
```

**Use cases**:
- Transient failures
- External service calls
- Network timeouts

**Behavior**:
- Retries up to `maxAttempts` times
- Waits `backoffMs * (backoffMultiplier ^ attempt)` between retries
- Records each attempt in execution history

## Retry Configuration

### Parameters

| Parameter          | Type    | Default | Description                                |
|--------------------|---------|---------|-------------------------------------------|
| maxAttempts        | Integer | 3       | Maximum number of retry attempts          |
| backoffMs          | Long    | 1000    | Initial backoff delay in milliseconds     |
| backoffMultiplier  | Double  | 2.0     | Multiplier for exponential backoff        |

### Example

```json
{
  "maxAttempts": 5,
  "backoffMs": 2000,
  "backoffMultiplier": 1.5
}
```

**Retry timeline**:
1. Attempt 1: Immediate
2. Attempt 2: After 2000ms
3. Attempt 3: After 3000ms (2000 * 1.5)
4. Attempt 4: After 4500ms (3000 * 1.5)
5. Attempt 5: After 6750ms (4500 * 1.5)

## Performance Considerations

### Script Optimization

1. **Minimize complexity**: Keep JEXL scripts simple and focused
2. **Avoid nested loops**: Use efficient algorithms
3. **Cache lookups**: Store frequently accessed data in metadata
4. **Limit string operations**: String concatenation can be expensive

### Execution Timeout

Default maximum execution time: **5000ms** per hook

Configure in `application.yml`:
```yaml
beema:
  message-processing:
    max-hook-execution-time-ms: 5000
```

### Database Indexing

Key indexes for performance:
- `idx_message_hooks_type_source` - Hook lookup by message type and source
- `idx_message_hooks_preprocessing_order` - Ordered hook execution
- `idx_message_executions_started_at` - Execution history queries

### Monitoring

Enable metrics and tracing:
```yaml
beema:
  message-processing:
    enable-metrics: true
    enable-tracing: true
```

Metrics available:
- Hook execution count
- Execution duration (p50, p95, p99)
- Success/failure rates
- Retry attempts

## Adding New Message Types

### Step 1: Define Hook Configuration

```sql
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    preprocessing_jexl,
    transformation_jexl,
    postprocessing_jexl,
    error_handling_strategy,
    retry_config,
    enabled
) VALUES (
    'my_new_message_type',
    'claim.submitted',
    'claims_system',
    '-- validation script --',
    '-- transformation script --',
    '-- postprocessing script --',
    'fail_fast',
    '{"maxAttempts": 3}'::jsonb,
    true
);
```

### Step 2: Test Hook

```bash
POST /api/v1/message-hooks/{id}/test
{
  "claimNumber": "CLM001",
  "claimant": "John Doe",
  "amount": 5000
}
```

### Step 3: Process Messages

```bash
POST /api/v1/message-hooks/process
{
  "messageType": "claim.submitted",
  "sourceSystem": "claims_system",
  "message": {
    "claimNumber": "CLM001",
    "claimant": "John Doe",
    "amount": 5000
  }
}
```

## Common Scenarios

### Scenario 1: Retail Policy Creation

**Requirements**:
- Validate required fields
- Normalize policy number
- Map to standard agreement structure
- Calculate annual premium

**Hook Configuration**:
```javascript
// Pre-processing
if (message.policyNumber == null || message.customerName == null) {
    throw "Missing required fields";
}
message.policyNumber = message.policyNumber.trim().toUpperCase();

// Transformation
{
    "agreementId": message.policyNumber,
    "agreementType": "POLICY",
    "marketContext": "RETAIL",
    "customer": {"name": message.customerName},
    "premium": {"amount": message.premiumAmount, "frequency": "MONTHLY"}
}

// Post-processing
if (result.premium.frequency == "MONTHLY") {
    result.premium.annualAmount = result.premium.amount * 12;
}
```

### Scenario 2: Commercial High-Value Policy

**Requirements**:
- Enhanced validation for high-value policies
- Require underwriter approval
- Send notifications
- Fail fast on validation errors

**Hook Configuration**:
```javascript
// Pre-processing
if (message.premiumAmount > 100000) {
    if (message.underwriterApproval == null) {
        throw "High value policy requires underwriter approval";
    }
}

// Transformation
{
    "agreementId": message.policyNumber,
    "marketContext": "COMMERCIAL",
    "premium": {"amount": message.premiumAmount},
    "underwriter": {
        "approval": message.underwriterApproval,
        "approvalDate": message.approvalDate
    }
}

// Post-processing
if (result.premium.amount > 100000) {
    result.notifications = [{
        "type": "high_value_policy",
        "recipient": "underwriting_team"
    }];
}
```

### Scenario 3: London Market Placement

**Requirements**:
- Validate UMR format
- Map to Lloyd's structure
- Add market-specific metadata
- Retry on transient failures

**Hook Configuration**:
```javascript
// Pre-processing
if (message.umr == null || !message.umr.matches("^[A-Z0-9]{10,}$")) {
    throw "Invalid UMR format";
}

// Transformation
{
    "agreementId": message.umr,
    "marketContext": "LONDON_MARKET",
    "placement": {
        "umr": message.umr,
        "leadUnderwriter": message.leadUnderwriter
    },
    "syndicate": {"number": message.syndicateNumber}
}

// Post-processing
result.marketMetadata = {
    "submittedToLloyds": true,
    "lloydsReference": "LLO-" + result.placement.umr
};
```

### Scenario 4: Data Enrichment with External Lookup

**Requirements**:
- Look up product details
- Continue processing if lookup fails
- Add enrichment metadata

**Hook Configuration**:
```javascript
// Pre-processing (with log_continue strategy)
var productDb = {
    "HOME-001": {"name": "Home Insurance", "basePremium": 500}
};
var product = productDb[message.productCode];
if (product == null) {
    throw "Product not found: " + message.productCode; // Logged, not fatal
}
message.productDetails = product;

// Transformation
{
    "agreementId": message.policyNumber,
    "product": message.productDetails != null ? {
        "code": message.productCode,
        "name": message.productDetails.name
    } : {"code": message.productCode}
}

// Post-processing
result.enrichmentMetadata = {
    "enriched": message.productDetails != null,
    "source": "product_database"
};
```

## API Reference

### List Hooks
```
GET /api/v1/message-hooks?messageType={type}&sourceSystem={system}
```

### Create Hook
```
POST /api/v1/message-hooks
Content-Type: application/json

{
  "hookName": "my_hook",
  "messageType": "policy.created",
  "sourceSystem": "retail_system",
  "transformationJexl": "message",
  "errorHandlingStrategy": "fail_fast",
  "enabled": true
}
```

### Update Hook
```
PUT /api/v1/message-hooks/{id}
```

### Delete Hook
```
DELETE /api/v1/message-hooks/{id}
```

### Validate Hook
```
POST /api/v1/message-hooks/{id}/validate
```

### Test Hook
```
POST /api/v1/message-hooks/{id}/test
Content-Type: application/json

{
  "policyNumber": "TEST123",
  "customerName": "John Doe"
}
```

### Process Message
```
POST /api/v1/message-hooks/process
Content-Type: application/json

{
  "messageType": "policy.created",
  "sourceSystem": "retail_system",
  "message": {
    "policyNumber": "ABC123",
    "customerName": "John Doe"
  }
}
```

## Troubleshooting

### Common Errors

#### "Invalid JEXL syntax"
- Check for typos in variable names
- Ensure proper quoting of strings
- Validate bracket/parenthesis matching

#### "Missing required field"
- Verify message structure matches expected format
- Check preprocessing validation logic
- Review error handling strategy

#### "Expression timeout"
- Simplify JEXL scripts
- Remove nested loops
- Consider increasing max execution time

#### "Retry limit exceeded"
- Check external service availability
- Review retry configuration
- Consider increasing maxAttempts or backoff

### Debugging

1. **Test with sample data**: Use `/test` endpoint with known-good data
2. **Check execution history**: Query `sys_message_processing_executions`
3. **Enable debug logging**: Set `com.beema.kernel.service.message=DEBUG`
4. **Validate JEXL syntax**: Use `/validate` endpoint

## Best Practices

1. **Keep scripts simple**: Complex logic should be in Java services
2. **Validate early**: Use preprocessing for validation
3. **Fail fast when appropriate**: Don't process invalid data
4. **Log context**: Include relevant metadata in errors
5. **Test thoroughly**: Use `/test` endpoint before enabling
6. **Monitor execution**: Track metrics and errors
7. **Version hooks**: Use descriptive hook names with versions
8. **Document scripts**: Add comments in JEXL for clarity

## Support

For questions or issues:
- Check execution logs: `sys_message_processing_executions`
- Review this documentation
- Contact the Beema Platform team
