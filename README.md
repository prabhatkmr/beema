# Beema Insurance Platform - Turborepo Monorepo

A modern, AI-powered insurance platform with metadata-driven architecture, event-driven workflows, and visual form builders.

**Tech Stack:** Spring Boot 3, Next.js, Temporal.io, Apache Flink, Kafka, PostgreSQL, OpenRouter AI

## 🏗️ Architecture

This is a **Turborepo monorepo** containing multiple microservices and shared packages:

```
beema/
├── apps/                           # Microservices
│   ├── beema-kernel/              # Core kernel (Spring Boot + Temporal + AI)
│   ├── studio/                    # Visual builder (Next.js + dnd-kit)
│   ├── beema-message-processor/   # Stream processor (Flink + Kafka)
│   ├── metadata-service/          # Schema registry (Spring Boot)
│   └── auth-service/              # Authentication (To be implemented)
├── packages/                       # Shared libraries
│   └── ui/                        # React components (Button, Card, Input, Label)
├── platform/                       # Kubernetes/Helm charts
├── docker-compose.yml             # Full stack (Temporal, Kafka, Postgres, etc.)
├── turbo.json                     # Turborepo pipeline config
└── package.json                   # Root workspace config
```

## ✨ Key Features

### 🤖 AI-Powered Intelligence
- **Claim Analysis:** GPT-4, Claude, or Llama analyze claims and recommend actions
- **5 AI Tools:** Field metadata, JEXL evaluation, business rules, validation, metrics
- **Multi-Provider:** OpenRouter gives access to 10+ AI providers
- **Auto Model Selection:** Smart routing based on claim complexity
- **Cost Optimization:** Up to 95% cost reduction using Llama vs GPT-4

### 🎨 Visual Form Builder
- **Drag & Drop:** Build forms visually with 9 field types
- **Real-time Validation:** Against beema-kernel metadata schema
- **Export/Import:** sys_layouts JSON format
- **Shared Components:** Reusable UI library (`@beema/ui`)

### ⚡ Event-Driven Architecture
- **Temporal Workflows:** Durable, retryable policy and claim workflows
- **Kafka Streaming:** Real-time message transformation with Flink
- **JEXL Expressions:** Pre-compiled for high performance
- **Database Hooks:** Event-driven with sys_workflow_hooks and sys_message_hooks

### 🔒 Enterprise-Grade
- **Bitemporal Data:** Valid time + Transaction time
- **Multi-Tenancy:** Row-Level Security
- **Metadata-Driven:** Zero-code field additions
- **Write Shield:** Mass assignment protection
- **Caching Layer:** Caffeine with 4-hour TTL

### 🌍 Market Support
All features support **Retail, Commercial, and London Market** contexts.

## 🚀 Quick Start

### Prerequisites

- **Node.js** >= 18.0.0
- **pnpm** >= 8.0.0 (or npm/yarn)
- **Java** >= 21
- **Maven** >= 3.8.0
- **Docker** & **Docker Compose**

### Installation

```bash
# Install dependencies
pnpm install

# Install Turborepo globally (optional)
pnpm add -g turbo
```

### Environment Configuration

Create `.env` files for each service:

**beema-kernel** (`apps/beema-kernel/.env`):
```bash
# OpenRouter AI
OPENROUTER_API_KEY=sk-or-v1-your-key-here
OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229

# Database
DATABASE_URL=jdbc:postgresql://localhost:5433/beema_kernel

# Temporal
TEMPORAL_HOST=localhost:7233
```

**studio** (`apps/studio/.env.local`):
```bash
# API
BEEMA_KERNEL_URL=http://localhost:8080
NEXT_PUBLIC_API_URL=http://localhost:3000
```

See `.env.example` files in each service for complete reference.

## 📦 Available Commands

### Build All Services

```bash
pnpm build
# or
turbo build
```

Builds all services in parallel with intelligent caching.

### Run Tests

```bash
pnpm test
# or
turbo test
```

Runs tests for all services. Tests are cached based on code changes.

### Lint

```bash
pnpm lint
# or
turbo lint
```

Runs linting (checkstyle for Java services).

### Development Mode

```bash
pnpm dev
# or
turbo dev
```

Starts all services in development mode with hot-reload.

### Clean

```bash
pnpm clean
# or
turbo clean
```

Removes all build artifacts (`target/`, `dist/`, etc.).

### Format Code

```bash
pnpm format
# or
turbo format
```

Formats code using Spotless (Java) or Prettier (JS/TS).

## 🔧 Working with Individual Services

### Run a specific service

```bash
# Build only beema-kernel
turbo build --filter=@beema/kernel

# Test only metadata-service
turbo test --filter=@beema/metadata-service

# Dev mode for beema-kernel
turbo dev --filter=@beema/kernel
```

### Navigate to a service

```bash
cd apps/beema-kernel
mvn spring-boot:run
```

## 🐳 Docker Compose

Start the complete platform with all dependencies:

```bash
docker-compose up -d
```

**Services included:**
- PostgreSQL 16 (port 5433) - Shared database
- Keycloak (port 8090) - Authentication
- Temporal Server (port 7233) - Workflow engine
- Temporal UI (port 8088) - Workflow monitoring
- Zookeeper + Kafka (port 9092) - Message broker
- **Jaeger (port 16686)** - Distributed tracing backend
- **Prometheus (port 9090)** - Metrics collection
- **Grafana (port 3001)** - Observability dashboards
- beema-kernel (port 8080) - Core API + Temporal worker
- metadata-service (port 8081) - Schema registry
- beema-message-processor - Flink streaming job
- studio (port 3000) - Visual builder

**Access URLs:**
- Studio UI: http://localhost:3000
- Beema Kernel API: http://localhost:8080/swagger-ui
- Temporal UI: http://localhost:8088
- Keycloak: http://localhost:8090
- **Grafana (Observability)**: http://localhost:3001 (admin/admin)
- **Jaeger (Tracing)**: http://localhost:16686
- **Prometheus (Metrics)**: http://localhost:9090

**Verify all services:**
```bash
./docker-compose-verify.sh

# Verify observability stack specifically
./platform/observability/verify-stack.sh
```

## ☸️ Kubernetes Deployment

Deploy to Kubernetes using Helm:

```bash
cd platform
helm install beema . -f values.yaml
```

## 📊 Turborepo Features

### Intelligent Caching

Turborepo caches build outputs based on:
- Source code changes
- Dependencies
- Environment variables
- Configuration files

**Result:** Only rebuild what changed. 10x faster builds!

### Parallel Execution

Services build in parallel respecting dependency graph:
```
metadata-service  ┐
                  ├─> beema-kernel (depends on metadata-service)
auth-service      ┘
```

### Remote Caching (Optional)

Enable remote caching for team collaboration:

```bash
# Login to Vercel (for remote cache)
turbo login

# Link to your team
turbo link
```

## 🏢 Applications & Services

### beema-kernel

**Core Agreement Kernel** - Bitemporal insurance agreement system with:
- Metadata-driven schema with JSONB flex-schema
- Pre-compiled JEXL expressions with Write Shield (mass assignment protection)
- Temporal workflow orchestration (PolicyWorkflow, ClaimWorkflow)
- **AI-Powered Claim Analysis** with OpenRouter (GPT-4, Claude, Llama)
- Multi-tenancy with Row-Level Security
- 5 AI-callable tools for intelligent decision making

**Tech:** Spring Boot 3, PostgreSQL, Temporal.io, Caffeine Cache, Spring AI

**Port:** 8080

**Key Features:**
- Policy lifecycle workflows (SUBMITTED → ISSUED)
- AI claim analyzer with 6 recommendation types
- Retryable activities with exponential backoff
- Metadata caching layer (4-hour TTL)

### studio

**Visual Form Builder** - Next.js application for designing layouts and blueprints:
- **Layout Builder:** Drag-and-drop form designer with 9 field types
- **Blueprint Editor:** Visual message mapping between systems
- **Canvas:** Sortable field blocks with properties panel
- **API Integration:** Validates against beema-kernel metadata schema

**Tech:** Next.js 16 (App Router), TypeScript, Tailwind CSS, dnd-kit, Zustand

**Port:** 3000

**Key Features:**
- Real-time validation with beema-kernel
- Export/import layouts as JSON (sys_layouts schema)
- Shared UI components from `@beema/ui`
- Three-panel interface (sidebar, canvas, properties)

### beema-message-processor

**Stream Processor** - Flink job for real-time message transformation:
- Reads from `raw-messages` Kafka topic
- Applies JEXL hooks from `sys_message_hooks` table
- Pre/Transform/Post processing pipeline
- Writes to `processed-messages` topic

**Tech:** Apache Flink 1.18, Kafka, PostgreSQL, JEXL

**Features:**
- Database-driven transformation rules
- Retail, Commercial, and London Market support
- Checkpointing and state management
- Error handling with fail_fast, log_continue, retry

### metadata-service

**Schema Registry** - Manages metadata definitions for:
- Field definitions and validation rules
- UI layouts and calculation rules
- Market-specific configurations

**Tech:** Spring Boot 3, PostgreSQL

**Port:** 8081

### auth-service

**Authentication Service** - OAuth2/JWT authentication (To be implemented)

**Port:** 8082

## 📦 Shared Packages

### @beema/ui

Shared React component library:
- **Button** - 4 variants (primary, secondary, outline, ghost)
- **Card** - Card, CardHeader, CardTitle, CardContent
- **Input** - Text input with error states
- **Label** - Form labels with required indicator

Used by Studio and future frontend applications.

## 🛠️ Adding a New Service

1. Create service in `apps/`:
```bash
mkdir -p apps/new-service
cd apps/new-service
```

2. Create `package.json`:
```json
{
  "name": "@beema/new-service",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "build": "mvn clean package -DskipTests",
    "test": "mvn test",
    "dev": "mvn spring-boot:run"
  }
}
```

3. Turborepo will automatically discover it!

## 📚 Documentation

### Platform Guides
- [Quick Start](QUICK_START.md) - 5-minute getting started
- [Docker Setup](DOCKER_SETUP.md) - Complete Docker guide
- [Turborepo Migration](TURBOREPO_MIGRATION.md) - Monorepo architecture
- **[Observability Guide](platform/observability/README.md)** - OpenTelemetry, Jaeger, Prometheus, Grafana

### beema-kernel
- [Temporal Workflow Guide](apps/beema-kernel/TEMPORAL_WORKFLOW_GUIDE.md) - Workflow system
- [Policy Workflow Guide](apps/beema-kernel/POLICY_WORKFLOW_GUIDE.md) - Policy state machine
- [Message Processing Guide](apps/beema-kernel/MESSAGE_PROCESSING.md) - Message hooks
- [Metadata Cache Guide](apps/beema-kernel/METADATA_CACHE.md) - Caching layer
- [AI Agent Guide](apps/beema-kernel/AI_AGENT_GUIDE.md) - AI integration
- [OpenRouter Setup](apps/beema-kernel/OPENROUTER_SETUP.md) - AI configuration
- [AI Quick Start](apps/beema-kernel/AI_QUICK_START.md) - Get started with AI

### studio
- [Studio README](apps/studio/README.md) - Application overview
- [Layout Builder Guide](apps/studio/LAYOUT_BUILDER.md) - Form builder
- [Architecture](apps/studio/ARCHITECTURE.md) - Technical design
- [API Documentation](apps/studio/app/api/layouts/README.md) - REST endpoints

### beema-message-processor
- [Flink Processor README](apps/beema-message-processor/README.md) - Stream processing
- [Configuration Guide](apps/beema-message-processor/CONFIG.md) - Setup

### Shared Packages
- [UI Components](packages/ui/README.md) - Component library

## 💡 Example Workflows

### Create and Analyze a Claim with AI

```bash
# 1. Start AI-powered claim analysis
curl -X POST http://localhost:8080/api/v1/claims/analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "claimNumber": "CLM-2026-001",
    "claimType": "motor_accident",
    "claimAmount": 5000,
    "policyNumber": "POL-001",
    "marketContext": "RETAIL"
  }'

# Response:
{
  "nextAction": "APPROVE_IMMEDIATELY",
  "confidence": 0.95,
  "reasoning": "Low-value claim, all validations pass..."
}
```

### Build a Form in Studio

```bash
# 1. Open Studio
open http://localhost:3000/canvas

# 2. Drag fields from sidebar to canvas
# 3. Configure field properties
# 4. Export as sys_layouts JSON
# 5. Save to beema-kernel for validation
```

### Process Messages with Flink

```bash
# 1. Send message to Kafka
echo '{"type":"claim","data":{...}}' | \
  kafka-console-producer --topic raw-messages

# 2. Flink applies JEXL hooks from sys_message_hooks
# 3. Transformed message appears in processed-messages topic
```

### Monitor Workflows in Temporal

```bash
# 1. Open Temporal UI
open http://localhost:8088

# 2. View PolicyWorkflow executions
# 3. See activity retries, signals, queries
# 4. Debug failed workflows
```

### Monitor Application with Observability Stack

```bash
# 1. Generate sample traces
for i in {1..10}; do
  curl http://localhost:8080/actuator/health
  sleep 1
done

# 2. View traces in Jaeger
open http://localhost:16686
# Search: Service = "beema-kernel"

# 3. Import Spring Boot dashboard in Grafana
open http://localhost:3001
# Login: admin/admin
# Dashboards → Import → ID: 19004

# 4. Query metrics in Prometheus
open http://localhost:9090
# Query: rate(http_server_requests_seconds_count[5m])

# 5. Verify observability stack
./platform/observability/verify-stack.sh
```

## 🧪 Testing

### Unit Tests

```bash
turbo test
```

### Integration Tests (Requires Docker)

```bash
# Start dependencies
docker-compose up -d postgres temporal

# Run integration tests
cd apps/beema-kernel
mvn test -Dtest=*Integration*
```

## 🔍 Monitoring Cache Performance

```bash
# View Turborepo cache stats
turbo run build --summarize

# Clear Turborepo cache
rm -rf .turbo
```

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.x** - Java microservices framework
- **PostgreSQL 16** - Bitemporal database with JSONB
- **Temporal.io** - Workflow orchestration
- **Apache Flink 1.18** - Stream processing
- **Kafka** - Event streaming
- **Spring AI** - AI integration framework
- **JEXL** - Expression language
- **Caffeine** - In-memory caching

### Frontend
- **Next.js 16** - React framework with App Router
- **TypeScript 5** - Type-safe JavaScript
- **Tailwind CSS v4** - Utility-first styling
- **dnd-kit** - Drag and drop
- **Zustand** - State management
- **TanStack Query** - Server state

### Infrastructure
- **Docker & Docker Compose** - Containerization
- **Kubernetes & Helm** - Orchestration
- **Turborepo** - Monorepo build system
- **pnpm** - Package manager
- **Maven** - Java build tool

### Observability
- **OpenTelemetry** - Distributed tracing instrumentation
- **Jaeger** - Trace collection and visualization
- **Prometheus** - Metrics collection and alerting
- **Grafana** - Unified observability dashboards
- **Micrometer** - Application metrics

### AI & LLMs
- **OpenRouter** - Unified LLM API
- Supports: GPT-4, Claude 3, Gemini, Llama 3.1, and more
- Function calling for tool use

## 🤝 Contributing

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
- Document all new features

## 📄 License

ISC

## 🚧 Current Status

### Production Ready
✅ Metadata-driven kernel with JEXL expressions
✅ Temporal workflows (Policy, Claim)
✅ AI-powered claim analysis with OpenRouter
✅ Flink stream processing
✅ Visual form builder (Studio)
✅ **Observability stack (OpenTelemetry, Jaeger, Prometheus, Grafana)**
✅ Docker Compose full stack

### In Development
🔄 Keycloak authentication integration
🔄 Inngest integration gateway (webhooks)
🔄 Production Kubernetes deployments

### Planned
📋 Claims management UI
📋 Policy administration UI
📋 Analytics dashboard
📋 Document processing with OCR

## 🆘 Support & Resources

### Getting Help
- Check the [Documentation](#-documentation) section
- Review service-specific READMEs
- See `.env.example` files for configuration

### Troubleshooting
- **Build failures:** Clear cache with `rm -rf .turbo`
- **Docker issues:** Run `./docker-compose-verify.sh`
- **AI errors:** Verify `OPENROUTER_API_KEY` is set
- **Temporal issues:** Check http://localhost:8088

## 🔗 Links

- [Turborepo Docs](https://turbo.build/repo/docs)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Temporal.io](https://temporal.io)
- [Next.js](https://nextjs.org)
- [OpenRouter](https://openrouter.ai)
- [Apache Flink](https://flink.apache.org)

---

**Beema** - Modern Insurance Platform with AI-Powered Workflows
Built with ❤️ using Turborepo, Spring Boot, Next.js, and Temporal.io
