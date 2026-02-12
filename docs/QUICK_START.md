# Beema Platform - Quick Start Guide

**Complete Docker-based setup with Temporal workflow orchestration**

## TL;DR - Get Running in 2 Minutes

```bash
# 1. Navigate to project
cd /Users/prabhatkumar/Desktop/dev-directory/beema

# 2. Start everything
docker-compose up -d

# 3. Wait for services to be healthy (~2 minutes)
docker-compose ps

# 4. Verify setup
./docker-compose-verify.sh
./scripts/verify-inngest-setup.sh

# 5. Access services
open http://localhost:3000      # Studio UI
open http://localhost:8080      # Beema Kernel API
open http://localhost:8088      # Temporal UI
open http://localhost:8288      # Inngest Dev UI
```

## Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Studio** (Frontend) | http://localhost:3000 | - |
| **Beema Kernel API** | http://localhost:8080 | - |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | - |
| **Temporal UI** | http://localhost:8088 | - |
| **Inngest Dev UI** | http://localhost:8288 | - |
| **Keycloak Admin** | http://localhost:8180 | admin / admin |
| **Metadata Service** | http://localhost:8082 | - |
| **PostgreSQL** | localhost:5433 | beema / beema |

## Essential Commands

### Start/Stop

```bash
# Start all services
docker-compose up -d

# Stop all services (keep data)
docker-compose stop

# Stop and remove (keep data)
docker-compose down

# Stop and delete everything (DELETES ALL DATA)
docker-compose down -v
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f beema-kernel
docker-compose logs -f temporal

# Last 100 lines
docker-compose logs --tail=100 beema-kernel
```

### Check Status

```bash
# List all services
docker-compose ps

# Verify setup
./docker-compose-verify.sh

# Check specific service health
docker inspect beema-kernel --format='{{.State.Health.Status}}'
```

### Rebuild After Code Changes

```bash
# Rebuild and restart beema-kernel
docker-compose build beema-kernel
docker-compose up -d beema-kernel

# Rebuild everything
docker-compose build
docker-compose up -d
```

## Test Policy Workflow

Create a policy (triggers Temporal workflow):

```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-2026-001",
    "productType": "MOTOR_COMPREHENSIVE",
    "insuredName": "John Doe",
    "coverageAmount": 500000.00,
    "effectiveDate": "2026-02-15T00:00:00Z",
    "expiryDate": "2027-02-15T00:00:00Z"
  }'
```

Then view the workflow execution:
- **Temporal UI**: http://localhost:8088
- Navigate to "Workflows" tab
- Find workflow with ID containing the policy number

## Test Event Flow

Run the comprehensive event flow test:

```bash
# Test Inngest event flow
./scripts/test-inngest-events.sh
```

Or manually publish test events:

```bash
# Publish Policy Bound event
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound

# Publish Claim Opened event
curl -X POST http://localhost:8080/api/v1/events/test/claim-opened
```

Monitor events:
- **Inngest UI**: http://localhost:8288 (see events in real-time)
- **Studio Webhooks**: http://localhost:3000/webhooks (view deliveries)

## Temporal CLI Commands

```bash
# Install Temporal CLI
brew install temporal  # macOS

# Check cluster health
tctl --address localhost:7233 cluster health

# List workflows
tctl --address localhost:7233 workflow list

# Describe specific workflow
tctl --address localhost:7233 workflow describe -w <workflow-id>
```

## Database Access

```bash
# Connect to PostgreSQL
docker exec -it beema-postgres psql -U beema -d beema_kernel

# Or from host (if psql installed)
psql -h localhost -p 5433 -U beema -d beema_kernel
```

## Kafka Commands

```bash
# List topics
docker exec beema-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Produce test message
echo '{"test": "message"}' | docker exec -i beema-kafka \
  kafka-console-producer --bootstrap-server localhost:9092 --topic raw-messages

# Consume messages
docker exec beema-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic processed-messages \
  --from-beginning
```

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker-compose logs <service-name>

# Check if ports are in use
lsof -i :8080  # Beema Kernel
lsof -i :7233  # Temporal
lsof -i :5433  # PostgreSQL

# Restart specific service
docker-compose restart <service-name>
```

### Temporal Worker Not Connecting

```bash
# Check environment variables
docker exec beema-kernel env | grep TEMPORAL

# Should show:
# TEMPORAL_HOST=temporal
# TEMPORAL_PORT=7233
# TEMPORAL_WORKER_ENABLED=true
# TEMPORAL_TASK_QUEUE=POLICY_TASK_QUEUE

# Restart kernel
docker-compose restart beema-kernel
```

### Database Connection Issues

```bash
# Check postgres health
docker-compose ps postgres

# Test connection
docker exec beema-postgres pg_isready -U beema

# Restart postgres
docker-compose restart postgres
```

### Reset Everything

```bash
# Nuclear option - deletes all data
docker-compose down -v
docker-compose up -d
```

## Service Dependencies

Services start in this order:

```
postgres (10s)
    ↓
temporal (30s) + keycloak (20s) + zookeeper (10s) + inngest (10s)
    ↓
temporal-ui (5s) + metadata-service (30s) + kafka (20s)
    ↓
beema-kernel (60s) + kafka-init (5s)
    ↓
studio (10s) + beema-message-processor (20s)

Total: ~2 minutes to full healthy state
```

## Development Mode

Run kernel locally with Docker services:

```bash
# Stop kernel container
docker-compose stop beema-kernel

# Keep other services running
docker-compose ps

# Set environment variables
export TEMPORAL_HOST=localhost
export TEMPORAL_PORT=7233
export DB_HOST=localhost
export DB_PORT=5433

# Run from IDE or command line
./gradlew bootRun
```

## Health Check Endpoints

```bash
# Beema Kernel
curl http://localhost:8080/actuator/health

# Metadata Service
curl http://localhost:8082/actuator/health

# Temporal (tctl)
tctl --address localhost:7233 cluster health

# PostgreSQL
docker exec beema-postgres pg_isready -U beema
```

## Resource Requirements

**Minimum**:
- RAM: 8GB (allocated to Docker)
- CPU: 4 cores
- Disk: 10GB free space

**Recommended**:
- RAM: 16GB
- CPU: 8 cores
- Disk: 20GB free space

## Port Summary

| Port  | Service              |
|-------|----------------------|
| 2181  | Zookeeper            |
| 3000  | Studio               |
| 5433  | PostgreSQL           |
| 7233  | Temporal gRPC        |
| 8080  | Beema Kernel         |
| 8081  | Message Processor    |
| 8082  | Metadata Service     |
| 8088  | Temporal UI          |
| 8180  | Keycloak             |
| 8288  | Inngest Dev Server   |
| 9092  | Kafka (external)     |
| 29092 | Kafka (internal)     |

## What's Included

- ✅ PostgreSQL 16 (shared database)
- ✅ Temporal Server 1.25.2 (workflow orchestration)
- ✅ Temporal UI 2.32.0 (workflow monitoring)
- ✅ Beema Kernel (Spring Boot + embedded worker)
- ✅ Metadata Service (metadata-driven config)
- ✅ Keycloak 23.0 (OAuth2/OIDC)
- ✅ Kafka 7.6.0 (message broker)
- ✅ Inngest Dev Server v0.38.0 (event infrastructure)
- ✅ Studio (React frontend)
- ✅ Message Processor (Flink)

## Documentation

- **Full Setup Guide**: [DOCKER_SETUP.md](DOCKER_SETUP.md)
- **Inngest Setup**: [INNGEST_SETUP.md](../INNGEST_SETUP.md)
- **Detailed README**: [docker-compose.README.md](docker-compose.README.md)
- **Integration Details**: [TEMPORAL_INTEGRATION_SUMMARY.md](TEMPORAL_INTEGRATION_SUMMARY.md)
- **Environment Config**: [.env.example](../.env.example)

## Need Help?

1. Run verification script: `./docker-compose-verify.sh`
2. Check service logs: `docker-compose logs <service-name>`
3. Review [DOCKER_SETUP.md](DOCKER_SETUP.md) troubleshooting section
4. Check Temporal UI for workflow errors: http://localhost:8088

---

**Last Updated**: 2026-02-12
**Platform Version**: 1.0.0
