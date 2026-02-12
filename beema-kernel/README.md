# Beema Unified Agreement Kernel

Bitemporal, metadata-driven insurance agreement system supporting Retail, Commercial, and London Market contexts.

## Architecture

### Bitemporal Pattern
- **Valid Time**: When data is/was/will be effective in the real world (`valid_from`, `valid_to`)
- **Transaction Time**: When data was recorded in the database (`transaction_time`)
- **Composite Primary Key**: `(id, valid_from, transaction_time)` enforces temporal uniqueness

### Key Features
✅ **Bitemporal Tracking** - Full audit history with time travel queries
✅ **JSONB Flex-Schema** - Market-specific attributes without schema migrations
✅ **Multi-Context Support** - Single codebase for Retail, Commercial, London Market
✅ **Multi-Tenancy** - Row-Level Security with PostgreSQL RLS
✅ **Metadata-Driven** - JSON Schema validation for flexible data models

## Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 3.2.1
- **Database**: PostgreSQL 15.4 with JSONB
- **Migrations**: Flyway
- **Build**: Maven
- **Cache**: Caffeine
- **API Docs**: SpringDoc OpenAPI
- **Auth**: OAuth2/JWT

## Prerequisites

1. **Java 21**
   ```bash
   java -version  # Should be 21+
   ```

2. **PostgreSQL 15+**
   ```bash
   # macOS
   brew install postgresql@15
   brew services start postgresql@15

   # Linux
   sudo apt install postgresql-15
   ```

3. **Maven 3.9+**
   ```bash
   mvn -version
   ```

## Quick Start

### 1. Create Database

```bash
# Create database and user
psql postgres
```

```sql
CREATE DATABASE beema_dev;
CREATE USER beema_admin WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE beema_dev TO beema_admin;
\c beema_dev
GRANT ALL ON SCHEMA public TO beema_admin;
```

### 2. Set Environment Variables

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=beema_dev
export DB_USERNAME=beema_admin
export DB_PASSWORD=changeme
export SPRING_PROFILE=dev
```

### 3. Build and Run

```bash
cd beema-kernel

# Install dependencies
./mvnw clean install

# Run Flyway migrations
./mvnw flyway:migrate

# Start application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

### 4. Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}

# OpenAPI docs
open http://localhost:8080/swagger-ui.html
```

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| V1 | Base schema (extensions, functions) |
| V2 | Metadata tables (agreement types, attributes) |
| V3 | Agreement core tables (agreements, parties, coverages) |
| V4 | Performance indexes (GIN, temporal, tenant) |
| V5 | Row-Level Security (multi-tenancy) |
| V6 | Seed metadata (sample schemas for all markets) |

### Run Migrations Manually

```bash
./mvnw flyway:migrate
```

### Verify Migrations

```bash
psql -U beema_admin -d beema_dev -c "SELECT * FROM schema_version ORDER BY applied_at;"
```

## Bitemporal Queries

### Query Current Version

```sql
-- Get current agreement
SELECT *
FROM agreements
WHERE id = 'some-uuid'
  AND is_current = TRUE;
```

### Point-in-Time Query

```sql
-- "What was valid on 2024-01-15?"
SELECT *
FROM agreements
WHERE id = 'some-uuid'
  AND valid_from <= '2024-01-15T10:00:00Z'
  AND valid_to > '2024-01-15T10:00:00Z'
  AND transaction_time <= '2024-01-15T10:00:00Z'
ORDER BY transaction_time DESC
LIMIT 1;
```

### Full History

```sql
-- Get all temporal versions
SELECT
    id,
    valid_from,
    valid_to,
    transaction_time,
    is_current,
    agreement_number,
    status
FROM agreements
WHERE id = 'some-uuid'
ORDER BY valid_from, transaction_time;
```

## JSONB Flex-Schema

### Store Market-Specific Attributes

```sql
-- Auto policy (RETAIL)
INSERT INTO agreements (id, valid_from, transaction_time, tenant_id, agreement_type_code, market_context, attributes)
VALUES (
    uuid_generate_v4(),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'tenant-123',
    'AUTO_POLICY',
    'RETAIL',
    '{
      "vehicle_vin": "1HGCM82633A123456",
      "vehicle_year": 2024,
      "vehicle_make": "Honda",
      "primary_driver_age": 35
    }'::jsonb
);
```

### Query by JSONB Attribute

```sql
-- Find all Honda vehicles
SELECT *
FROM agreements
WHERE attributes @> '{"vehicle_make": "Honda"}'
  AND is_current = TRUE;

-- Find by specific VIN
SELECT *
FROM agreements
WHERE attributes->>'vehicle_vin' = '1HGCM82633A123456'
  AND is_current = TRUE;
```

## Multi-Tenancy

The application uses PostgreSQL Row-Level Security (RLS) for tenant isolation.

### Set Tenant Context

```sql
-- Application sets this on each request
SELECT set_tenant_context('tenant-123');

-- Now queries only see tenant-123 data
SELECT * FROM agreements WHERE is_current = TRUE;
```

### Test Tenant Isolation

```sql
-- Test function
SELECT * FROM test_tenant_isolation('tenant-123');
```

### View RLS Status

```sql
SELECT * FROM v_rls_status;
SELECT * FROM v_rls_policies;
```

## Development

### Run Tests

```bash
./mvnw test
```

### Code Formatting

```bash
./mvnw spotless:apply
```

### Generate OpenAPI Spec

```bash
# Start application, then:
curl http://localhost:8080/api-docs > openapi.json
```

### Database Console

```bash
psql -U beema_admin -d beema_dev
```

Useful queries:

```sql
-- View metadata
SELECT type_code, market_context, schema_version, is_active
FROM metadata_agreement_types;

-- Count agreements by market
SELECT market_context, count(*)
FROM agreements
WHERE is_current = TRUE
GROUP BY market_context;

-- Check indexes
\d+ agreements
```

## Configuration

Key configuration in `application.yml`:

```yaml
beema:
  kernel:
    multi-tenancy:
      enabled: true
      tenant-header: X-Tenant-ID

    temporal:
      default-valid-from: 1970-01-01T00:00:00Z
      default-valid-to: 9999-12-31T23:59:59Z

    metadata:
      cache-ttl: 3600  # seconds
```

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Health check |
| `GET /actuator/metrics` | Prometheus metrics |
| `GET /swagger-ui.html` | OpenAPI documentation |
| `GET /api-docs` | OpenAPI JSON spec |

## Production Deployment

See `/platform/` directory for Kubernetes/Helm deployment.

### Docker Build

```bash
docker build -t beema-kernel:latest .
docker run -p 8080:8080 \
  -e DB_HOST=postgres.example.com \
  -e DB_PASSWORD=secret \
  beema-kernel:latest
```

### Helm Deployment

```bash
cd ../platform
helm install beema-kernel ./charts/beema-kernel \
  --set image.tag=latest \
  --set postgresql.enabled=false \
  --set externalDatabase.host=rds-postgres.us-east-1.rds.amazonaws.com
```

## Troubleshooting

### Flyway Migration Fails

```bash
# Check current state
./mvnw flyway:info

# Repair checksums
./mvnw flyway:repair

# Baseline (for existing DB)
./mvnw flyway:baseline
```

### Connection Pool Exhausted

Check `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase if needed
```

### RLS Blocking Queries

```sql
-- Check current tenant
SELECT get_current_tenant();

-- Bypass RLS (requires superuser)
SET SESSION AUTHORIZATION postgres;
```

## Next Steps

- [ ] Phase 2: Implement Metadata Service
- [ ] Phase 3: Implement Agreement CRUD
- [ ] Phase 4: Build REST API
- [ ] Phase 5: Add React UI
- [ ] Phase 6: Production monitoring

---

**Version**: 1.0.0
**Last Updated**: 2026-02-12
