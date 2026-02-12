# Beema Kernel - Docker Setup with Temporal

This guide explains how to run Beema Kernel with Temporal workflows using Docker Compose.

## Architecture

The Docker Compose setup includes:

1. **PostgreSQL** - Database for Beema Kernel and Temporal
2. **Temporal Server** - Workflow orchestration engine
3. **Temporal Web UI** - Workflow monitoring and debugging
4. **Beema Kernel** - Spring Boot application with Temporal worker

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ RAM available for Docker

## Quick Start

### 1. Start All Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL on port 5432
- Temporal Server on port 7233
- Temporal Web UI on port 8088
- Beema Kernel on port 8080

### 2. Check Service Status

```bash
docker-compose ps
```

All services should be in "running" state.

### 3. View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f beema-kernel
docker-compose logs -f temporal
```

### 4. Access Services

- **Beema Kernel API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui
- **Temporal Web UI**: http://localhost:8088
- **Health Check**: http://localhost:8080/actuator/health

## Testing Workflows

### 1. Create a Test Agreement Workflow

Use the Beema Kernel API or curl:

```bash
curl -X POST http://localhost:8080/api/workflows/start \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "agreement.created",
    "agreementData": {
      "agreementId": 12345,
      "agreementType": "POLICY",
      "premiumAmount": 150000,
      "marketType": "LONDON_MARKET",
      "placementType": "DIRECT",
      "lineOfBusiness": "COMMERCIAL"
    }
  }'
```

### 2. View Workflow in Temporal UI

1. Open http://localhost:8088
2. Navigate to "Workflows" tab
3. Find your workflow by ID
4. View execution history, activities, and results

### 3. Check Policy Snapshots

```bash
curl http://localhost:8080/mock-policy-api/snapshots/stats
```

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v
```

## Troubleshooting

### Temporal Worker Not Connecting

Check Temporal server health:

```bash
docker-compose exec temporal tctl cluster health
```

### Database Connection Issues

Check PostgreSQL logs:

```bash
docker-compose logs postgres
```

Verify connection:

```bash
docker-compose exec postgres psql -U beema -d beema_kernel -c "SELECT 1;"
```

### Application Startup Issues

Check Beema Kernel logs:

```bash
docker-compose logs beema-kernel
```

Verify Flyway migrations:

```bash
docker-compose exec beema-kernel curl http://localhost:8080/actuator/flyway
```

### Temporal UI Not Loading

Check if Temporal server is healthy:

```bash
docker-compose exec temporal tctl --address temporal:7233 cluster health
```

## Development Mode

For development, you can run Beema Kernel locally and only use Docker for dependencies:

```bash
# Start only PostgreSQL and Temporal
docker-compose up -d postgres temporal temporal-ui

# Run Beema Kernel locally
./mvnw spring-boot:run -Dspring-boot.run.arguments="\
  --temporal.service.host=localhost \
  --temporal.service.port=7233"
```

## Environment Variables

Key environment variables for Beema Kernel:

| Variable | Default | Description |
|----------|---------|-------------|
| `TEMPORAL_HOST` | localhost | Temporal server host |
| `TEMPORAL_PORT` | 7233 | Temporal server port |
| `TEMPORAL_NAMESPACE` | default | Temporal namespace |
| `TEMPORAL_WORKER_ENABLED` | true | Enable/disable Temporal worker |
| `TEMPORAL_MAX_CONCURRENT_ACTIVITIES` | 10 | Max concurrent activities |
| `TEMPORAL_MAX_CONCURRENT_WORKFLOWS` | 10 | Max concurrent workflows |

## Production Considerations

For production deployment:

1. **Use external PostgreSQL** - Replace embedded PostgreSQL with managed database
2. **Scale Temporal** - Deploy Temporal server cluster
3. **Separate Workers** - Run workers in separate pods/containers
4. **Configure Resources** - Set CPU/memory limits
5. **Enable TLS** - Configure TLS for Temporal connections
6. **Configure Auth** - Enable authentication and authorization
7. **Monitoring** - Add Prometheus/Grafana for metrics

See Helm charts in `/platform/` directory for Kubernetes deployment.
