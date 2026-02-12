# Helm/Kubernetes Status Report

## ❌ Current State: OUTDATED

The Helm charts are **significantly out of sync** with the current Docker Compose setup and need substantial updates.

## 📊 Gap Analysis

### What Exists in Helm Charts ✅
- ✅ **beema-kernel** deployment (basic configuration)
- ✅ **temporal-worker** deployment (separate from kernel)
- ✅ Observability config files (Prometheus, Grafana datasources)
- ✅ Basic HPA, Service, ConfigMap templates

### What's Missing in Helm Charts ❌

#### Application Services
- ❌ **metadata-service** - Message hook registry + Kafka publisher
- ❌ **beema-message-processor** - Flink streaming job deployment
- ❌ **studio** - Next.js frontend application
- ❌ **inngest** - Event orchestration platform

#### Infrastructure Components
- ❌ **PostgreSQL** - Database (referenced but not deployed)
- ❌ **Kafka + Zookeeper** - Message broker (or Redpanda alternative)
- ❌ **Flink Cluster** - Stream processing (JobManager + TaskManager)
- ❌ **Keycloak** - Authentication/Authorization
- ❌ **Jaeger** - Distributed tracing deployment
- ❌ **Prometheus** - Metrics collection deployment
- ❌ **Grafana** - Dashboard deployment
- ❌ **Kafka Topics** - Initialization job for topics

#### Configuration Gaps
- ❌ **OpenRouter AI** - Environment variables and secrets
- ❌ **Inngest Integration** - Event key and signing key
- ❌ **Updated Temporal config** - Task queue names changed from `BEEMA_AGREEMENT_TASK_QUEUE` to `POLICY_TASK_QUEUE`
- ❌ **Kafka control topic** - `message-hooks-control` configuration
- ❌ **Multiple databases** - beema_kernel, beema_metadata, keycloak
- ❌ **Spring profiles** - Dev vs prod configuration

## 📝 What Needs to Be Done

### Priority 1: Critical Application Services (1-2 days)

1. **Create metadata-service deployment**
   ```bash
   platform/templates/metadata/
   ├── deployment.yaml
   ├── service.yaml
   ├── hpa.yaml
   └── servicemonitor.yaml
   ```

2. **Create message-processor Flink job**
   ```bash
   platform/templates/processor/
   ├── flink-job.yaml        # FlinkDeployment CRD or Job
   ├── configmap.yaml
   └── servicemonitor.yaml
   ```

3. **Create studio frontend deployment**
   ```bash
   platform/templates/studio/
   ├── deployment.yaml
   ├── service.yaml
   ├── ingress.yaml
   └── hpa.yaml
   ```

4. **Update beema-kernel configuration**
   - Add OpenRouter API key secret reference
   - Add Inngest credentials
   - Update Temporal task queue name
   - Add metadata service URL

### Priority 2: Infrastructure Dependencies (2-3 days)

5. **Add Helm chart dependencies** (Recommended approach)
   ```yaml
   # platform/Chart.yaml
   dependencies:
     - name: postgresql
       version: 15.2.5
       repository: https://charts.bitnami.com/bitnami

     - name: kafka
       version: 28.2.1
       repository: https://charts.bitnami.com/bitnami

     - name: keycloak
       version: 21.4.4
       repository: https://charts.bitnami.com/bitnami

     - name: kube-prometheus-stack
       version: 58.0.0
       repository: https://prometheus-community.github.io/helm-charts

     - name: jaeger
       version: 3.1.1
       repository: https://jaegertracing.github.io/helm-charts
   ```

6. **Configure Flink operator or static cluster**
   - Use [Flink Kubernetes Operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/)
   - Or deploy static JobManager + TaskManager

7. **Add Inngest deployment**
   ```bash
   platform/templates/inngest/
   ├── deployment.yaml
   ├── service.yaml
   └── pvc.yaml
   ```

### Priority 3: Configuration & Secrets (1 day)

8. **Create Kubernetes Secrets**
   ```yaml
   # Using Sealed Secrets or External Secrets Operator
   - beema-db-credentials      # PostgreSQL username/password
   - beema-ai-credentials      # OpenRouter API key
   - beema-inngest-credentials # Event key, signing key
   - keycloak-admin           # Keycloak admin credentials
   ```

9. **Create Kafka topics initialization Job**
   ```yaml
   # platform/templates/kafka/topics-init-job.yaml
   # Creates: raw-messages, beema-events, message-hooks-control
   ```

10. **Update values.yaml with all environment variables**
    - See `platform/values-updated.yaml` for complete reference

### Priority 4: Networking & Production Readiness (1-2 days)

11. **Create Ingress resources**
    - Studio frontend: `studio.beema.example.com`
    - Kernel API: `api.beema.example.com`
    - Keycloak: `auth.beema.example.com`

12. **Add production hardening**
    - PodDisruptionBudgets for all services
    - NetworkPolicies for service isolation
    - ResourceQuotas per namespace
    - RBAC roles and service accounts
    - Pod Security Standards

13. **Configure monitoring**
    - ServiceMonitors for all services
    - PrometheusRules for alerts
    - Grafana dashboards

## 🚀 Recommended Implementation Path

### Phase 1: Quick Win (Use External Infrastructure)
**Goal**: Deploy Beema apps assuming infrastructure exists (managed services or separate charts)

1. Update existing templates for kernel + worker
2. Add metadata-service templates
3. Add message-processor templates
4. Add studio templates
5. Configure to use external PostgreSQL/Kafka/Temporal endpoints

**Timeline**: 2-3 days
**Benefit**: Can deploy to Kubernetes immediately using managed services (RDS, MSK, etc.)

### Phase 2: Infrastructure as Code (Full Self-Contained)
**Goal**: Complete Helm chart that deploys everything

1. Add chart dependencies (Bitnami charts)
2. Configure Flink operator
3. Add Inngest deployment
4. Complete observability stack
5. Kafka topics initialization

**Timeline**: 1 week
**Benefit**: Can deploy entire platform with `helm install beema ./platform`

### Phase 3: Production Hardening
**Goal**: Production-ready with best practices

1. Secrets management (Sealed Secrets/External Secrets)
2. NetworkPolicies and RBAC
3. High availability configurations
4. Disaster recovery (backups, snapshots)
5. CI/CD integration

**Timeline**: 1-2 weeks
**Benefit**: Production-grade deployment ready for real workloads

## 📦 Deliverables Created

I've created the following files to help with the update:

1. **`platform/values-updated.yaml`**
   - Complete values file with all services configured
   - Includes all missing environment variables
   - Matches current docker-compose.yml setup
   - Ready to use as reference

2. **`platform/HELM_UPDATE_PLAN.md`**
   - Detailed technical plan
   - Architecture recommendations
   - Task breakdown by phase

3. **`HELM_STATUS.md`** (this file)
   - Current state assessment
   - Gap analysis
   - Implementation roadmap

## 🎯 Immediate Next Steps

To bring Helm charts up to date, choose one path:

### Option A: Manual Update (Best for Learning)
```bash
# 1. Review the updated values file
cat platform/values-updated.yaml

# 2. Create missing templates (metadata, processor, studio)
mkdir -p platform/templates/{metadata,processor,studio}

# 3. Copy and adapt from existing kernel templates
# 4. Test locally with minikube or kind
helm install beema ./platform --dry-run --debug

# 5. Deploy to dev cluster
helm install beema ./platform -n beema --create-namespace
```

### Option B: Automated Generation (Faster)
```bash
# Use a tool to generate Helm templates
# I can create the complete updated Helm chart with all templates
```

### Option C: Use Existing Infrastructure
```bash
# Deploy only Beema apps, assume PostgreSQL/Kafka/etc exist
# Update values.yaml to point to existing services
# This is fastest path to Kubernetes deployment
```

## ⚠️ Breaking Changes from Current Helm Chart

If you deploy the updated Helm chart, be aware:

1. **Database connection** - Now supports multiple databases (kernel, metadata, keycloak)
2. **Temporal task queue** - Changed from `BEEMA_AGREEMENT_TASK_QUEUE` to `POLICY_TASK_QUEUE`
3. **New secrets required** - AI credentials, Inngest credentials
4. **Kafka dependency** - Apps now depend on Kafka being available
5. **Flink cluster** - Message processor needs Flink JobManager

## 📚 References

- [Bitnami PostgreSQL Chart](https://github.com/bitnami/charts/tree/main/bitnami/postgresql)
- [Bitnami Kafka Chart](https://github.com/bitnami/charts/tree/main/bitnami/kafka)
- [Flink Kubernetes Operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/)
- [Temporal Helm Charts](https://github.com/temporalio/helm-charts)
- [Kube Prometheus Stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)

---

**Status**: ❌ Helm charts are outdated and need significant updates before production use.

**Recommendation**: Implement Phase 1 (Quick Win) first to get services running on Kubernetes, then progressively add infrastructure components.
