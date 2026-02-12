# Beema Platform - Helm Deployment Guide

Complete guide for deploying the Beema platform to Kubernetes using the updated Helm charts.

## Prerequisites

- Kubernetes cluster (1.28+)
- Helm 3.12+
- kubectl configured
- Sufficient cluster resources:
  - 8 CPU cores
  - 16 GB RAM
  - 50 GB storage

## Quick Start

### 1. Add Helm Chart Repositories

```bash
# Add Bitnami repository (for PostgreSQL, Kafka, Keycloak)
helm repo add bitnami https://charts.bitnami.com/bitnami

# Add Prometheus community (for observability stack)
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Add Jaeger (for distributed tracing)
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts

# Update repositories
helm repo update
```

### 2. Create Namespace

```bash
kubectl create namespace beema
```

### 3. Create Secrets

```bash
# Database credentials
kubectl create secret generic beema-db-credentials \
  --from-literal=username=beema \
  --from-literal=password='changeme' \
  -n beema

# OpenRouter AI credentials
kubectl create secret generic beema-ai-credentials \
  --from-literal=api-key='YOUR_OPENROUTER_API_KEY' \
  -n beema

# Inngest credentials
kubectl create secret generic beema-inngest-credentials \
  --from-literal=event-key='local' \
  --from-literal=signing-key='test-signing-key' \
  -n beema
```

### 4. Install Dependencies (Optional)

If using external chart dependencies:

```bash
cd platform
helm dependency update
```

### 5. Deploy Beema Platform

#### Option A: Full Stack (All Services + Infrastructure)

```bash
helm install beema ./platform \
  --namespace beema \
  --values platform/values-updated.yaml \
  --set postgresql.enabled=true \
  --set kafka.enabled=true \
  --set keycloak.enabled=true \
  --set observability.prometheus.enabled=true \
  --set observability.jaeger.enabled=true
```

#### Option B: Apps Only (Use Existing Infrastructure)

```bash
helm install beema ./platform \
  --namespace beema \
  --values platform/values-updated.yaml \
  --set postgresql.enabled=false \
  --set kafka.enabled=false \
  --set keycloak.enabled=false \
  --set postgresql.external.host=my-postgres.example.com \
  --set kafka.external.bootstrapServers=my-kafka.example.com:9092
```

#### Option C: Specific Services Only

```bash
helm install beema ./platform \
  --namespace beema \
  --values platform/values-updated.yaml \
  --set beemaKernel.enabled=true \
  --set metadataService.enabled=true \
  --set messageProcessor.enabled=false \
  --set studio.enabled=false
```

### 6. Verify Deployment

```bash
# Check pod status
kubectl get pods -n beema

# Check services
kubectl get svc -n beema

# Check ingress (if enabled)
kubectl get ingress -n beema

# View logs
kubectl logs -f deployment/beema-beema-kernel -n beema
```

## Configuration

### Update Values

The main configuration file is `values-updated.yaml`. Key sections:

```yaml
# Application images
beemaKernel:
  image:
    registry: ghcr.io
    repository: beema/beema-kernel
    tag: "0.1.0-SNAPSHOT"

# Resource limits
beemaKernel:
  resources:
    requests:
      cpu: 500m
      memory: 1Gi
    limits:
      cpu: 2
      memory: 2Gi

# Ingress configuration
studio:
  ingress:
    enabled: true
    hosts:
      - host: studio.beema.example.com
```

### Environment Variables

Override environment variables per service:

```bash
helm install beema ./platform \
  --set beemaKernel.env.SPRING_PROFILES_ACTIVE=prod,custom \
  --set studio.env.NEXT_PUBLIC_API_BASE_URL=https://api.custom.com
```

## Services Deployed

| Service | Port | Description |
|---------|------|-------------|
| **beema-kernel** | 8080 | Core application + REST API |
| **metadata-service** | 8082 | Message hook registry |
| **studio** | 3000 | Next.js frontend |
| **inngest** | 8288 | Event orchestration |
| **flink-jobmanager** | 8081 | Flink web UI |
| **postgresql** | 5432 | Database |
| **kafka** | 9092 | Message broker |
| **keycloak** | 8080 | Authentication |
| **jaeger** | 16686 | Tracing UI |
| **prometheus** | 9090 | Metrics |
| **grafana** | 3000 | Dashboards |

## Accessing Services

### Via Port Forward

```bash
# Kernel API
kubectl port-forward svc/beema-beema-kernel 8080:8080 -n beema

# Studio Frontend
kubectl port-forward svc/beema-studio 3000:3000 -n beema

# Flink Dashboard
kubectl port-forward svc/beema-flink-jobmanager 8081:8081 -n beema

# Jaeger UI
kubectl port-forward svc/beema-jaeger-query 16686:16686 -n beema
```

### Via Ingress

If ingress is enabled, access via configured hostnames:
- Studio: `https://studio.beema.example.com`
- API: `https://api.beema.example.com`

## Scaling

### Manual Scaling

```bash
# Scale kernel
kubectl scale deployment beema-beema-kernel --replicas=5 -n beema

# Scale metadata service
kubectl scale deployment beema-metadata --replicas=3 -n beema
```

### Auto-scaling (HPA)

Horizontal Pod Autoscalers are configured in `values-updated.yaml`:

```yaml
beemaKernel:
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 75
```

Check HPA status:

```bash
kubectl get hpa -n beema
```

## Monitoring

### Prometheus Metrics

Metrics are exposed at `/actuator/prometheus` for Spring Boot services.

ServiceMonitors are automatically created for:
- beema-kernel
- metadata-service

### View Metrics

```bash
# Port forward Prometheus
kubectl port-forward svc/beema-kube-prometheus-prometheus 9090:9090 -n beema

# Visit http://localhost:9090
```

### Grafana Dashboards

```bash
# Port forward Grafana
kubectl port-forward svc/beema-grafana 3000:80 -n beema

# Visit http://localhost:3000
# Default credentials: admin/admin (change after first login)
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod events
kubectl describe pod <pod-name> -n beema

# Check logs
kubectl logs <pod-name> -n beema --previous

# Check init container logs
kubectl logs <pod-name> -c flyway-migrate -n beema
```

### Database Connection Issues

```bash
# Test PostgreSQL connectivity
kubectl run -it --rm psql-test \
  --image=postgres:16 \
  --restart=Never \
  -n beema \
  -- psql -h beema-postgresql -U beema -d beema_kernel
```

### Kafka Issues

```bash
# Check Kafka topics
kubectl exec -it beema-kafka-0 -n beema -- \
  kafka-topics --bootstrap-server localhost:9092 --list

# Check topic messages
kubectl exec -it beema-kafka-0 -n beema -- \
  kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic beema-events --from-beginning --max-messages 10
```

### Flink Job Issues

```bash
# Check Flink JobManager logs
kubectl logs deployment/beema-flink-jobmanager -n beema

# Access Flink Web UI
kubectl port-forward svc/beema-flink-jobmanager 8081:8081 -n beema
# Visit http://localhost:8081
```

## Upgrade

```bash
# Update Helm chart
helm upgrade beema ./platform \
  --namespace beema \
  --values platform/values-updated.yaml \
  --reuse-values

# With specific changes
helm upgrade beema ./platform \
  --namespace beema \
  --set beemaKernel.image.tag=0.2.0
```

## Rollback

```bash
# List releases
helm history beema -n beema

# Rollback to previous version
helm rollback beema -n beema

# Rollback to specific revision
helm rollback beema 2 -n beema
```

## Uninstall

```bash
# Uninstall release
helm uninstall beema -n beema

# Delete namespace
kubectl delete namespace beema

# Delete PVCs (if not using dynamic provisioning)
kubectl delete pvc --all -n beema
```

## Production Considerations

### 1. Use Sealed Secrets

```bash
# Install Sealed Secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create sealed secret
kubeseal --format yaml < secret.yaml > sealed-secret.yaml
kubectl apply -f sealed-secret.yaml -n beema
```

### 2. Enable TLS

```bash
# Install cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --set installCRDs=true

# Create ClusterIssuer
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@beema.example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### 3. Configure Backups

```bash
# Install Velero for cluster backups
velero install --provider aws --bucket beema-backups

# Create backup
velero backup create beema-backup --include-namespaces beema
```

### 4. Network Policies

Network policies are included in the chart. Enable with:

```yaml
networkPolicy:
  enabled: true
```

### 5. Pod Security Standards

```bash
# Label namespace for restricted PSS
kubectl label namespace beema \
  pod-security.kubernetes.io/enforce=restricted \
  pod-security.kubernetes.io/audit=restricted \
  pod-security.kubernetes.io/warn=restricted
```

## Support

For issues or questions:
- GitHub Issues: https://github.com/beema/platform/issues
- Documentation: See individual service READMEs in `apps/*/`
- Helm Chart: `platform/HELM_STATUS.md`
