# Development Mode Guide

This guide explains how to run Beema services in development mode with hot-reload capabilities.

## Quick Start

### Option 1: Run Everything in Docker (Recommended for Beginners)

```bash
# Start all services in dev mode with hot-reload
make dev

# Or run in background
make dev-up

# Follow logs
make dev-logs

# Stop everything
make dev-down
```

### Option 2: Infrastructure in Docker + Services Running Locally (Recommended for Active Development)

This mode gives you faster iteration cycles and better debugging experience.

```bash
# 1. Start infrastructure services only (postgres, kafka, temporal, etc.)
make infra-up

# 2. Run services locally (in separate terminals)
make local-studio      # Terminal 1: Next.js with hot-reload
make local-kernel      # Terminal 2: Spring Boot with hot-reload
make local-metadata    # Terminal 3: Spring Boot with hot-reload
make local-processor   # Terminal 4: Flink local mode

# Stop infrastructure when done
make infra-down
```

## What's Enabled in Dev Mode

### All Services
- **Hot Reload**: Code changes automatically restart the service
- **Debug Ports**: Remote debugging enabled on specific ports
- **Detailed Logging**: DEBUG level logs for application code
- **Live Reload**: Browser auto-refresh (for Studio)

### Spring Boot Services (Kernel, Metadata Service)
- **Spring DevTools**: Automatic restart on classpath changes
- **LiveReload Server**: Port 35729 for browser extensions
- **SQL Logging**: See all SQL queries with parameters
- **Actuator Endpoints**: Full access to health, metrics, etc.
- **Debug Port**:
  - Metadata Service: `5005`
  - Kernel: `5006`

### Studio (Next.js)
- **Next.js Fast Refresh**: Instant updates on file save
- **Hot Module Replacement**: No full page reload
- **Debug Port**: `9229` for Node.js debugging

### Message Processor (Flink)
- **Local Execution Mode**: No separate Flink cluster needed
- **Faster Iteration**: Direct Java execution via maven
- **Debug Port**: `5007`

## Individual Service Commands

```bash
# Start specific services in dev mode (requires infra-up)
make dev-studio      # Just Studio
make dev-kernel      # Just Kernel
make dev-metadata    # Just Metadata Service
make dev-processor   # Just Message Processor
```

## Debugging

### Remote Debugging with IntelliJ IDEA / VS Code

#### Metadata Service
- Host: `localhost`
- Port: `5005`

#### Kernel
- Host: `localhost`
- Port: `5006`

#### Message Processor
- Host: `localhost`
- Port: `5007`

#### Studio (Node.js)
- Host: `localhost`
- Port: `9229`

### IntelliJ IDEA Configuration
1. Run → Edit Configurations → Add New Configuration → Remote JVM Debug
2. Set host to `localhost` and port to service debug port
3. Click Debug to attach

### VS Code Configuration
Add to `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Metadata Service",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    },
    {
      "type": "java",
      "name": "Debug Kernel",
      "request": "attach",
      "hostName": "localhost",
      "port": 5006
    },
    {
      "type": "node",
      "name": "Debug Studio",
      "request": "attach",
      "port": 9229,
      "restart": true
    }
  ]
}
```

## Log Viewing

```bash
# Follow logs for specific services
make logs-studio
make logs-kernel
make logs-metadata
make logs-processor
make logs-kafka

# Or use docker-compose directly
docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f studio
```

## Shell Access

```bash
make shell-studio      # Open shell in Studio container
make shell-kernel      # Open shell in Kernel container
make shell-metadata    # Open shell in Metadata Service container
make shell-postgres    # Open PostgreSQL shell
```

## Testing

```bash
make test              # Run all tests
make test-kernel       # Run Kernel tests only
make test-metadata     # Run Metadata Service tests only
make test-processor    # Run Message Processor tests only
make test-studio       # Run Studio tests only
```

## Performance Tips

### Faster Maven Builds
The dev mode uses Docker volume caching for Maven `.m2` directories, so dependencies are only downloaded once.

### Faster Next.js Builds
- Node modules are cached in anonymous Docker volumes
- Turbopack is used for faster compilation
- `WATCHPACK_POLLING` is enabled for reliable file watching in Docker

### When to Use Docker vs Local
- **Use Docker dev mode** when:
  - First time setup
  - Want consistent environment
  - Debugging container-specific issues

- **Use local mode** when:
  - Active development (fastest iteration)
  - Need IDE integration (breakpoints, step debugging)
  - Working on a single service

## Troubleshooting

### Port Already in Use
```bash
# Find and kill process using a port
lsof -ti:8080 | xargs kill -9  # Replace 8080 with your port
```

### Hot Reload Not Working
```bash
# Rebuild without cache
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build --force-recreate
```

### Database Schema Issues
```bash
# Reset database (WARNING: Destroys all data)
docker-compose down -v
docker-compose up postgres -d
```

### Clean Everything and Start Fresh
```bash
make clean    # Remove containers, volumes, and build artifacts
make dev-up   # Start fresh
```

## Service URLs (Dev Mode)

| Service | URL | Purpose |
|---------|-----|---------|
| Studio | http://localhost:3000 | Next.js app |
| Kernel | http://localhost:8080 | Spring Boot API |
| Metadata Service | http://localhost:8082 | Spring Boot API |
| PostgreSQL | localhost:5433 | Database |
| Keycloak | http://localhost:8180 | OAuth2/OIDC |
| Temporal UI | http://localhost:8088 | Workflow management |
| Flink Dashboard | http://localhost:8081 | Stream processing |
| Kafka | localhost:9092 | Message broker |
| Inngest | http://localhost:8288 | Event orchestration |
| Jaeger | http://localhost:16686 | Distributed tracing |
| Prometheus | http://localhost:9090 | Metrics |
| Grafana | http://localhost:3001 | Dashboards |

## Environment Variables

Override any environment variable:
```bash
# In docker-compose.dev.yml or export before running
export LOGGING_LEVEL_COM_BEEMA=TRACE
make dev
```

## Next Steps

- See `Makefile` for all available commands: `make help`
- Check individual service documentation in `apps/*/README.md`
- Configure your IDE for optimal development experience
