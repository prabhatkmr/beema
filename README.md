# Beema Insurance Platform - Turborepo Monorepo

A modern, high-performance insurance platform built with Spring Boot, Temporal.io, and Turborepo.

## 🏗️ Architecture

This is a **Turborepo monorepo** containing multiple microservices and shared packages:

```
beema/
├── apps/                      # Microservices
│   ├── beema-kernel/         # Core agreement kernel (Spring Boot + Temporal)
│   ├── metadata-service/     # Metadata & schema registry (Spring Boot)
│   └── auth-service/         # Authentication service (To be implemented)
├── packages/                  # Shared libraries (Future)
│   └── ...
├── platform/                  # Kubernetes/Helm charts
├── docker/                    # Docker configurations
├── turbo.json                # Turborepo pipeline config
└── package.json              # Root workspace config
```

## 🚀 Quick Start

### Prerequisites

- **Node.js** >= 18.0.0
- **pnpm** >= 8.0.0 (or npm/yarn)
- **Java** >= 21
- **Maven** >= 3.8.0
- **Docker** & **Docker Compose**

### Installation

```bash
# Install dependencies
pnpm install

# Install Turborepo globally (optional)
pnpm add -g turbo
```

## 📦 Available Commands

### Build All Services

```bash
pnpm build
# or
turbo build
```

Builds all services in parallel with intelligent caching.

### Run Tests

```bash
pnpm test
# or
turbo test
```

Runs tests for all services. Tests are cached based on code changes.

### Lint

```bash
pnpm lint
# or
turbo lint
```

Runs linting (checkstyle for Java services).

### Development Mode

```bash
pnpm dev
# or
turbo dev
```

Starts all services in development mode with hot-reload.

### Clean

```bash
pnpm clean
# or
turbo clean
```

Removes all build artifacts (`target/`, `dist/`, etc.).

### Format Code

```bash
pnpm format
# or
turbo format
```

Formats code using Spotless (Java) or Prettier (JS/TS).

## 🔧 Working with Individual Services

### Run a specific service

```bash
# Build only beema-kernel
turbo build --filter=@beema/kernel

# Test only metadata-service
turbo test --filter=@beema/metadata-service

# Dev mode for beema-kernel
turbo dev --filter=@beema/kernel
```

### Navigate to a service

```bash
cd apps/beema-kernel
mvn spring-boot:run
```

## 🐳 Docker Compose

Start all services with dependencies (Postgres, Temporal, etc.):

```bash
docker-compose up -d
```

Or from the beema-kernel directory:

```bash
cd apps/beema-kernel
docker-compose up -d
```

## ☸️ Kubernetes Deployment

Deploy to Kubernetes using Helm:

```bash
cd platform
helm install beema . -f values.yaml
```

## 📊 Turborepo Features

### Intelligent Caching

Turborepo caches build outputs based on:
- Source code changes
- Dependencies
- Environment variables
- Configuration files

**Result:** Only rebuild what changed. 10x faster builds!

### Parallel Execution

Services build in parallel respecting dependency graph:
```
metadata-service  ┐
                  ├─> beema-kernel (depends on metadata-service)
auth-service      ┘
```

### Remote Caching (Optional)

Enable remote caching for team collaboration:

```bash
# Login to Vercel (for remote cache)
turbo login

# Link to your team
turbo link
```

## 🏢 Microservices

### beema-kernel

**Core Agreement Kernel** - Bitemporal insurance agreement system with:
- Metadata-driven schema
- JSONB flex-schema
- Pre-compiled JEXL expressions
- Temporal workflow orchestration
- Multi-tenancy with Row-Level Security

**Tech:** Spring Boot 3, PostgreSQL, Temporal.io, Caffeine Cache

**Port:** 8080

### metadata-service

**Schema Registry** - Manages metadata definitions for:
- Field definitions
- Validation rules
- UI layouts
- Calculation rules

**Tech:** Spring Boot 3, PostgreSQL

**Port:** 8081

### auth-service

**Authentication Service** - OAuth2/JWT authentication (To be implemented)

**Port:** 8082

## 🛠️ Adding a New Service

1. Create service in `apps/`:
```bash
mkdir -p apps/new-service
cd apps/new-service
```

2. Create `package.json`:
```json
{
  "name": "@beema/new-service",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "build": "mvn clean package -DskipTests",
    "test": "mvn test",
    "dev": "mvn spring-boot:run"
  }
}
```

3. Turborepo will automatically discover it!

## 📚 Documentation

- [Metadata Cache Guide](apps/beema-kernel/METADATA_CACHE.md)
- [Temporal Workflow Guide](apps/beema-kernel/TEMPORAL_WORKFLOW_GUIDE.md)
- [Docker Setup](apps/beema-kernel/DOCKER_SETUP.md)

## 🧪 Testing

### Unit Tests

```bash
turbo test
```

### Integration Tests (Requires Docker)

```bash
# Start dependencies
docker-compose up -d postgres temporal

# Run integration tests
cd apps/beema-kernel
mvn test -Dtest=*Integration*
```

## 🔍 Monitoring Cache Performance

```bash
# View Turborepo cache stats
turbo run build --summarize

# Clear Turborepo cache
rm -rf .turbo
```

## 🤝 Contributing

1. Create a feature branch
2. Make changes
3. Run tests: `pnpm test`
4. Run lint: `pnpm lint`
5. Submit PR

## 📄 License

ISC

## 🔗 Links

- [Turborepo Docs](https://turbo.build/repo/docs)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Temporal.io](https://temporal.io)
