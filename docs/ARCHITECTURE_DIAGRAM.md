# Beema Platform Architecture - Docker Compose Setup

## Complete Service Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Beema Unified Platform                               │
│                     Docker Compose - 11 Services                             │
└──────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────── FRONTEND LAYER ───────────────────────────────────┐
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────┐            │
│  │  Studio (React)                                             │            │
│  │  Port: 3000                                                 │            │
│  │  Container: beema-studio                                    │            │
│  └─────────────────────────┬───────────────────────────────────┘            │
│                            │                                                 │
└────────────────────────────┼─────────────────────────────────────────────────┘
                             │
                             │ HTTP REST API
                             ▼
┌─────────────────────────── API LAYER ────────────────────────────────────────┐
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────┐            │
│  │  Beema Kernel (Spring Boot)                                 │            │
│  │  Port: 8080                                                 │            │
│  │  Container: beema-kernel                                    │            │
│  │  ┌───────────────────────────────────────────────────────┐ │            │
│  │  │  Embedded Temporal Worker                             │ │            │
│  │  │  - Task Queue: POLICY_TASK_QUEUE                      │ │            │
│  │  │  - Max Concurrent Workflows: 10                       │ │            │
│  │  │  - Max Concurrent Activities: 10                      │ │            │
│  │  └───────────────────────────────────────────────────────┘ │            │
│  └─────────────┬───────────┬───────────┬───────────┬───────────┘            │
│                │           │           │           │                        │
└────────────────┼───────────┼───────────┼───────────┼────────────────────────┘
                 │           │           │           │
    ┌────────────┘           │           │           └──────────┐
    │                        │           │                      │
    ▼                        ▼           ▼                      ▼
┌─────────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐
│  Metadata       │  │  Temporal    │  │  Keycloak    │  │  Kafka          │
│  Service        │  │  Server      │  │  (OAuth2)    │  │  Broker         │
│  Port: 8082     │  │  Port: 7233  │  │  Port: 8180  │  │  Port: 9092     │
│                 │  │              │  │              │  │  /29092         │
└─────────────────┘  └──────────────┘  └──────────────┘  └─────────────────┘
                             │                                  │
                             │                                  │
                             ▼                                  ▼
                     ┌──────────────┐                  ┌─────────────────┐
                     │  Temporal UI │                  │  Message        │
                     │  Port: 8088  │                  │  Processor      │
                     └──────────────┘                  │  (Flink)        │
                                                       │  Port: 8081     │
                                                       └─────────────────┘
                             │
                             │
                             ▼
┌─────────────────────────── DATA LAYER ───────────────────────────────────────┐
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL 16                                                        │  │
│  │  Port: 5433 (external) / 5432 (internal)                             │  │
│  │  Container: beema-postgres                                            │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │  │
│  │  │  Databases:                                                      │ │  │
│  │  │  • beema_kernel      - Beema application data                   │ │  │
│  │  │  • keycloak          - OAuth2/OIDC data                         │ │  │
│  │  │  • beema_metadata    - Metadata service data                    │ │  │
│  │  │  • temporal          - Temporal workflow data (auto-created)    │ │  │
│  │  │  • temporal_visibility - Temporal search index (auto-created)   │ │  │
│  │  └─────────────────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  Zookeeper                                                            │  │
│  │  Port: 2181                                                           │  │
│  │  Container: beema-zookeeper                                           │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Service Dependency Graph

```
postgres (healthy)
  ├─→ keycloak (healthy)
  │    └─→ metadata-service (healthy)
  │         └─→ beema-kernel (healthy)
  │              └─→ studio
  │
  ├─→ temporal (healthy)
  │    ├─→ temporal-ui
  │    └─→ beema-kernel (worker)
  │
  └─→ beema-message-processor

zookeeper
  └─→ kafka (healthy)
       └─→ kafka-init (completed)
            └─→ beema-message-processor
```

## Data Flow: Policy Creation Workflow

```
┌──────────┐
│  User    │
│ (Studio) │
└────┬─────┘
     │
     │ 1. Create Policy Request (HTTP POST)
     ▼
┌──────────────────┐
│  Beema Kernel    │
│  (REST API)      │
└────┬─────────────┘
     │
     │ 2. Start Workflow
     ▼
┌──────────────────┐        ┌──────────────────┐
│  Temporal        │◄───────┤  Temporal Worker │
│  Server          │        │  (in Kernel)     │
│  (Orchestrator)  │  3.    └──────────────────┘
└────┬─────────────┘  Poll for tasks
     │
     │ 4. Schedule Activities
     ▼
┌──────────────────────────────────────────────────┐
│  Policy Activities (executed by worker)          │
│  ┌────────────────────────────────────────────┐  │
│  │  1. ValidatePolicyActivity                 │  │
│  │     └─→ Call Metadata Service             │  │
│  │  2. CalculatePremiumActivity               │  │
│  │     └─→ Call Metadata Service             │  │
│  │  3. PersistPolicyActivity                  │  │
│  │     └─→ Write to PostgreSQL               │  │
│  │  4. NotifyCustomerActivity                 │  │
│  │     └─→ Publish to Kafka                  │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
     │
     │ 5. Store workflow history
     ▼
┌──────────────────┐
│  PostgreSQL      │
│  (temporal DB)   │
└──────────────────┘
     │
     │ 6. Workflow completion
     ▼
┌──────────────────┐
│  Beema Kernel    │
│  (Response)      │
└────┬─────────────┘
     │
     │ 7. Policy Created Response (HTTP 201)
     ▼
┌──────────┐
│  Studio  │
│  (UI)    │
└──────────┘
```

## Port Mapping

```
┌─────────────────────────────────────────────────────────────┐
│  Host Machine                                               │
│                                                             │
│  Port 3000  ──→  Studio (React Frontend)                   │
│  Port 5433  ──→  PostgreSQL (Database)                     │
│  Port 7233  ──→  Temporal (gRPC Workflow Server)           │
│  Port 8080  ──→  Beema Kernel (REST API)                   │
│  Port 8081  ──→  Message Processor (Flink Web UI)          │
│  Port 8082  ──→  Metadata Service (REST API)               │
│  Port 8088  ──→  Temporal UI (Web Interface)               │
│  Port 8180  ──→  Keycloak (OAuth2 Admin Console)           │
│  Port 9092  ──→  Kafka (External Listener)                 │
│  Port 2181  ──→  Zookeeper (Coordination Service)          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ Docker Bridge Network
                           │ (beema-network)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Container Network (Internal)                               │
│                                                             │
│  postgres:5432       ──→  PostgreSQL                        │
│  temporal:7233       ──→  Temporal Server                   │
│  keycloak:8080       ──→  Keycloak                          │
│  metadata-service:8082 ──→  Metadata Service                │
│  beema-kernel:8080   ──→  Beema Kernel                      │
│  kafka:29092         ──→  Kafka (Internal Listener)         │
│  zookeeper:2181      ──→  Zookeeper                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Temporal Worker Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Beema Kernel Container (beema-kernel)                      │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Spring Boot Application                              │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │  REST Controllers                               │ │ │
│  │  │  • PolicyController                             │ │ │
│  │  │  • ClaimController                              │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │  Temporal Worker (Embedded)                     │ │ │
│  │  │                                                 │ │ │
│  │  │  Configuration:                                 │ │ │
│  │  │  • Host: temporal                               │ │ │
│  │  │  • Port: 7233                                   │ │ │
│  │  │  • Namespace: default                           │ │ │
│  │  │  • Task Queue: POLICY_TASK_QUEUE                │ │ │
│  │  │  • Max Workflows: 10                            │ │ │
│  │  │  • Max Activities: 10                           │ │ │
│  │  │                                                 │ │ │
│  │  │  ┌─────────────────────────────────────────┐   │ │ │
│  │  │  │  Workflow Implementations               │   │ │ │
│  │  │  │  • PolicyWorkflowImpl                   │   │ │ │
│  │  │  │  • ClaimWorkflowImpl                    │   │ │ │
│  │  │  └─────────────────────────────────────────┘   │ │ │
│  │  │                                                 │ │ │
│  │  │  ┌─────────────────────────────────────────┐   │ │ │
│  │  │  │  Activity Implementations               │   │ │ │
│  │  │  │  • PolicyActivitiesImpl                 │   │ │ │
│  │  │  │  • ClaimActivitiesImpl                  │   │ │ │
│  │  │  └─────────────────────────────────────────┘   │ │ │
│  │  │                                                 │ │ │
│  │  └─────────────────┬───────────────────────────────┘ │ │
│  │                    │                                 │ │
│  └────────────────────┼─────────────────────────────────┘ │
│                       │                                   │
└───────────────────────┼───────────────────────────────────┘
                        │
                        │ gRPC Connection
                        │
                        ▼
                ┌────────────────┐
                │  Temporal      │
                │  Server        │
                │  :7233         │
                └────────────────┘
```

## Temporal Workflow Execution Flow

```
┌──────────────────────────────────────────────────────────────────┐
│  Step 1: Client Starts Workflow                                 │
└──────────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Beema Kernel                                                    │
│  workflowClient.newWorkflowStub(PolicyWorkflow.class)            │
│  workflow.createPolicy(request)                                  │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ gRPC: StartWorkflowExecution
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Temporal Server                                                 │
│  • Creates workflow execution                                    │
│  • Assigns workflow ID                                           │
│  • Persists to PostgreSQL                                        │
│  • Adds task to POLICY_TASK_QUEUE                                │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ Long poll: PollWorkflowTaskQueue
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Temporal Worker (in Beema Kernel)                               │
│  • Receives workflow task                                        │
│  • Executes PolicyWorkflowImpl.createPolicy()                    │
│  • Schedules activities                                          │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ RespondWorkflowTaskCompleted
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Temporal Server                                                 │
│  • Records workflow decisions                                    │
│  • Creates activity tasks                                        │
│  • Adds to POLICY_TASK_QUEUE                                     │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ Long poll: PollActivityTaskQueue
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Temporal Worker (in Beema Kernel)                               │
│  • Receives activity task                                        │
│  • Executes activity (e.g., validatePolicy)                      │
│  • Interacts with external services (DB, APIs)                   │
│  • Returns result                                                │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ RespondActivityTaskCompleted
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Temporal Server                                                 │
│  • Records activity result                                       │
│  • Continues workflow execution                                  │
│  • Repeats for next activity                                     │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ (All activities complete)
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Temporal Server                                                 │
│  • Marks workflow as completed                                   │
│  • Persists final state                                          │
│  • Returns result to client                                      │
└──────────────────────────────────────────────────────────────────┘
                        │
                        │ Workflow result
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│  Beema Kernel                                                    │
│  • Receives workflow result                                      │
│  • Returns HTTP response to client                               │
└──────────────────────────────────────────────────────────────────┘
```

## Volume Persistence

```
┌─────────────────────────────────────────────────────────────────┐
│  Docker Host                                                    │
│                                                                 │
│  /var/lib/docker/volumes/                                      │
│    ├─ beema_postgres_data/                                     │
│    │   └─ _data/                                               │
│    │      ├─ base/                (PostgreSQL data)            │
│    │      ├─ global/              (System catalogs)            │
│    │      └─ pg_wal/              (Write-ahead log)            │
│    │                                                            │
│    ├─ beema_temporal_data/                                     │
│    │   └─ _data/                                               │
│    │      └─ config/              (Temporal config)            │
│    │                                                            │
│    ├─ beema_zookeeper_data/                                    │
│    │   └─ _data/                                               │
│    │      └─ version-2/           (Zookeeper state)            │
│    │                                                            │
│    ├─ beema_kafka_data/                                        │
│    │   └─ _data/                                               │
│    │      ├─ raw-messages-0/      (Topic partition)            │
│    │      └─ processed-messages-0/ (Topic partition)           │
│    │                                                            │
│    └─ beema_zookeeper_logs/                                    │
│        └─ _data/                                               │
│           └─ log/                 (Transaction logs)           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Network Communication Matrix

```
┌─────────────┬────────┬──────────┬──────────┬────────┬────────┬────────┐
│ From \ To   │ Kernel │ Temporal │ Postgres │ Kafka  │ Keyclk │ Studio │
├─────────────┼────────┼──────────┼──────────┼────────┼────────┼────────┤
│ Studio      │  HTTP  │    -     │    -     │   -    │   -    │   -    │
│ Kernel      │   -    │   gRPC   │   JDBC   │  TCP   │  HTTP  │   -    │
│ Temporal    │   -    │    -     │   JDBC   │   -    │   -    │   -    │
│ Temporal-UI │   -    │   gRPC   │    -     │   -    │   -    │   -    │
│ Metadata    │   -    │    -     │   JDBC   │   -    │  HTTP  │   -    │
│ Msg-Proc    │   -    │    -     │   JDBC   │  TCP   │   -    │   -    │
└─────────────┴────────┴──────────┴──────────┴────────┴────────┴────────┘
```

## Resource Allocation

```
┌──────────────────────────────────────────────────────────────┐
│  Service            │  Memory    │  CPU     │  Storage      │
├─────────────────────┼────────────┼──────────┼───────────────┤
│  postgres           │  512MB     │  1 core  │  5GB (vol)    │
│  temporal           │  1GB       │  1 core  │  1GB (vol)    │
│  temporal-ui        │  256MB     │  0.5 cor │  -            │
│  beema-kernel       │  1.5GB     │  2 cores │  500MB (logs) │
│  metadata-service   │  512MB     │  1 core  │  -            │
│  keycloak           │  512MB     │  1 core  │  -            │
│  kafka              │  1GB       │  1 core  │  2GB (vol)    │
│  zookeeper          │  256MB     │  0.5 cor │  1GB (vol)    │
│  msg-processor      │  1GB       │  1 core  │  -            │
│  studio             │  256MB     │  0.5 cor │  -            │
├─────────────────────┼────────────┼──────────┼───────────────┤
│  TOTAL              │  ~7GB      │  10 cores│  ~10GB        │
└──────────────────────────────────────────────────────────────┘

Recommended Host Resources:
  • RAM: 16GB (8GB minimum)
  • CPU: 8 cores (4 minimum)
  • Disk: 50GB free space
  • Docker allocation: 10GB RAM, 6 CPU cores
```

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-12
**Architecture**: Microservices with Temporal Workflow Orchestration
**Platform**: Docker Compose
