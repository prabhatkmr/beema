# Helm Charts Update Plan

## Current Gap Analysis

### Missing Services (Critical)
1. **metadata-service** - Message hook registry + Kafka publisher
2. **beema-message-processor** - Flink streaming job
3. **studio** - Next.js frontend application
4. **inngest** - Event orchestration platform

### Missing Infrastructure (Critical)
5. **PostgreSQL** - Database (referenced but not deployed)
6. **Kafka + Zookeeper** - Message broker
7. **Flink Cluster** - Stream processing (JobManager + TaskManager)
8. **Keycloak** - Authentication/Authorization
9. **Jaeger** - Distributed tracing
10. **Prometheus** - Metrics collection (has config files but no deployment)
11. **Grafana** - Dashboards (has config files but no deployment)

### Environment Variable Gaps
- Temporal configuration updated (task queue names changed)
- AI/OpenRouter configuration missing
- Inngest integration environment variables missing
- Kafka control topic configuration missing

## Recommended Approach

### Option 1: Helm Chart per Service (Microservices Best Practice)
```
platform/
├── charts/
│   ├── beema-kernel/           # Main application chart
│   ├── metadata-service/       # Metadata service chart
│   ├── message-processor/      # Flink job chart
│   ├── studio/                 # Frontend chart
│   └── infrastructure/         # Shared infrastructure
│       ├── kafka/             # Kafka subchart
│       ├── postgresql/        # PostgreSQL subchart
│       ├── keycloak/          # Keycloak subchart
│       └── observability/     # Jaeger, Prometheus, Grafana
└── Chart.yaml                  # Umbrella chart with dependencies
```

**Pros:**
- Independent versioning and deployment
- Team ownership per chart
- Easier to manage
- Can use community charts (Bitnami, etc.)

**Cons:**
- More complex initial setup
- Requires Helm dependencies management

### Option 2: Monolithic Chart with Conditional Deployment (Current Approach)
```
platform/
├── templates/
│   ├── kernel/                 # beema-kernel manifests
│   ├── metadata/               # metadata-service manifests
│   ├── processor/              # message-processor manifests
│   ├── studio/                 # studio manifests
│   └── infrastructure/         # All infra manifests
├── values.yaml                 # Single values file with enable flags
└── Chart.yaml
```

**Pros:**
- Single `helm install` command
- Easier for simple deployments
- All configurations in one place

**Cons:**
- Large values.yaml file
- Tight coupling
- Harder to version independently

## Recommendation: Hybrid Approach

Use **umbrella chart** with external dependencies for infrastructure:

```yaml
# platform/Chart.yaml
dependencies:
  - name: postgresql
    version: 15.2.5
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled

  - name: kafka
    version: 28.2.1
    repository: https://charts.bitnami.com/bitnami
    condition: kafka.enabled

  - name: keycloak
    version: 21.4.4
    repository: https://charts.bitnami.com/bitnami
    condition: keycloak.enabled

  - name: jaeger
    version: 3.1.1
    repository: https://jaegertracing.github.io/helm-charts
    condition: observability.jaeger.enabled
```

Then create custom templates for Beema-specific services.

## Implementation Tasks

### Phase 1: Infrastructure Dependencies
- [ ] Add PostgreSQL subchart configuration
- [ ] Add Kafka subchart configuration
- [ ] Add Keycloak subchart configuration
- [ ] Add Flink operator or static cluster
- [ ] Add Jaeger/Prometheus/Grafana stack

### Phase 2: Application Services
- [ ] Update beema-kernel deployment with new env vars
- [ ] Create metadata-service deployment
- [ ] Create beema-message-processor Flink job
- [ ] Create studio deployment
- [ ] Create inngest deployment

### Phase 3: Configuration Alignment
- [ ] Align environment variables with docker-compose.yml
- [ ] Add Temporal task queue configurations
- [ ] Add OpenRouter AI configuration
- [ ] Add Kafka topic initialization job
- [ ] Add secrets management (SealedSecrets or External Secrets)

### Phase 4: Networking & Ingress
- [ ] Create Ingress for Studio
- [ ] Create Ingress for Kernel API
- [ ] Configure TLS certificates
- [ ] Set up service mesh (optional: Istio/Linkerd)

### Phase 5: Production Readiness
- [ ] PodDisruptionBudgets
- [ ] NetworkPolicies
- [ ] ResourceQuotas
- [ ] RBAC configurations
- [ ] ServiceMonitors for Prometheus

## Quick Start: Minimal Update

For immediate use, I'll create a minimal update that:
1. Adds metadata-service deployment
2. Adds message-processor Flink job
3. Adds studio frontend
4. References external PostgreSQL/Kafka (assumes they're already deployed)
5. Updates environment variables to match current setup

This allows deploying Beema apps while using managed services or separate infrastructure charts.
