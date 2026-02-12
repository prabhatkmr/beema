# Beema Docker Compose Guide

This guide provides instructions for running the Beema Unified Platform using Docker Compose.

## Services Overview

The complete Beema platform includes the following services:

### Core Services
- **postgres**: PostgreSQL 16 database (shared by all services)
- **keycloak**: OAuth2/OIDC authentication provider
- **metadata-service**: Metadata-driven configuration service
- **beema-kernel**: Core Spring Boot application with Temporal worker

### Temporal Services
- **temporal**: Temporal server for workflow orchestration
- **temporal-ui**: Web UI for monitoring workflows

### Message Processing
- **zookeeper**: Coordination service for Kafka
- **kafka**: Message broker for event streaming
- **kafka-init**: Initialization container for topic creation
- **beema-message-processor**: Flink-based message processing job

### Frontend
- **studio**: React-based visual message blueprint editor

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- Minimum 8GB RAM allocated to Docker
- Ports available: 3000, 5433, 7233, 8080, 8081, 8082, 8088, 8180, 9092, 29092, 2181

## Quick Start

### Start All Services

```bash
docker-compose up -d
```

This will start all services in the correct order based on dependencies.

### Check Service Status

```bash
docker-compose ps
```

Expected output shows all services as "healthy" or "running":
```
NAME                        STATUS
beema-kafka                 Up (healthy)
beema-kafka-init            Exited (0)
beema-kernel                Up (healthy)
beema-keycloak              Up (healthy)
beema-message-processor     Up
beema-metadata              Up (healthy)
beema-postgres              Up (healthy)
beema-studio                Up (healthy)
beema-temporal              Up (healthy)
beema-temporal-ui           Up
beema-zookeeper             Up
```

## Selective Service Startup

### Core Platform Only (Kernel + Temporal)

```bash
docker-compose up -d postgres keycloak metadata-service temporal temporal-ui beema-kernel
```

This starts the minimum required services for the core Beema platform with workflow support.

### Add Studio Frontend

```bash
docker-compose up -d studio
```

### Add Message Processing

```bash
docker-compose up -d zookeeper kafka kafka-init beema-message-processor
```

### Add Individual Services

```bash
# Start only Temporal UI
docker-compose up -d temporal-ui

# Start only Kafka
docker-compose up -d zookeeper kafka kafka-init
```

## Accessing Services

Once all services are running, you can access:

- **Beema Kernel API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator Health**: http://localhost:8080/actuator/health
- **Temporal Web UI**: http://localhost:8088
- **Studio (Frontend)**: http://localhost:3000
- **Keycloak Admin**: http://localhost:8180 (admin/admin)
- **Metadata Service**: http://localhost:8082
- **PostgreSQL**: localhost:5433 (user: beema, password: beema, database: beema_kernel)
- **Kafka**: localhost:9092 (internal: kafka:29092)

## Viewing Logs

### Follow logs for all services

```bash
docker-compose logs -f
```

### Follow logs for specific service

```bash
docker-compose logs -f beema-kernel
docker-compose logs -f temporal
docker-compose logs -f beema-message-processor
```

### View last 100 lines

```bash
docker-compose logs --tail=100 beema-kernel
```

## Temporal Workflow Operations

### Install Temporal CLI (tctl)

**macOS**:
```bash
brew install temporal
```

**Linux**:
```bash
curl -sSf https://temporal.download/cli.sh | sh
```

**Windows**:
Download from https://github.com/temporalio/cli/releases

### Basic Temporal Commands

```bash
# Check cluster health
tctl --address localhost:7233 cluster health

# List all workflows
tctl --address localhost:7233 workflow list

# Describe a specific workflow
tctl --address localhost:7233 workflow describe -w <workflow-id>

# Show workflow execution history
tctl --address localhost:7233 workflow show -w <workflow-id> -r <run-id>

# Cancel a workflow
tctl --address localhost:7233 workflow cancel -w <workflow-id>

# Terminate a workflow
tctl --address localhost:7233 workflow terminate -w <workflow-id> --reason "manual termination"
```

### Temporal Workflow Testing with curl

**Start a Policy Workflow via Beema Kernel API**:
```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-2026-001",
    "productType": "MOTOR_COMPREHENSIVE",
    "insuredName": "John Doe",
    "coverageAmount": 500000.00
  }'
```

**Check workflow status in Temporal UI**: Visit http://localhost:8088

## Database Access

### Connect to PostgreSQL

```bash
# Using psql from host (if installed)
psql -h localhost -p 5433 -U beema -d beema_kernel

# Using Docker exec
docker exec -it beema-postgres psql -U beema -d beema_kernel
```

### List all databases

```sql
\l
```

### Check tables in beema_kernel

```sql
\c beema_kernel
\dt
```

## Kafka Operations

### List topics

```bash
docker exec -it beema-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Describe a topic

```bash
docker exec -it beema-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic raw-messages
```

### Produce test message

```bash
docker exec -it beema-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic raw-messages
# Type your message and press Enter
# Press Ctrl+C to exit
```

### Consume messages

```bash
docker exec -it beema-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic processed-messages --from-beginning
```

## Troubleshooting

### Service won't start

```bash
# Check service logs
docker-compose logs <service-name>

# Check if ports are already in use
lsof -i :8080  # Check if port 8080 is in use
lsof -i :7233  # Check if Temporal port is in use

# Restart specific service
docker-compose restart <service-name>
```

### Temporal server fails to start

```bash
# Check Temporal logs
docker-compose logs temporal

# Common issues:
# 1. PostgreSQL not ready - wait for postgres healthcheck
# 2. Port 7233 in use - check with: lsof -i :7233
# 3. Database connection issues - verify DB credentials
```

### Worker doesn't connect to Temporal

```bash
# Check beema-kernel logs
docker-compose logs beema-kernel | grep -i temporal

# Verify Temporal is accessible
docker exec -it beema-kernel curl -v http://temporal:7233

# Check environment variables
docker exec -it beema-kernel env | grep TEMPORAL
```

### Database connection issues

```bash
# Check if postgres is healthy
docker-compose ps postgres

# Test database connection
docker exec -it beema-postgres pg_isready -U beema

# Check if database exists
docker exec -it beema-postgres psql -U beema -c "\l"
```

### Kafka topic not created

```bash
# Check kafka-init logs
docker-compose logs kafka-init

# Manually create topic
docker exec -it beema-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic raw-messages --partitions 3 --replication-factor 1
```

### Clear all data and restart

```bash
# Stop all services
docker-compose down

# Remove all volumes (WARNING: This deletes all data)
docker-compose down -v

# Remove all images (optional)
docker-compose down --rmi all

# Start fresh
docker-compose up -d
```

### Check container health

```bash
# View detailed status
docker-compose ps

# Inspect specific container
docker inspect beema-kernel

# Check healthcheck logs
docker inspect --format='{{json .State.Health}}' beema-kernel | jq
```

## Stopping Services

### Stop all services (keep data)

```bash
docker-compose stop
```

### Stop and remove containers (keep data)

```bash
docker-compose down
```

### Stop and remove everything including volumes (DELETE ALL DATA)

```bash
docker-compose down -v
```

### Stop specific service

```bash
docker-compose stop beema-kernel
```

## Updating Services

### Rebuild specific service

```bash
docker-compose build beema-kernel
docker-compose up -d beema-kernel
```

### Rebuild all services

```bash
docker-compose build
docker-compose up -d
```

### Pull latest images

```bash
docker-compose pull
docker-compose up -d
```

## Performance Tuning

### Adjust resource limits

Edit `docker-compose.yml` and add resource constraints:

```yaml
beema-kernel:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

### Scale services

```bash
# Scale message processor to 3 instances
docker-compose up -d --scale beema-message-processor=3
```

## Development Workflow

### Local development with hot reload

```bash
# Stop beema-kernel container
docker-compose stop beema-kernel

# Run kernel locally with IDE (IntelliJ/VS Code)
# Make sure to set environment variables:
export TEMPORAL_HOST=localhost
export TEMPORAL_PORT=7233
export DB_HOST=localhost
export DB_PORT=5433

# Other services continue running in Docker
```

### Testing changes

```bash
# Make code changes
# Rebuild and restart service
docker-compose build beema-kernel
docker-compose up -d beema-kernel

# Watch logs
docker-compose logs -f beema-kernel
```

## Monitoring

### Container resource usage

```bash
docker stats
```

### Disk usage

```bash
docker system df
```

### Cleanup unused resources

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune -a
```

## Environment Configuration

Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
```

Then edit `.env` with your specific configuration.

## Support

For issues and questions:
- Check logs: `docker-compose logs <service>`
- Verify health: `docker-compose ps`
- Check Temporal UI: http://localhost:8088
- Review this README for common troubleshooting steps

## Service Dependencies

```
postgres (healthy)
  ├── keycloak (healthy)
  ├── metadata-service (healthy)
  │   └── beema-kernel (healthy)
  │       └── studio
  ├── temporal (healthy)
  │   ├── temporal-ui
  │   └── beema-kernel (worker)
  └── beema-message-processor

zookeeper
  └── kafka (healthy)
      └── kafka-init (completed)
          └── beema-message-processor
```

## Additional Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
