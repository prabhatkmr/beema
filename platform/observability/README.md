# Beema Observability Stack

Complete observability setup with OpenTelemetry, Jaeger, Prometheus, and Grafana.

## 🏗️ Architecture

```
┌─────────────────┐
│  Beema Kernel   │──── Metrics ────▶ Prometheus ──┐
│  (Spring Boot)  │                                 │
└─────────────────┘                                 │
         │                                          │
    Traces (OTLP)                                   │
         │                                          ▼
         ▼                                   ┌──────────┐
   ┌──────────┐                              │ Grafana  │
   │  Jaeger  │─────────────────────────────▶│          │
   └──────────┘          Traces              └──────────┘
         │
    Trace Metrics
         │
         └──────────────────────▶ Prometheus
```

## 📊 Components

### Jaeger (Port 16686)
- **Purpose:** Distributed tracing backend
- **UI:** http://localhost:16686
- **Features:**
  - Trace visualization and search
  - Service dependency graph
  - Performance analysis
  - Root cause analysis

### Prometheus (Port 9090)
- **Purpose:** Metrics collection and storage
- **UI:** http://localhost:9090
- **Scrapes:**
  - beema-kernel: `/actuator/prometheus` every 10s
  - metadata-service: `/actuator/prometheus` every 10s
  - temporal: `/metrics`
  - jaeger: `/metrics`

### Grafana (Port 3001)
- **Purpose:** Unified observability dashboard
- **UI:** http://localhost:3001
- **Credentials:** admin / admin
- **Pre-configured:**
  - Prometheus datasource (metrics)
  - Jaeger datasource (traces)
  - Trace-to-metrics correlation

## 🚀 Quick Start

### 1. Start the Stack

```bash
# Start all services
docker-compose up -d

# Check services are healthy
docker-compose ps

# Watch beema-kernel logs
docker-compose logs -f beema-kernel
```

### 2. Access UIs

| Service | URL | Description |
|---------|-----|-------------|
| Grafana | http://localhost:3001 | Main observability dashboard |
| Jaeger | http://localhost:16686 | Trace search and visualization |
| Prometheus | http://localhost:9090 | Metrics explorer and alerting |
| Beema Kernel API | http://localhost:8080/swagger-ui | Generate traces/metrics |

### 3. Generate Sample Traces

```bash
# Create some API requests to generate traces
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
curl http://localhost:8080/api/v1/metadata/types

# View traces in Jaeger
open http://localhost:16686
# Search for service: beema-kernel
```

## 📈 Grafana Dashboards

### Import Pre-built Dashboards

Grafana has thousands of community dashboards. Here are recommended ones for Spring Boot:

1. **Spring Boot 3.x Dashboard** (ID: 19004)
   - Go to Grafana → Dashboards → Import
   - Enter dashboard ID: `19004`
   - Select Prometheus datasource
   - Click Import

2. **JVM Micrometer** (ID: 4701)
   - JVM memory, GC, threads
   - Dashboard ID: `4701`

3. **Spring Boot Statistics** (ID: 6756)
   - HTTP requests, database connections
   - Dashboard ID: `6756`

4. **Jaeger Traces Overview** (ID: 10001)
   - Trace metrics and service graph
   - Dashboard ID: `10001`

### Create Custom Dashboard

```bash
# Navigate to Grafana
open http://localhost:3001

# Login: admin / admin
# Click "+" → "Create Dashboard"
# Add panel → Select Prometheus
# Query: rate(http_server_requests_seconds_count[5m])
```

## 🔍 Using Jaeger

### Search Traces

1. Open Jaeger UI: http://localhost:16686
2. Select Service: `beema-kernel`
3. Select Operation: (e.g., `GET /actuator/health`)
4. Set Lookback: Last 1 hour
5. Click "Find Traces"

### Trace Features

- **Trace Timeline:** See complete request flow
- **Span Details:** Click any span to see tags, logs, duration
- **Service Graph:** View service dependencies
- **Compare Traces:** Select multiple traces to compare

### Key Metrics to Watch

- **P95 Latency:** 95th percentile response time
- **Error Rate:** Percentage of failed requests
- **Throughput:** Requests per second
- **Trace Duration:** End-to-end request time

## 📊 Key Prometheus Queries

### HTTP Metrics

```promql
# Request rate (per second)
rate(http_server_requests_seconds_count[5m])

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))

# P95 latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

### JVM Metrics

```promql
# Heap memory usage
jvm_memory_used_bytes{area="heap"}

# GC time
rate(jvm_gc_pause_seconds_sum[5m])

# Thread count
jvm_threads_live_threads
```

### Database Metrics

```promql
# Active connections
hikaricp_connections_active{pool="BeemaKernelPool"}

# Connection pool usage
hikaricp_connections{pool="BeemaKernelPool"}

# Query duration
rate(spring_data_repository_invocations_seconds_sum[5m])
```

## 🔧 Configuration Files

### Prometheus Config
Location: `platform/observability/prometheus.yml`

```yaml
scrape_configs:
  - job_name: 'beema-kernel'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['beema-kernel:8080']
```

### OpenTelemetry Config (application.yml)
Location: `apps/beema-kernel/src/main/resources/application.yml`

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (reduce in production)
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces

otel:
  service:
    name: beema-kernel
  exporter:
    otlp:
      endpoint: http://localhost:4318
```

### Grafana Datasources
Location: `platform/observability/grafana/provisioning/datasources/datasources.yml`

Auto-configured:
- Prometheus (default datasource)
- Jaeger (with trace-to-metrics correlation)

## 🎯 Trace Annotations in Code

Add custom spans to your Java code:

```java
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.api.trace.Span;

@Service
public class MyService {

    @WithSpan("processAgreement")  // Creates a custom span
    public void processAgreement(Agreement agreement) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("agreement.id", agreement.getId().toString());
        currentSpan.setAttribute("agreement.type", agreement.getType());

        // Your business logic
    }
}
```

## 📋 Production Recommendations

### 1. Sampling Rate

Reduce sampling in production to control costs:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of traces
```

### 2. Resource Limits

Add resource limits to docker-compose.yml:

```yaml
jaeger:
  deploy:
    resources:
      limits:
        memory: 512M
        cpus: '1'

prometheus:
  deploy:
    resources:
      limits:
        memory: 1G
        cpus: '1'
```

### 3. Data Retention

Configure retention in Prometheus:

```yaml
command:
  - '--storage.tsdb.retention.time=30d'
  - '--storage.tsdb.retention.size=10GB'
```

### 4. Authentication

Enable Grafana auth for production:

```yaml
environment:
  - GF_AUTH_ANONYMOUS_ENABLED=false
  - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
```

## 🐛 Troubleshooting

### No Traces in Jaeger

1. Check beema-kernel logs for OTLP errors:
   ```bash
   docker-compose logs beema-kernel | grep -i otel
   ```

2. Verify Jaeger is receiving traces:
   ```bash
   curl http://localhost:16686/api/traces?service=beema-kernel&limit=10
   ```

3. Check OTLP endpoint is reachable:
   ```bash
   docker-compose exec beema-kernel curl -v http://jaeger:4318
   ```

### No Metrics in Prometheus

1. Check Prometheus targets:
   ```bash
   open http://localhost:9090/targets
   ```

2. Verify beema-kernel metrics endpoint:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. Check Prometheus logs:
   ```bash
   docker-compose logs prometheus | grep -i error
   ```

### Grafana Connection Issues

1. Verify datasources:
   ```bash
   docker-compose exec grafana curl http://prometheus:9090/api/v1/status/config
   docker-compose exec grafana curl http://jaeger:16686/api/services
   ```

2. Check Grafana logs:
   ```bash
   docker-compose logs grafana
   ```

## 📚 Resources

- [OpenTelemetry Java Instrumentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Prometheus Querying](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboards](https://grafana.com/grafana/dashboards/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 🎓 Learning Path

1. **Start Simple:**
   - Generate traces with API calls
   - View traces in Jaeger
   - Explore service dependency graph

2. **Add Metrics:**
   - Import Spring Boot dashboard in Grafana
   - Create alerts for high latency
   - Monitor JVM memory and GC

3. **Correlate:**
   - Click trace links in Grafana to jump to Jaeger
   - Use trace IDs to find related logs
   - Analyze slow traces with metrics

4. **Optimize:**
   - Identify slow database queries
   - Find N+1 query problems
   - Track down performance regressions

---

**Ready to explore!** Start with http://localhost:3001 (Grafana) and import dashboard ID 19004 for instant Spring Boot insights.
