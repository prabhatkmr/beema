# Policy Lifecycle Engine - Quick Start Guide

Get the Policy Lifecycle Engine running in 5 minutes.

---

## Prerequisites

- Java 21
- PostgreSQL 14+
- Temporal.io (optional for full workflow testing)
- Maven 3.8+

---

## Step 1: Start PostgreSQL

```bash
# Using Docker
docker run -d \
  --name beema-postgres \
  -e POSTGRES_DB=beema \
  -e POSTGRES_USER=beema \
  -e POSTGRES_PASSWORD=beema \
  -p 5432:5432 \
  postgres:14
```

---

## Step 2: Run Database Migrations

```bash
# Ensure V19 migration is included
ls src/main/resources/db/migration/V19__create_policies_table.sql

# Run migrations
mvn flyway:migrate

# Verify
PGPASSWORD=beema psql -h localhost -U beema -d beema -c "\d policies"
```

**Expected Output:**
```
Table "public.policies"
     Column       |           Type           | Nullable | Default
------------------+--------------------------+----------+---------
 id               | uuid                     | not null |
 policy_number    | character varying(100)   | not null |
 version          | integer                  | not null |
 valid_from       | timestamp with time zone | not null |
 valid_to         | timestamp with time zone | not null |
 transaction_time | timestamp with time zone | not null |
 is_current       | boolean                  | not null |
 status           | character varying(50)    | not null |
 premium          | numeric(15,2)            |          |
 ...
```

---

## Step 3: Start Temporal (Optional)

```bash
# Using Docker
docker run -d \
  --name temporal \
  -p 7233:7233 \
  -p 8088:8088 \
  temporalio/auto-setup:latest

# Verify
curl http://localhost:7233/api/v1/namespaces
```

Temporal UI: http://localhost:8088

---

## Step 4: Start beema-kernel

```bash
# Build
mvn clean compile

# Run
mvn spring-boot:run
```

**Expected Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v3.2.0)

INFO  Started KernelApplication in 2.456 seconds
INFO  PolicyLifecycleWorkflowImpl registered
```

---

## Step 5: Test the Implementation

### Option A: Run Test Script

```bash
./test-policy-lifecycle.sh
```

**Expected Output:**
```
==================================================
  Policy Lifecycle Engine - Test Suite
==================================================

[1/6] Checking prerequisites...
✅ Temporal is running
✅ PostgreSQL is accessible
✅ beema-kernel is running

[2/6] Verifying database schema...
✅ policies table exists
✅ Bitemporal columns present

[3/6] Checking Temporal workflow registration...
✅ Connected to Temporal namespace

[4/6] Querying existing policies...
   Found 0 existing policy versions

[5/6] Testing SCD Type 2 versioning...
   Test policy: TEST-POL-1707768024
   Creating version 1...
✅ Version 1 created
   Creating endorsement (version 2)...
✅ Version 2 created (endorsement)

   Policy versions for TEST-POL-1707768024:
 version | valid_from      | valid_to        | is_current | status | premium | updated_by
---------+-----------------+-----------------+------------+--------+---------+------------
       1 | 2024-02-12 20:00| 2024-05-12 20:00| f          | ACTIVE | 1200.00 | endorsement
       2 | 2024-05-12 20:00| CURRENT         | t          | ACTIVE | 1400.00 | endorsement

[6/6] Testing bitemporal queries...
   Current version query:
   Result:  2 | 1400.00

   As-of query (1 month ago - should return version 1):
   Result:  1 | 1200.00

   Audit trail query (all versions):
 version | premium | event       | recorded_at
---------+---------+-------------+-------------
       1 | 1200.00 | test-script | 2024-02-12 20:00
       2 | 1400.00 | endorsement | 2024-02-12 20:01

==================================================
                   Test Summary
==================================================

✅ All tests passed!
```

### Option B: Manual Testing

#### 1. Create Initial Policy

```sql
INSERT INTO policies (
    policy_number, version, valid_from, valid_to, transaction_time, is_current,
    status, premium, inception_date, expiry_date, coverage_details,
    tenant_id, created_by, updated_by
) VALUES (
    'POL-2024-001', 1, NOW(), '9999-12-31'::TIMESTAMPTZ, NOW(), true,
    'ACTIVE', 1200.00, NOW(), NOW() + INTERVAL '1 year', '{"vehicle_make": "Toyota"}',
    'default', 'system', 'system'
);
```

**Verify:**
```sql
SELECT * FROM policies WHERE policy_number = 'POL-2024-001';
```

#### 2. Create Endorsement (SCD Type 2)

**Close Version 1:**
```sql
UPDATE policies
SET valid_to = NOW() + INTERVAL '3 months',
    is_current = false,
    updated_by = 'endorsement'
WHERE policy_number = 'POL-2024-001'
  AND version = 1;
```

**Create Version 2:**
```sql
INSERT INTO policies (
    policy_number, version, valid_from, valid_to, transaction_time, is_current,
    status, premium, inception_date, expiry_date, coverage_details,
    tenant_id, created_by, updated_by
)
SELECT
    policy_number,
    2,
    NOW() + INTERVAL '3 months',
    '9999-12-31'::TIMESTAMPTZ,
    NOW(),
    true,
    status,
    1400.00,
    inception_date,
    expiry_date,
    '{"vehicle_make": "Toyota", "sum_insured": 60000}'::jsonb,
    tenant_id,
    'endorsement',
    'endorsement'
FROM policies
WHERE policy_number = 'POL-2024-001'
  AND version = 1;
```

**Verify:**
```sql
SELECT version, valid_from, valid_to, is_current, premium
FROM policies
WHERE policy_number = 'POL-2024-001'
ORDER BY version;
```

**Expected Result:**
```
 version |      valid_from      |       valid_to       | is_current | premium
---------+----------------------+----------------------+------------+---------
       1 | 2024-02-12 20:00:00  | 2024-05-12 20:00:00  | f          | 1200.00
       2 | 2024-05-12 20:00:00  | 9999-12-31 23:59:59  | t          | 1400.00
```

#### 3. Query Current Version

```sql
SELECT *
FROM policies
WHERE policy_number = 'POL-2024-001'
  AND is_current = true;
```

#### 4. Query Historical Version (Point-in-Time)

```sql
-- Get policy state as of 1 month ago
SELECT *
FROM policies
WHERE policy_number = 'POL-2024-001'
  AND valid_from <= NOW() - INTERVAL '1 month'
  AND valid_to > NOW() - INTERVAL '1 month'
  AND transaction_time <= NOW() - INTERVAL '1 month';
```

---

## Step 6: Test with Temporal Workflow (Advanced)

### Start Workflow Worker

```java
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class PolicyLifecycleWorker {
    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker("policy-lifecycle-queue");
        worker.registerWorkflowImplementationTypes(PolicyLifecycleWorkflowImpl.class);
        worker.registerActivitiesImplementations(
            new PersistenceActivitiesImpl(policyRepository),
            new RatingActivitiesImpl()
        );

        factory.start();
        System.out.println("Policy Lifecycle Worker started");
    }
}
```

### Start Policy Workflow

```java
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

public class StartPolicy {
    public static void main(String[] args) {
        WorkflowClient client = WorkflowClient.newInstance(...);

        PolicyLifecycleWorkflow workflow = client.newWorkflowStub(
            PolicyLifecycleWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("policy-POL-2024-001")
                .setTaskQueue("policy-lifecycle-queue")
                .build()
        );

        Map<String, Object> coverage = Map.of(
            "vehicle_make", "Toyota",
            "vehicle_model", "Camry"
        );

        PolicyStartArgs args = new PolicyStartArgs(
            "POL-2024-001",
            LocalDateTime.now(),
            LocalDateTime.now().plusYears(1),
            1200.0,
            coverage
        );

        // Start workflow asynchronously
        WorkflowClient.start(workflow::execute, args);

        System.out.println("Policy workflow started: policy-POL-2024-001");
    }
}
```

### Send Endorsement Signal

```java
public class SendEndorsement {
    public static void main(String[] args) {
        WorkflowClient client = WorkflowClient.newInstance(...);

        PolicyLifecycleWorkflow workflow = client.newWorkflowStub(
            PolicyLifecycleWorkflow.class,
            "policy-POL-2024-001"
        );

        Map<String, Object> changes = Map.of("sum_insured", 60000);

        EndorsementArgs endorsement = new EndorsementArgs(
            LocalDateTime.now().plusMonths(3),
            1200.0,
            1400.0,
            changes
        );

        workflow.endorse(endorsement);

        // Query status
        String status = workflow.getCurrentStatus();
        Integer version = workflow.getActiveVersion();

        System.out.println("Status: " + status);
        System.out.println("Version: " + version);
    }
}
```

### View in Temporal UI

1. Open http://localhost:8088
2. Find workflow: `policy-POL-2024-001`
3. View history:
   - WorkflowExecutionStarted
   - ActivityTaskScheduled (createPolicyVersion)
   - ActivityTaskCompleted
   - SignalReceived (endorse)
   - ActivityTaskScheduled (createEndorsementVersion)
   - ...

---

## Bitemporal Query Patterns

### Get Current Version
```sql
SELECT * FROM policies
WHERE policy_number = 'POL-2024-001'
  AND is_current = true;
```

### Get Version as of Specific Date
```sql
SELECT * FROM policies
WHERE policy_number = 'POL-2024-001'
  AND valid_from <= '2024-04-01 00:00:00'::TIMESTAMPTZ
  AND valid_to > '2024-04-01 00:00:00'::TIMESTAMPTZ;
```

### Get All Versions (Audit Trail)
```sql
SELECT version, status, premium, valid_from, valid_to, updated_by
FROM policies
WHERE policy_number = 'POL-2024-001'
ORDER BY version;
```

### Get Policies Active in Date Range
```sql
SELECT DISTINCT policy_number, version
FROM policies
WHERE valid_from <= '2024-12-31 23:59:59'::TIMESTAMPTZ
  AND valid_to > '2024-01-01 00:00:00'::TIMESTAMPTZ
  AND status = 'ACTIVE';
```

---

## Troubleshooting

### Migration Not Applied
```bash
# Check migration status
mvn flyway:info

# Force clean and re-migrate (CAUTION: deletes all data)
mvn flyway:clean flyway:migrate
```

### Temporal Connection Error
```bash
# Check Temporal is running
docker ps | grep temporal

# Check logs
docker logs temporal

# Restart Temporal
docker restart temporal
```

### Policy Versions Not Created
```bash
# Check transaction logs
PGPASSWORD=beema psql -h localhost -U beema -d beema -c "
  SELECT * FROM policies
  WHERE transaction_time > NOW() - INTERVAL '1 hour'
  ORDER BY transaction_time DESC
"
```

---

## Next Steps

1. **REST API** - Create endpoints to trigger workflows via HTTP
2. **Scheduled Events** - Add auto-renewal workflow
3. **Claims Workflow** - Create child workflow for claims processing
4. **Analytics** - Query historical versions for reporting

---

## References

- [Implementation Summary](./POLICY_LIFECYCLE_IMPLEMENTATION_SUMMARY.md)
- [Temporal.io Documentation](https://docs.temporal.io)
- [Bitemporal Data](https://en.wikipedia.org/wiki/Bitemporal_data)
- [SCD Type 2](https://en.wikipedia.org/wiki/Slowly_changing_dimension#Type_2:_add_new_row)

---

**Status:** ✅ Ready to Use
**Last Updated:** 2026-02-12
