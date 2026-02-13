# Beema Insurance Platform

A modern, AI-powered insurance platform with metadata-driven architecture, bitemporal data modeling, event-driven workflows, and visual form builders.

**Tech Stack:** Spring Boot 3, Next.js 16, Temporal.io, Apache Flink 1.18, Kafka, PostgreSQL 16, OpenRouter AI, Helm/K8s

## Architecture

This is a **Turborepo monorepo** containing multiple microservices, frontend apps, and shared packages:

```
beema/
├── apps/                              # Applications & Services
│   ├── dashboard/                     # Hub landing page (Next.js, port 3000)
│   ├── studio/                        # Visual builder & scheduling (Next.js, port 3010)
│   ├── portal/                        # Policy & claims portal (Next.js, port 3011)
│   ├── admin/                         # Global admin console (Next.js, port 3012)
│   ├── beema-streaming/               # Speed layer - Kafka→Parquet→MinIO (Flink)
│   ├── beema-message-processor/       # Stream processor - JEXL hooks (Flink)
│   ├── metadata-service/              # Schema registry (Spring Boot, port 8082)
│   └── auth-service/                  # Authentication (planned)
├── beema-kernel/                      # Core kernel (Spring Boot, port 8080)
├── packages/                          # Shared libraries
│   └── ui/                            # React component library (@beema/ui)
├── platform/                          # Kubernetes/Helm charts
│   ├── Chart.yaml                     # Helm chart (v0.1.0)
│   ├── values.yaml                    # Default values
│   ├── templates/                     # 28 K8s templates
│   └── observability/                 # Prometheus, Grafana provisioning
├── infra/                             # Developer tooling
│   ├── dev-aliases.sh                 # Shell aliases for local dev
│   └── DEV_SETUP.md                   # Comprehensive setup guide
├── docker-compose.yml                 # Full stack (24+ services)
├── turbo.json                         # Turborepo pipeline config
└── package.json                       # Root workspace config
```

## Key Features

### AI-Powered Intelligence
- **Claim Analysis:** GPT-4, Claude, or Llama analyze claims and recommend actions
- **5 AI Tools:** Field metadata, JEXL evaluation, business rules, validation, metrics
- **Multi-Provider:** OpenRouter gives access to 10+ AI providers
- **Auto Model Selection:** Smart routing based on claim complexity

### Visual Form Builder (Studio)
- **Drag & Drop:** Build forms visually with 9 field types
- **Real-time Validation:** Against beema-kernel metadata schema
- **Export/Import:** sys_layouts JSON format
- **Batch Scheduling:** Per-tenant scheduled job management

### Event-Driven Architecture
- **Temporal Workflows:** Durable, retryable policy and claim workflows
- **Kafka Streaming:** Real-time message transformation with Flink
- **Speed Layer:** Kafka → Parquet → MinIO datalake pipeline
- **JEXL Expressions:** Pre-compiled for high performance

### Enterprise-Grade
- **Bitemporal Data:** Valid time + Transaction time
- **Multi-Tenancy:** Row-Level Security with cell-based datasource routing
- **Metadata-Driven:** Zero-code field additions via JSONB flex-schema
- **Global Admin Console:** Tenant, region, and datasource management

### Analytics Layer
- **Parquet Export:** Batch export agreements to columnar Parquet format via Spring Batch
- **Cloud-Agnostic Storage:** Pluggable blob storage -- AWS S3, Azure Blob, MinIO, or local filesystem
- **Dynamic Schema:** JSONB attributes auto-flattened into Avro → Parquet columns
- **Tenant Isolation:** Hive-style partitioned paths (`tenant={id}/object=agreement/date={date}/`)

### Observability
- **OpenTelemetry → Jaeger** for distributed tracing
- **Prometheus** for metrics collection
- **Grafana** for unified dashboards
- **Micrometer** for application metrics

### Market Support
All features support **Retail, Commercial, and London Market** contexts.

## Quick Start

> See [infra/DEV_SETUP.md](infra/DEV_SETUP.md) for a comprehensive setup guide with shell aliases and troubleshooting tips.

### Prerequisites

- **Node.js** >= 18.0.0
- **pnpm** >= 8.0.0
- **Java** >= 21
- **Maven** >= 3.8.0
- **Docker** & **Docker Compose**

### Installation

```bash
# Clone and install dependencies
pnpm install

# Load developer aliases (optional but recommended)
source ./infra/dev-aliases.sh
```

### Start Development

```bash
# Option 1: Infrastructure only (run apps locally)
beema-infra                  # Start all Docker infrastructure
pnpm run dev:frontend        # Start Dashboard, Studio, Portal, Admin

# Option 2: Everything in Docker
docker compose up -d

# Option 3: Full stack with local frontend
beema-dev-full               # Infrastructure + backend in Docker, frontend local
```

### Environment Configuration

**beema-kernel** (`beema-kernel/.env` or environment variables):
```bash
OPENROUTER_API_KEY=sk-or-v1-your-key-here
DATABASE_URL=jdbc:postgresql://localhost:5433/beema_kernel
TEMPORAL_HOST=localhost:7233
```

**studio** (`apps/studio/.env.local`):
```bash
BEEMA_KERNEL_URL=http://localhost:8080
```

**admin** (`apps/admin/.env.local`):
```bash
BEEMA_KERNEL_URL=http://localhost:8080
```

## Service URLs

| Service | Port | URL |
|---------|------|-----|
| **Dashboard** (Hub) | 3000 | http://localhost:3000 |
| **Studio** (Builder) | 3010 | http://localhost:3010 |
| **Portal** (Policies) | 3011 | http://localhost:3011 |
| **Admin** (Console) | 3012 | http://localhost:3012 |
| **Kernel API** | 8080 | http://localhost:8080/swagger-ui.html |
| **Metadata API** | 8082 | http://localhost:8082 |
| **Keycloak** | 8180 | http://localhost:8180 (admin/admin) |
| **Temporal UI** | 8088 | http://localhost:8088 |
| **Grafana** | 3002 | http://localhost:3002 (admin/admin) |
| **Jaeger** | 16686 | http://localhost:16686 |
| **Prometheus** | 9090 | http://localhost:9090 |
| **Flink Dashboard** | 8081 | http://localhost:8081 |
| **MinIO Console** | 9001 | http://localhost:9001 (admin/password123) |
| **Inngest** | 8288 | http://localhost:8288 |
| **Kafka** | 9092 | localhost:9092 |
| **PostgreSQL** | 5433 | localhost:5433 (beema/beema) |

## Docker Compose

Start the complete platform with all dependencies:

```bash
docker compose up -d
```

**24+ services included:**

| Category | Services |
|----------|----------|
| **Database** | PostgreSQL 16 (port 5433) |
| **Auth** | Keycloak 23 (port 8180) |
| **Messaging** | Zookeeper + Kafka (port 9092) + topic init |
| **Workflows** | Temporal Server (port 7233) + Temporal UI (port 8088) |
| **Storage** | MinIO S3 (ports 9000/9001) + bucket init |
| **Streaming** | Flink JobManager (port 8081) + TaskManagers (x2) |
| **Observability** | Jaeger (port 16686) + Prometheus (port 9090) + Grafana (port 3002) |
| **Background Jobs** | Inngest (port 8288) |
| **Backend** | beema-kernel (port 8080) + metadata-service (port 8082) |
| **Flink Jobs** | beema-message-processor + beema-streaming |
| **Frontend** | Dashboard (port 3000) + Studio (port 3010) |

**Verify all services:**
```bash
./docker-compose-verify.sh
./platform/observability/verify-stack.sh
```

## Applications & Services

### Dashboard (port 3000)
Hub landing page with links to all platform applications.

**Tech:** Next.js 16, Tailwind CSS v4, `@beema/ui`

### Studio (port 3010)
Visual form builder, message blueprint editor, and batch schedule management.
- **Layout Builder:** Drag-and-drop form designer with 9 field types
- **Blueprint Editor:** Visual message mapping between systems
- **Batch Schedules:** Per-tenant scheduled job management with Temporal integration

**Tech:** Next.js 16, TypeScript, Tailwind CSS v4, dnd-kit, Zustand

### Portal (port 3011)
Policy and claims management portal for end users.
- Agreement management with bitemporal queries
- Task management and workflow tracking
- Metadata-driven forms

**Tech:** Next.js 16, TypeScript, Tailwind CSS v4, shadcn/ui, TanStack Query

### Admin Console (port 3012)
Global administration console for platform operators.
- **Tenant Management:** Create, update, activate, suspend, deactivate tenants
- **Region Management:** Data residency regions with compliance rules
- **Datasource Routing:** Manage database connection pools for cell-based routing
- **System Health:** Service status monitoring with links to observability tools
- **Dashboard:** Platform overview with tenant/region/agreement statistics

**Tech:** Next.js 16, TypeScript, Tailwind CSS v4, `@beema/ui`, lucide-react

### beema-kernel (port 8080)
Core agreement kernel with bitemporal data, AI analysis, and workflow orchestration.
- Metadata-driven schema with JSONB flex-schema
- Pre-compiled JEXL expressions with Write Shield
- Temporal workflow orchestration (Policy, Claim workflows)
- AI-powered claim analysis with OpenRouter
- Multi-tenancy with Row-Level Security
- Spring Batch Parquet export pipeline
- Admin API for tenant/region/datasource management

**Tech:** Spring Boot 3, PostgreSQL, Temporal.io, Spring AI, Spring Batch, Caffeine Cache

### metadata-service (port 8082)
Schema registry for field definitions, validation rules, UI layouts, and market-specific configurations.

**Tech:** Spring Boot 3, PostgreSQL

### beema-message-processor
Flink streaming job for real-time message transformation.
- Reads from `raw-messages` Kafka topic
- Applies JEXL hooks from `sys_message_hooks` table
- Writes to `beema-events` topic

**Tech:** Apache Flink 1.18, Kafka, PostgreSQL, JEXL

### beema-streaming
Flink speed layer for real-time data lake ingestion.
- Reads from `beema.events.policy_change` Kafka topic
- Writes Parquet files to MinIO datalake (`s3a://beema-datalake/speed/policy/`)
- Checkpointing with RocksDB state backend

**Tech:** Apache Flink 1.18, Kafka, MinIO/S3, Parquet

### Shared Packages

**@beema/ui** — React component library:
- Button (4 variants), Card, Input, Label
- Used by Dashboard, Studio, Portal, and Admin apps

## Available Commands

```bash
# Build all services
pnpm build

# Run all tests
pnpm test

# Lint all services
pnpm lint

# Development mode (all services)
pnpm dev

# Frontend apps only (Dashboard + Studio + Portal + Admin)
pnpm run dev:frontend

# Backend services only (Docker)
pnpm run dev:backend

# Clean build artifacts
pnpm clean

# Format code
pnpm format
```

### Working with Individual Services

```bash
# Build only beema-kernel
turbo build --filter=@beema/kernel

# Test only metadata-service
turbo test --filter=@beema/metadata-service

# Run Studio in dev mode
turbo dev --filter=@beema/studio

# Run Admin Console
turbo dev --filter=@beema/admin
```

## Kubernetes Deployment

Deploy to Kubernetes using the Helm chart in `platform/`:

```bash
cd platform
helm install beema . -f values.yaml
```

### Helm Chart Overview

| | |
|---|---|
| **Chart Version** | 0.1.0 |
| **App Version** | 1.0.0 |
| **Chart Type** | application |

### Templates (28 files)

The Helm chart includes templates for all platform services:

| Category | Templates |
|----------|-----------|
| **Core** | `deployment.yaml`, `service.yaml`, `configmap.yaml`, `secrets.yaml`, `hpa.yaml` |
| **Temporal Worker** | `temporal-worker-deployment.yaml`, `temporal-worker-service.yaml`, `temporal-worker-hpa.yaml`, `temporal-worker-servicemonitor.yaml` |
| **Studio** | `studio/deployment.yaml`, `studio/service.yaml` |
| **Metadata** | `metadata/deployment.yaml`, `metadata/service.yaml` |
| **Flink** | `flink/jobmanager-deployment.yaml`, `flink/taskmanager-deployment.yaml`, `flink/jobmanager-service.yaml` |
| **Kafka** | `kafka/deployment.yaml`, `kafka/service.yaml` |
| **Inngest** | `inngest/deployment.yaml`, `inngest/service.yaml` |
| **Message Processor** | `processor/deployment.yaml`, `processor/service.yaml` |

### Configuration

Key values in `values.yaml`:

```yaml
image:
  repository: beema-kernel
  tag: "1.0.0"

replicaCount: 3

resources:
  requests: { memory: "1Gi", cpu: "500m" }
  limits: { memory: "2Gi", cpu: "1000m" }

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10

externalDatabase:
  host: beema-prod-postgres.rds.amazonaws.com
  database: beema_prod
```

See [platform/DEPLOY.md](platform/DEPLOY.md) for the full deployment guide.

## Developer Aliases

Load shell aliases for faster development:

```bash
source ./infra/dev-aliases.sh
```

| Alias | Description |
|-------|-------------|
| `beema-infra` | Start all infrastructure services |
| `beema-infra-down` | Stop all Docker containers |
| `beema-infra-clean` | Stop + remove volumes (clean slate) |
| `beema-dev` | Infrastructure + frontend apps |
| `beema-dev-full` | Infrastructure + backend + frontend |
| `beema-health` | Show status + all service URLs |
| `beema-up` | Start everything in Docker |
| `beema-down` | Stop everything |

## Technology Stack

### Backend
- **Spring Boot 3.x** — Java microservices framework
- **PostgreSQL 16** — Bitemporal database with JSONB
- **Temporal.io 1.25** — Workflow orchestration
- **Apache Flink 1.18** — Stream processing
- **Kafka (Confluent 7.6)** — Event streaming
- **Spring AI** — AI integration framework
- **Spring Batch** — Batch processing (Parquet export)
- **JEXL** — Expression language
- **Caffeine** — In-memory caching

### Frontend
- **Next.js 16** — React framework with App Router
- **TypeScript 5** — Type-safe JavaScript
- **Tailwind CSS v4** — Utility-first styling
- **shadcn/ui + Radix UI** — Component library
- **dnd-kit** — Drag and drop
- **Zustand** — State management
- **TanStack Query** — Server state

### Infrastructure
- **Docker & Docker Compose** — Containerization (24+ services)
- **Kubernetes & Helm** — Orchestration (28 templates, HPA, ServiceMonitor)
- **Turborepo** — Monorepo build system with caching
- **pnpm** — Package manager (workspaces)
- **Maven** — Java build tool

### Observability
- **OpenTelemetry** — Distributed tracing instrumentation
- **Jaeger** — Trace collection and visualization
- **Prometheus** — Metrics collection and alerting
- **Grafana** — Unified observability dashboards
- **Micrometer** — Application metrics

### AI & LLMs
- **OpenRouter** — Unified LLM API
- Supports: GPT-4, Claude, Gemini, Llama 3.1, and more
- Function calling for tool use

## Documentation

### Platform Guides
- [Quick Start](QUICK_START.md)
- [Docker Setup](DOCKER_SETUP.md)
- [Dev Setup Guide](infra/DEV_SETUP.md)
- [Observability Guide](platform/observability/README.md)
- [K8s Deployment Guide](platform/DEPLOY.md)

### Analytics & Streaming
- [Analytics Quick Start](docs/QUICKSTART.md)
- [Batch API Reference](docs/api/BATCH_API.md)
- [Architecture Guide](docs/architecture/ANALYTICS_LAYER.md)
- [Deployment Guide](docs/deployment/ANALYTICS_DEPLOYMENT.md)

### beema-kernel
- [Temporal Workflow Guide](apps/beema-kernel/TEMPORAL_WORKFLOW_GUIDE.md)
- [Policy Workflow Guide](apps/beema-kernel/POLICY_WORKFLOW_GUIDE.md)
- [Message Processing Guide](apps/beema-kernel/MESSAGE_PROCESSING.md)
- [Metadata Cache Guide](apps/beema-kernel/METADATA_CACHE.md)
- [AI Agent Guide](apps/beema-kernel/AI_AGENT_GUIDE.md)
- [OpenRouter Setup](apps/beema-kernel/OPENROUTER_SETUP.md)

### Frontend Apps
- [Studio README](apps/studio/README.md)
- [Layout Builder Guide](apps/studio/LAYOUT_BUILDER.md)
- [UI Components](packages/ui/README.md)

## Example Workflows

### Analyze a Claim with AI
```bash
curl -X POST http://localhost:8080/api/v1/claims/analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "claimNumber": "CLM-2026-001",
    "claimType": "motor_accident",
    "claimAmount": 5000,
    "policyNumber": "POL-001",
    "marketContext": "RETAIL"
  }'
```

### Export Agreements to Parquet
```bash
curl -X POST http://localhost:8080/api/v1/batch/export/parquet \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-123" \
  -d '{"tenantId": "tenant-123"}'
```

### Monitor Observability Stack
```bash
# View traces in Jaeger
open http://localhost:16686

# Grafana dashboards (admin/admin)
open http://localhost:3002

# Prometheus metrics
open http://localhost:9090

# Verify stack
./platform/observability/verify-stack.sh
```

## Current Status

### Production Ready
- Metadata-driven kernel with JEXL expressions
- Temporal workflows (Policy, Claim)
- AI-powered claim analysis with OpenRouter
- Flink stream processing (message processor + speed layer)
- Visual form builder (Studio)
- Batch scheduling with per-tenant Temporal integration
- Analytics layer (Parquet export, cloud-agnostic blob storage, MinIO)
- Global Admin Console (tenant/region/datasource management)
- Observability stack (OpenTelemetry, Jaeger, Prometheus, Grafana)
- Policy & Claims Portal
- Docker Compose full stack (24+ services)

### In Development
- Keycloak authentication integration
- Inngest integration gateway (webhooks)
- Production Kubernetes deployments

### Planned
- Claims management workflows
- Document processing with OCR
- Analytics dashboard with real-time charts

## Troubleshooting

- **Build failures:** Clear cache with `rm -rf .turbo`
- **Docker issues:** Run `./docker-compose-verify.sh`
- **AI errors:** Verify `OPENROUTER_API_KEY` is set
- **Temporal issues:** Check http://localhost:8088
- **Missing node_modules:** Run `pnpm install` from project root
- **Flink not reachable:** Ensure `flink-jobmanager` is running: `docker compose up -d flink-jobmanager`

## Contributing

1. Create a feature branch
2. Make changes
3. Run tests: `pnpm test`
4. Run lint: `pnpm lint`
5. Build: `pnpm build`
6. Verify with Docker: `./docker-compose-verify.sh`
7. Submit PR

### Development Guidelines
- Follow metadata-driven architecture
- Support all market contexts (Retail, Commercial, London Market)
- Use JEXL for business rules
- Write comprehensive tests

## License

ISC

---

**Beema** — Modern Insurance Platform with AI-Powered Workflows
Built with Turborepo, Spring Boot, Next.js, Temporal.io, and Apache Flink
