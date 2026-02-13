# Beema Platform - Local Development Setup

This guide helps new developers set up their local development environment for the Beema insurance platform.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Shell Aliases Setup](#shell-aliases-setup)
- [Development Workflows](#development-workflows)
- [Service URLs](#service-urls)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before you begin, ensure you have:

1. **Docker Desktop** (includes Docker Compose V2)
   - [Download Docker Desktop](https://www.docker.com/products/docker-desktop/)
   - Version: Latest stable

2. **Node.js & npm/pnpm**
   - Node.js >= 18.0.0
   - pnpm >= 8.0.0 (recommended) or npm

3. **Java Development Kit (JDK)**
   - JDK 21 (for Spring Boot services)

4. **Maven** (for Java services)
   - Maven 3.9+

---

## Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/prabhatkmr/beema.git
cd beema
```

### 2. Install Dependencies
```bash
# Install npm dependencies
npm install

# Or use pnpm (recommended)
pnpm install
```

### 3. Set Up Development Aliases (Optional but Recommended)
```bash
# For zsh users (macOS default)
echo "source $(pwd)/infra/dev-aliases.sh" >> ~/.zshrc
source ~/.zshrc

# For bash users
echo "source $(pwd)/infra/dev-aliases.sh" >> ~/.bashrc
source ~/.bashrc
```

### 4. Build Docker Images (First Time Only)
```bash
docker compose build
```

---

## Shell Aliases Setup

The project includes helpful shell aliases in `infra/dev-aliases.sh` to streamline your development workflow.

### Installation

**Option A: Permanent Setup (Recommended)**

Add the following line to your shell configuration file:

```bash
# For zsh (macOS default)
echo "source ~/path/to/beema/infra/dev-aliases.sh" >> ~/.zshrc
source ~/.zshrc

# For bash
echo "source ~/path/to/beema/infra/dev-aliases.sh" >> ~/.bashrc
source ~/.bashrc
```

**Option B: One-Time Setup (Per Session)**

```bash
source ./infra/dev-aliases.sh
```

### Available Aliases

#### Infrastructure Management
| Alias | Description |
|-------|-------------|
| `beema-infra` | Start all infrastructure services (PostgreSQL, Kafka, Temporal, etc.) |
| `beema-infra-down` | Stop all Docker containers |
| `beema-infra-logs` | View logs from all infrastructure services (follow mode) |
| `beema-infra-ps` | Check status of all Docker containers |
| `beema-infra-restart` | Restart all infrastructure services |
| `beema-infra-clean` | Stop and remove all containers, networks, and volumes |

#### Full Stack Management
| Alias | Description |
|-------|-------------|
| `beema-up` | Start EVERYTHING (infrastructure + application services) |
| `beema-down` | Stop all services |
| `beema-rebuild` | Rebuild all Docker images |
| `beema-logs` | View logs from all services |

#### Development Workflow
| Alias | Description |
|-------|-------------|
| `beema-dev` | Start infrastructure and run `npm run dev` automatically |
| `beema-health` | Quick health check - shows status and service URLs |

#### Utilities
| Alias | Description |
|-------|-------------|
| `beema-code` | Open Beema project in VS Code |
| `beema-cd` | Navigate to Beema project directory |

---

## Development Workflows

### Workflow 1: Docker Only (Full Stack)

Run the entire platform (all services) in Docker containers.

```bash
# Start everything
docker compose up -d

# Or use alias
beema-up

# Check status
beema-health

# View logs
beema-logs

# Stop everything
beema-down
```

**Best for:** Testing the full system, production-like environment.

---

### Workflow 2: Hybrid Development (Recommended)

Run infrastructure in Docker, run application services locally with hot-reload.

```bash
# Step 1: Start infrastructure services
beema-infra

# Step 2: Wait for services to be healthy (~30-60 seconds)
beema-infra-ps

# Step 3: Run application services locally
npm run dev

# To stop:
# Ctrl+C (stop npm)
# beema-infra-down (stop Docker)
```

**Best for:** Active development with fast hot-reload.

---

### Workflow 3: Automated Hybrid (One Command)

```bash
# Start infrastructure + npm dev automatically
beema-dev
```

This alias:
1. Starts all infrastructure services in Docker
2. Waits 10 seconds for health checks
3. Runs `npm run dev` to start apps locally

---

## Service URLs

### Application Services (Local Dev)
- **Dashboard** (Main Landing Page): http://localhost:3000
- **Studio** (Blueprint Editor): http://localhost:3010
- **Portal** (Customer Portal): http://localhost:3011
- **Kernel API** (Core API): http://localhost:8080
- **Metadata Service API**: http://localhost:8082

### Infrastructure Services (Docker)
- **Keycloak** (Authentication): http://localhost:8180
  - Username: `admin`
  - Password: `admin`

- **Grafana** (Observability): http://localhost:3002
  - Username: `admin`
  - Password: `admin`

- **Jaeger UI** (Distributed Tracing): http://localhost:16686

- **Temporal UI** (Workflows): http://localhost:8088

- **Inngest** (Background Jobs): http://localhost:8288

- **MinIO Console** (S3 Storage): http://localhost:9001
  - Username: `admin`
  - Password: `password123`

- **Flink Dashboard** (Stream Processing): http://localhost:8081

- **Prometheus** (Metrics): http://localhost:9090

### Database
- **PostgreSQL**: localhost:5433
  - Database: `beema_kernel`, `beema_metadata`
  - Username: `beema`
  - Password: `beema`

### Message Streaming
- **Kafka**: localhost:9092

---

## Troubleshooting

### Port Conflicts

**Error:** `EADDRINUSE: address already in use :::3000`

**Solution:** Another process is using the port. Find and kill it:

```bash
# Find process on port 3000
lsof -ti:3000

# Kill process
lsof -ti:3000 | xargs kill -9

# Or use the Docker-only approach
beema-up
```

---

### Docker Credential Issues

**Error:** `exec: "docker-credential-desktop": executable file not found`

**Solution:** Remove the credential helper from Docker config:

```bash
# Edit ~/.docker/config.json and remove the "credsStore" line
```

Or the fix has already been applied in this project.

---

### Workspace Conflicts

**Error:** `must not have multiple workspaces with the same name`

**Solution:** The backup directory is excluded in `package.json`:

```json
"workspaces": [
  "apps/*",
  "!apps/studio-vite-backup",
  "packages/*"
]
```

If you see this error, ensure backup directories are excluded.

---

### Services Not Healthy

**Error:** Some Docker services show "unhealthy" status

**Solution:**

```bash
# Check logs for the specific service
docker compose logs [service-name]

# Example:
docker compose logs postgres

# Restart the service
docker compose restart [service-name]

# Or clean and restart everything
beema-infra-clean
beema-infra
```

---

### Maven Dependency Issues (Java Services)

**Error:** Maven download failures or missing dependencies

**Solution:**

```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild containers
beema-rebuild
```

---

### npm/pnpm Install Failures

**Solution:**

```bash
# Clear node_modules and lockfile
rm -rf node_modules pnpm-lock.yaml

# Reinstall
pnpm install

# Or use npm
npm install
```

---

## Development Tips

### 1. Check Service Health Regularly
```bash
beema-health
```

### 2. Monitor Logs
```bash
# All services
beema-logs

# Specific service
docker compose logs -f postgres
```

### 3. Clean Start (Reset Everything)
```bash
# Stop and remove all containers, volumes, and data
beema-infra-clean

# Rebuild and restart
beema-rebuild
beema-infra
```

### 4. Working on a Single Service
```bash
# Start only the infrastructure you need
docker compose up -d postgres kafka temporal

# Run your service locally
cd apps/studio
npm run dev
```

---

## Architecture Overview

The Beema platform uses:
- **Metadata-driven architecture** - Schema changes via database, not code deployments
- **Bitemporal data model** - Tracks valid_time (business) and transaction_time (audit)
- **Multi-tenant** - Row-Level Security with tenant isolation
- **Event-driven** - Temporal workflows for durable orchestration
- **Observable** - Distributed tracing and metrics for all services

See: `platform/observability/README.md` for full observability guide.

---

## Getting Help

- **Documentation**: Check the `docs/` folder
- **Issues**: https://github.com/prabhatkmr/beema/issues
- **Platform Guide**: `.claude/CLAUDE.md`
- **Observability**: `platform/observability/README.md`

---

## Contributing

When setting up a new development machine:
1. Follow this guide
2. Install the dev aliases
3. Run `beema-dev` to verify everything works
4. Happy coding! 🚀
