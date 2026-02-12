# Beema Platform Docker Setup

This document provides a comprehensive guide for setting up the Beema Unified Platform using Docker Compose with Temporal workflow orchestration.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Beema Unified Platform                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐      ┌──────────────┐      ┌──────────────┐    │
│  │ Studio   │─────▶│ Beema Kernel │─────▶│ PostgreSQL   │    │
│  │ (React)  │      │ (Spring Boot)│      │ (Database)   │    │
│  │ :3000    │      │ :8080        │      │ :5433        │    │
│  └──────────┘      └──────────────┘      └──────────────┘    │
│                            │                      ▲            │
│                            │                      │            │
│                            ▼                      │            │
│                    ┌──────────────┐               │            │
│                    │   Temporal   │───────────────┘            │
│                    │   Server     │                            │
│                    │   :7233      │                            │
│                    └──────────────┘                            │
│                            │                                   │
│                            ▼                                   │
│                    ┌──────────────┐                            │
│                    │ Temporal UI  │                            │
│                    │  (Web UI)    │                            │
│                    │  :8088       │                            │
│                    └──────────────┘                            │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  Keycloak    │  │  Metadata    │  │   Kafka      │        │
│  │  (OAuth2)    │  │  Service     │  │  Broker      │        │
│  │  :8180       │  │  :8082       │  │  :9092       │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
│                                               │                │
│                                               ▼                │
│                                       ┌──────────────┐         │
│                                       │   Message    │         │
│                                       │  Processor   │         │
│                                       │  (Flink)     │         │
│                                       │  :8081       │         │
│                                       └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

## Service Dependency Chain

The services start in this order based on dependencies:

```
1. postgres (PostgreSQL database - foundation for all services)
   ↓
2. ├─ keycloak (OAuth2 provider)
   ├─ temporal (Workflow orchestration server)
   └─ zookeeper (Kafka coordination)
      ↓
3. ├─ metadata-service (depends on postgres + keycloak)
   ├─ temporal-ui (depends on temporal)
   └─ kafka (depends on zookeeper)
      ↓
4. ├─ beema-kernel (depends on postgres + keycloak + metadata-service + temporal)
   └─ kafka-init (topic creation, depends on kafka)
      ↓
5. ├─ studio (depends on beema-kernel)
   └─ beema-message-processor (depends on kafka + postgres)
```

## Prerequisites

### System Requirements
- **Docker Desktop** 4.0+ (or Docker Engine 20+ with Docker Compose 2+)
- **RAM**: Minimum 8GB allocated to Docker (16GB recommended)
- **CPU**: Minimum 4 cores
- **Disk**: 10GB free space

### Port Requirements
Ensure these ports are available on your host machine:

| Port  | Service              | Purpose                        |
|-------|----------------------|--------------------------------|
| 3000  | Studio               | React frontend                 |
| 5433  | PostgreSQL           | Database (external access)     |
| 7233  | Temporal             | Temporal gRPC endpoint         |
| 8080  | Beema Kernel         | REST API                       |
| 8081  | Message Processor    | Flink Web UI                   |
| 8082  | Metadata Service     | Metadata API                   |
| 8088  | Temporal UI          | Temporal Web Interface         |
| 8180  | Keycloak             | OAuth2 Admin Console           |
| 9092  | Kafka                | Kafka external listener        |
| 29092 | Kafka                | Kafka internal listener        |
| 2181  | Zookeeper            | Kafka coordination             |
| 8288  | Inngest Dev Server   | Event infrastructure UI        |

## Quick Start

### 1. Clone and Navigate to Project

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema
```

### 2. Review Environment Configuration

```bash
# Copy example environment file
cp .env.example .env

# Edit if needed (optional for local development)
nano .env
```

### 3. Start All Services

```bash
# Start all services in detached mode
docker-compose up -d

# Or start with logs visible
docker-compose up
```

### 4. Monitor Startup

```bash
# Watch all service logs
docker-compose logs -f

# Watch specific service
docker-compose logs -f beema-kernel
docker-compose logs -f temporal
```

### 5. Verify Services

```bash
# Run verification script
./docker-compose-verify.sh

# Or manually check
docker-compose ps
```

Expected output:
```
NAME                        STATUS
beema-kafka                 Up (healthy)
beema-kernel                Up (healthy)
beema-keycloak              Up (healthy)
beema-metadata              Up (healthy)
beema-postgres              Up (healthy)
beema-studio                Up (healthy)
beema-temporal              Up (healthy)
beema-temporal-ui           Up
beema-zookeeper             Up
```

### 6. Access Services

Once all services are healthy (1-2 minutes):

- **Studio UI**: http://localhost:3000
- **Beema Kernel API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Temporal UI**: http://localhost:8088
- **Keycloak Admin**: http://localhost:8180 (admin/admin)
- **Inngest Dev UI**: http://localhost:8288

## Configuration Details

### PostgreSQL Configuration

The database is shared by all services:

- **Container**: `beema-postgres`
- **Image**: `postgres:16-alpine`
- **Port**: `5433:5432` (host:container)
- **User**: `beema`
- **Password**: `beema`
- **Databases**:
  - `beema_kernel` (Beema Kernel data)
  - `keycloak` (Keycloak data)
  - `beema_metadata` (Metadata Service)
  - Temporal databases (auto-created)

### Temporal Configuration

**Temporal Server**:
- **Container**: `beema-temporal`
- **Image**: `temporalio/auto-setup:1.25.2`
- **Port**: `7233` (gRPC)
- **Database**: PostgreSQL (auto-setup)
- **Namespace**: `default`
- **Task Queue**: `POLICY_TASK_QUEUE`

**Temporal UI**:
- **Container**: `beema-temporal-ui`
- **Image**: `temporalio/ui:2.32.0`
- **Port**: `8088:8080`
- **Access**: http://localhost:8088

**Temporal Worker** (embedded in Beema Kernel):
- **Enabled**: `TEMPORAL_WORKER_ENABLED=true`
- **Task Queue**: `POLICY_TASK_QUEUE`
- **Max Concurrent Activities**: 10
- **Max Concurrent Workflows**: 10

### Beema Kernel Configuration

**Service**: `beema-kernel`
- **Build Context**: `./beema-kernel`
- **Port**: `8080:8080`
- **Environment**:
  - Database connection to PostgreSQL
  - Temporal worker enabled
  - OAuth2 via Keycloak
  - Metadata Service integration

**Key Environment Variables**:
```yaml
TEMPORAL_HOST: temporal
TEMPORAL_PORT: 7233
TEMPORAL_NAMESPACE: default
TEMPORAL_WORKER_ENABLED: true
TEMPORAL_TASK_QUEUE: POLICY_TASK_QUEUE
```

### Kafka Configuration

**Zookeeper**:
- **Container**: `beema-zookeeper`
- **Image**: `confluentinc/cp-zookeeper:7.6.0`
- **Port**: `2181`

**Kafka Broker**:
- **Container**: `beema-kafka`
- **Image**: `confluentinc/cp-kafka:7.6.0`
- **Ports**:
  - `9092` (external - from host)
  - `29092` (internal - from containers)
- **Topics** (auto-created):
  - `raw-messages` (3 partitions)
  - `processed-messages` (3 partitions)

### Inngest Dev Server

**Service**: `inngest`
- **Container**: `beema-inngest`
- **Image**: `inngest/inngest:v0.38.0`
- **Port**: `8288:8288`
- **Purpose**: Event infrastructure for webhook dispatching and event visualization
- **Environment**:
  - `INNGEST_EVENT_KEY`: local
  - `INNGEST_SIGNING_KEY`: test-signing-key (development only)
  - `INNGEST_LOG_LEVEL`: info

**Key Features**:
- Event visualization in Dev UI
- Webhook dispatcher function execution
- Event debugging and replay
- Function run monitoring

## Inngest Dev Server

### Starting Inngest

```bash
# Start just Inngest
docker-compose up -d inngest

# Or use the startup script
./scripts/start-inngest.sh
```

### Access Points

- **Inngest Dev UI**: http://localhost:8288
- **Event API**: http://localhost:8288/e/local
- **Health Check**: http://localhost:8288/health

### Viewing Events

1. Open http://localhost:8288
2. Navigate to "Events" tab
3. See all published events from beema-kernel

### Viewing Function Runs

1. Open http://localhost:8288
2. Navigate to "Functions" tab
3. See webhook-dispatcher executions
4. View logs and execution details

### Testing Event Flow

```bash
# Run the test script
./scripts/test-inngest-events.sh

# Or manually publish an event
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound
```

### Troubleshooting

**Inngest not starting:**
```bash
# Check logs
docker-compose logs inngest

# Restart
docker-compose restart inngest
```

**Events not appearing:**
- Check beema-kernel is connected: `docker-compose logs beema-kernel | grep inngest`
- Verify INNGEST_BASE_URL is set correctly
- Check Inngest health: `curl http://localhost:8288/health`

**Functions not triggering:**
- Ensure Studio is running: `docker-compose ps studio`
- Check function registration: Visit http://localhost:8288/functions
- Verify Inngest serve route: `curl http://localhost:3000/api/inngest`

## Testing the Setup

### 1. Test Database Connectivity

```bash
# Connect to PostgreSQL
docker exec -it beema-postgres psql -U beema -d beema_kernel

# List databases
\l

# Check tables
\dt

# Exit
\q
```

### 2. Test Temporal

**Via Temporal UI** (http://localhost:8088):
- Navigate to "Workflows"
- Should see empty list initially
- Check "Task Queues" for `POLICY_TASK_QUEUE`

**Via CLI**:
```bash
# Install Temporal CLI
brew install temporal  # macOS
# or download from https://github.com/temporalio/cli/releases

# Check cluster health
tctl --address localhost:7233 cluster health

# List namespaces
tctl --address localhost:7233 namespace list
```

### 3. Test Beema Kernel API

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}

# Check Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 4. Test Policy Workflow

```bash
# Create a policy (triggers PolicyWorkflow)
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-2026-001",
    "productType": "MOTOR_COMPREHENSIVE",
    "insuredName": "John Doe",
    "coverageAmount": 500000.00,
    "effectiveDate": "2026-02-15",
    "expiryDate": "2027-02-15"
  }'

# Check workflow execution in Temporal UI
open http://localhost:8088
```

### 5. Test Kafka

```bash
# List topics
docker exec beema-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Produce a test message
echo '{"test": "message"}' | docker exec -i beema-kafka \
  kafka-console-producer --bootstrap-server localhost:9092 --topic raw-messages

# Consume messages
docker exec beema-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic processed-messages \
  --from-beginning \
  --max-messages 10
```

## Common Operations

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f beema-kernel
docker-compose logs -f temporal

# Last 100 lines
docker-compose logs --tail=100 beema-kernel

# Search logs
docker-compose logs beema-kernel | grep -i "temporal"
docker-compose logs beema-kernel | grep -i "workflow"
```

### Restart Services

```bash
# Restart specific service
docker-compose restart beema-kernel

# Restart all services
docker-compose restart

# Rebuild and restart
docker-compose build beema-kernel
docker-compose up -d beema-kernel
```

### Stop Services

```bash
# Stop all (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove containers + volumes (DELETES ALL DATA)
docker-compose down -v
```

### Scale Services

```bash
# Scale message processor
docker-compose up -d --scale beema-message-processor=3

# Note: Only stateless services can be scaled
```

## Troubleshooting

### Temporal Server Won't Start

**Symptoms**:
- `beema-temporal` container exits immediately
- Logs show database connection errors

**Solution**:
```bash
# Check postgres is healthy
docker-compose ps postgres

# Check temporal logs
docker-compose logs temporal

# Restart temporal after postgres is ready
docker-compose restart temporal
```

### Worker Not Connecting to Temporal

**Symptoms**:
- Workflows don't execute
- Logs show "no worker available"

**Solution**:
```bash
# Check beema-kernel logs
docker-compose logs beema-kernel | grep -i temporal

# Verify environment variables
docker exec beema-kernel env | grep TEMPORAL

# Expected:
# TEMPORAL_HOST=temporal
# TEMPORAL_PORT=7233
# TEMPORAL_WORKER_ENABLED=true
# TEMPORAL_TASK_QUEUE=POLICY_TASK_QUEUE

# Restart beema-kernel
docker-compose restart beema-kernel
```

### Port Already in Use

**Symptoms**:
- Error: "port is already allocated"

**Solution**:
```bash
# Check what's using the port (example for port 8080)
lsof -i :8080

# Kill the process or change the port in docker-compose.yml
# For example, change beema-kernel port to 8081:
ports:
  - "8081:8080"
```

### Services Taking Too Long to Start

**Symptoms**:
- Services stuck in "starting" state
- Health checks failing

**Solution**:
```bash
# Increase Docker resources (Docker Desktop → Preferences → Resources)
# - CPU: 4+ cores
# - Memory: 8GB+

# Check health check logs
docker inspect beema-kernel --format='{{json .State.Health}}' | jq

# Increase healthcheck timeout in docker-compose.yml:
healthcheck:
  start_period: 120s  # Increase from 60s
```

### Database Connection Errors

**Symptoms**:
- Services can't connect to postgres
- "connection refused" errors

**Solution**:
```bash
# Check postgres is running and healthy
docker-compose ps postgres

# Test database connectivity
docker exec beema-postgres pg_isready -U beema

# Check if database exists
docker exec beema-postgres psql -U beema -lqt

# Recreate database (WARNING: deletes data)
docker-compose down postgres
docker volume rm beema_postgres_data
docker-compose up -d postgres
```

## Development Workflow

### Running Beema Kernel Locally with Docker Services

For development with hot reload:

```bash
# Stop beema-kernel container
docker-compose stop beema-kernel

# Keep other services running
docker-compose ps

# Run kernel from IDE (IntelliJ/VS Code) with these environment variables:
export TEMPORAL_HOST=localhost
export TEMPORAL_PORT=7233
export DB_HOST=localhost
export DB_PORT=5433
export METADATA_SERVICE_URL=http://localhost:8082
export OAUTH2_ISSUER_URI=http://localhost:8180/realms/beema

# Or use application-dev.yml with profiles
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Debugging Temporal Workflows

```bash
# Enable debug logging in beema-kernel
docker-compose stop beema-kernel
# Edit application.yml or set environment variable:
# LOGGING_LEVEL_IO_TEMPORAL=DEBUG

docker-compose up -d beema-kernel

# Watch logs
docker-compose logs -f beema-kernel | grep -i workflow
```

### Resetting Everything

```bash
# Stop all services
docker-compose down

# Remove all volumes (DELETES ALL DATA)
docker-compose down -v

# Remove all images (optional)
docker-compose down --rmi all

# Clean Docker system
docker system prune -a

# Start fresh
docker-compose up -d
```

## Production Considerations

### Security

1. **Change default passwords**:
   - PostgreSQL: `POSTGRES_PASSWORD`
   - Keycloak admin: `KEYCLOAK_ADMIN_PASSWORD`

2. **Use secrets management**:
   - Don't commit `.env` file
   - Use Docker secrets or external secret managers

3. **Enable TLS**:
   - Configure Temporal with TLS
   - Use HTTPS for all HTTP services
   - Configure Keycloak with proper certificates

### High Availability

1. **Scale services**:
   ```yaml
   deploy:
     replicas: 3
   ```

2. **External databases**:
   - Use managed PostgreSQL (AWS RDS, Azure Database)
   - Configure Temporal with production database

3. **Load balancing**:
   - Add nginx/traefik for load balancing
   - Configure health checks

### Monitoring

1. **Add monitoring stack**:
   - Prometheus for metrics
   - Grafana for dashboards
   - Jaeger for distributed tracing

2. **Temporal metrics**:
   - Enable Prometheus endpoint in Temporal
   - Monitor workflow execution metrics

3. **Application metrics**:
   - Enable Spring Boot Actuator metrics
   - Export to Prometheus

## Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [Spring Boot Docker Deployment](https://spring.io/guides/topicals/spring-boot-docker/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Support

For issues:
1. Run verification script: `./docker-compose-verify.sh`
2. Check logs: `docker-compose logs <service-name>`
3. Review this document's troubleshooting section
4. Check Temporal UI for workflow errors: http://localhost:8088

---

**Last Updated**: 2026-02-12
**Version**: 1.0.0
**Beema Platform**: Unified Insurance Platform with Temporal Workflow Orchestration
