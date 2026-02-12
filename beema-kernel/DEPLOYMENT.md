# Beema Kernel - Deployment Guide

Production deployment guide for the Unified Agreement Kernel.

## Prerequisites

1. **Kubernetes cluster** (EKS from `/infra`)
2. **PostgreSQL database** (RDS from `/infra`)
3. **Docker registry** (ECR, Docker Hub, or private registry)
4. **kubectl** configured for your cluster
5. **helm** 3.x installed

## Quick Start

```bash
# 1. Build Docker image
cd beema-kernel
docker build -t beema-kernel:1.0.0 .

# 2. Push to registry
docker tag beema-kernel:1.0.0 YOUR_REGISTRY/beema-kernel:1.0.0
docker push YOUR_REGISTRY/beema-kernel:1.0.0

# 3. Deploy to Kubernetes
cd ../platform
helm install beema-kernel ./charts/beema-kernel \
  --set image.repository=YOUR_REGISTRY/beema-kernel \
  --set image.tag=1.0.0 \
  --set postgresql.enabled=false \
  --set externalDatabase.host=YOUR_RDS_ENDPOINT
```

## Configuration

### 1. Database Secrets

Create Kubernetes secret for database credentials:

```bash
kubectl create secret generic beema-db-secret \
  --from-literal=password='YOUR_DB_PASSWORD'
```

Or use AWS Secrets Manager (recommended for EKS):

```yaml
# external-secrets.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: beema-db-secret
spec:
  secretStoreRef:
    name: aws-secrets-manager
  target:
    name: beema-db-secret
  data:
    - secretKey: password
      remoteRef:
        key: beema-prod-db-master-password
```

### 2. Environment Variables

Update `platform/values.yaml`:

```yaml
env:
  DB_HOST: beema-prod-postgres.xxx.us-east-1.rds.amazonaws.com
  DB_PORT: "5432"
  DB_NAME: beema_prod
  DB_USERNAME: beema_admin
  SPRING_PROFILE: prod
  LOG_LEVEL: INFO
```

### 3. Resource Limits

Recommended settings for production:

```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

### 4. HikariCP Connection Pool

Configure in `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Health Checks

Kubernetes readiness and liveness probes:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## Monitoring

### Prometheus Metrics

Metrics endpoint: `http://beema-kernel:8080/actuator/prometheus`

ServiceMonitor for Prometheus Operator:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: beema-kernel
spec:
  selector:
    matchLabels:
      app: beema-kernel
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
```

### Key Metrics

- `beema.agreements.count{market_context="RETAIL"}` - Agreement count by market
- `beema.agreements.by_status{status="ACTIVE"}` - Agreement count by status
- `hikaricp_connections_active` - Active database connections
- `jvm_memory_used_bytes` - JVM memory usage
- `http_server_requests_seconds` - HTTP request duration

### Grafana Dashboard

Import dashboard template from `platform/grafana/beema-kernel-dashboard.json`

## Scaling

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: beema-kernel
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: beema-kernel
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

### Database Connection Pool Sizing

Formula: `connections = ((core_count * 2) + effective_spindle_count)`

For RDS db.r6g.xlarge (4 vCPU):
- Recommended: 20 connections per pod
- With 3 pods: 60 total connections
- RDS max connections: 200 (leaves headroom)

## Performance Tuning

### 1. Query Optimization

Enable query logging in dev/staging:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        use_sql_comments: true
        format_sql: true
```

Monitor slow queries:

```sql
-- PostgreSQL slow query log
ALTER DATABASE beema_prod SET log_min_duration_statement = 1000; -- 1 second
```

### 2. Index Usage

Verify indexes are being used:

```sql
EXPLAIN ANALYZE
SELECT * FROM agreements
WHERE tenant_id = 'tenant-123'
  AND is_current = TRUE;
```

### 3. Cache Hit Rates

Monitor Caffeine cache statistics via JMX or Micrometer.

## Troubleshooting

### Pod Not Starting

```bash
# Check pod logs
kubectl logs -f deployment/beema-kernel

# Check events
kubectl describe pod beema-kernel-xxx

# Check database connectivity
kubectl exec -it deployment/beema-kernel -- \
  psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME
```

### High Memory Usage

```bash
# Get heap dump
kubectl exec deployment/beema-kernel -- \
  jcmd 1 GC.heap_dump /tmp/heap.hprof

# Copy heap dump
kubectl cp beema-kernel-xxx:/tmp/heap.hprof ./heap.hprof

# Analyze with Eclipse MAT or VisualVM
```

### Database Connection Pool Exhausted

Check HikariCP metrics:

```bash
curl http://beema-kernel:8080/actuator/metrics/hikaricp.connections.active
curl http://beema-kernel:8080/actuator/metrics/hikaricp.connections.idle
```

Increase pool size if needed:

```yaml
env:
  DB_POOL_SIZE: "30"  # Increase from 20
```

### Slow Queries

Enable Hibernate statistics:

```yaml
env:
  HIBERNATE_STATS: "true"
```

Check metrics:

```bash
curl http://beema-kernel:8080/actuator/metrics | grep hibernate
```

## Disaster Recovery

### Database Backups

RDS automated backups (from `/infra`):
- Retention: 30 days
- Backup window: 2:00-3:00 UTC
- Snapshots: Daily

### Point-in-Time Recovery

```bash
# Restore to specific time
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier beema-prod-postgres \
  --target-db-instance-identifier beema-prod-postgres-recovery \
  --restore-time 2024-02-12T10:00:00Z
```

### Application State

No application state is stored in pods. All state is in PostgreSQL.
Pods are stateless and can be recreated at any time.

## Security Checklist

- [ ] Database credentials in Secrets Manager
- [ ] TLS/SSL for database connections
- [ ] Network policies restricting pod-to-pod traffic
- [ ] Pod security policies enforced
- [ ] Container image scanning enabled
- [ ] RBAC configured with least privilege
- [ ] Secrets encrypted at rest (KMS)
- [ ] Audit logging enabled

## Production Readiness Checklist

- [ ] Docker image built and pushed to registry
- [ ] Database migrations tested
- [ ] Health checks passing
- [ ] Metrics exposed and scraped by Prometheus
- [ ] Grafana dashboards created
- [ ] Alerts configured (CPU, memory, errors)
- [ ] Resource limits set
- [ ] HPA configured
- [ ] Logs aggregated (CloudWatch/ELK)
- [ ] Database backups verified
- [ ] DR plan tested
- [ ] Load testing completed
- [ ] Security scan passed

---

**Version**: 1.0.0
**Last Updated**: 2026-02-12
