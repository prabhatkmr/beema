# Analytics Layer Deployment Guide

## Prerequisites

- Java 17+
- Docker and Docker Compose
- PostgreSQL 15+ (or use Docker Compose)
- Maven 3.9+ (or use the included `mvnw` wrapper)

## Local Development Setup

### 1. Start Infrastructure

```bash
# Start PostgreSQL and MinIO
docker-compose up -d postgres minio

# Wait for services to be healthy
docker-compose ps
```

### 2. Initialize MinIO Buckets

```bash
# Create default buckets (beema-exports, beema-documents, beema-attachments)
./scripts/init-minio.sh
```

### 3. Run the Application

```bash
cd beema-kernel

# Run with local profile (uses MinIO at localhost:9000)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

### 4. Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Trigger a test export
curl -X POST http://localhost:8080/api/v1/batch/export/parquet \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{"tenantId": "test-tenant"}'
```

### 5. Access MinIO Console

Open http://localhost:9001 and log in with `admin` / `password123` to browse exported files.

## Storage Configuration

### Option A: Local Filesystem (Default)

No configuration needed. Files are written to `data/exports/` relative to the working directory.

```yaml
beema:
  storage:
    type: filesystem
    filesystem:
      base-path: data/exports
```

### Option B: MinIO (Local S3 Simulation)

Use the `local` Spring profile or set environment variables:

```yaml
beema:
  storage:
    type: s3
    s3:
      endpoint: http://localhost:9000
      region: us-east-1
      bucket: beema-exports
      access-key: admin
      secret-key: password123
      path-style-access: true
```

### Option C: AWS S3

```yaml
beema:
  storage:
    type: s3
    s3:
      bucket: my-company-beema-exports
      region: eu-west-1
```

AWS credentials are resolved via the default credential chain:

1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. AWS credentials file (`~/.aws/credentials`)
3. IAM instance profile (EC2/ECS)
4. EKS IRSA (IAM Roles for Service Accounts)

Required IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-company-beema-exports",
        "arn:aws:s3:::my-company-beema-exports/*"
      ]
    }
  ]
}
```

### Option D: Azure Blob Storage

```yaml
beema:
  storage:
    type: azure
    azure:
      connection-string: DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net
      container-name: beema-exports
```

For Managed Identity authentication, use the `DefaultAzureCredential` chain instead of a connection string.

## Environment Variables Reference

### Application

| Variable                          | Default         | Description                                        |
|-----------------------------------|-----------------|----------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`          | `dev`           | Active Spring profile (`dev`, `local`, `prod`)     |
| `DB_HOST`                         | `localhost`     | PostgreSQL host                                    |
| `DB_PORT`                         | `5432`          | PostgreSQL port                                    |
| `DB_NAME`                         | `beema_dev`     | Database name                                      |
| `DB_USERNAME`                     | `beema_admin`   | Database username                                  |
| `DB_PASSWORD`                     | `changeme`      | Database password                                  |
| `DB_POOL_SIZE`                    | `20`            | HikariCP connection pool size                      |

### Storage

| Variable                          | Default         | Description                                        |
|-----------------------------------|-----------------|----------------------------------------------------|
| `STORAGE_TYPE`                    | `filesystem`    | Storage backend: `filesystem`, `s3`, `azure`       |
| `S3_BUCKET`                       | `beema-exports` | S3 bucket name                                     |
| `S3_REGION`                       | `us-east-1`     | AWS region                                         |
| `S3_ENDPOINT`                     | *(empty)*       | Custom S3 endpoint (for MinIO/LocalStack)          |
| `AWS_ACCESS_KEY_ID`               | *(from chain)*  | AWS access key                                     |
| `AWS_SECRET_ACCESS_KEY`           | *(from chain)*  | AWS secret key                                     |
| `AZURE_STORAGE_CONNECTION_STRING` | *(empty)*       | Azure Storage connection string                    |
| `AZURE_CONTAINER_NAME`            | `beema-exports` | Azure Blob container name                          |
| `STORAGE_BASE_PATH`              | `data/exports`  | Filesystem storage base directory                  |

### Observability

| Variable                          | Default              | Description                                   |
|-----------------------------------|----------------------|-----------------------------------------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT`    | `http://jaeger:4318` | OpenTelemetry OTLP endpoint                   |
| `OTEL_SERVICE_NAME`              | `beema-kernel`       | Service name for traces                       |
| `LOG_LEVEL`                       | `INFO`               | Application log level                         |

## Docker Compose Deployment

### Full Stack

```bash
# Start everything (PostgreSQL, MinIO, Kafka, Temporal, Observability, App)
docker-compose up -d

# Initialize MinIO buckets
./scripts/init-minio.sh
```

### Minimal (Analytics Only)

```bash
# Start only what the analytics layer needs
docker-compose up -d postgres minio

# Run the application locally
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run -pl beema-kernel
```

### Development Mode

```bash
# Start with hot-reload and debug ports
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

## Kubernetes / Helm

Refer to the Helm charts in `platform/helm/` for production Kubernetes deployments. Key configuration:

```yaml
# values.yaml
beemaKernel:
  storage:
    type: s3
    s3:
      bucket: beema-exports-prod
      region: eu-west-1
  batch:
    chunkSize: 2000
    fetchSize: 2000
```

## Troubleshooting

### MinIO Connection Refused

```
software.amazon.awssdk.core.exception.SdkClientException: Unable to execute HTTP request: Connect to localhost:9000 failed
```

**Cause:** MinIO container is not running or not yet healthy.

**Fix:**
```bash
docker-compose up -d minio
docker-compose ps minio  # Check status is "healthy"
```

### S3 "Access Denied"

```
software.amazon.awssdk.services.s3.model.S3Exception: Access Denied
```

**Cause:** Incorrect credentials or bucket does not exist.

**Fix:**
```bash
# Verify credentials match docker-compose.yml
# Create buckets if missing
./scripts/init-minio.sh
```

### Parquet File Is Empty

**Cause:** No records match `WHERE is_current = true AND tenant_id = ?`.

**Fix:** Verify data exists for the tenant:
```sql
SELECT count(*) FROM agreements WHERE is_current = true AND tenant_id = 'your-tenant';
```

### Spring Batch Job Stuck

**Cause:** A previous job instance with the same parameters may be marked as `STARTED` in the `BATCH_JOB_EXECUTION` table.

**Fix:**
```sql
-- Check for stuck jobs
SELECT * FROM batch_job_execution WHERE status = 'STARTED';

-- Mark abandoned (if the JVM crashed)
UPDATE batch_job_execution SET status = 'ABANDONED', exit_code = 'ABANDONED'
WHERE status = 'STARTED' AND end_time IS NULL;
```

### Out of Memory During Export

**Cause:** Too many records in a single chunk or very large JSONB attributes.

**Fix:** Reduce chunk size:
```yaml
# In ParquetExportJobConfig, the chunk size is set to 1000 by default.
# For very large records, consider reducing to 500 or less.
```

## Performance Tuning

| Parameter            | Default | Tuning Guidance                                              |
|----------------------|---------|--------------------------------------------------------------|
| Chunk size           | 1,000   | Increase for small records, decrease for large JSONB payloads |
| JDBC fetch size      | 1,000   | Match to chunk size for optimal batching                     |
| HikariCP pool size   | 20      | 1-2 connections per concurrent export job is sufficient       |
| Parquet page size    | 1 MB    | Larger pages = better compression, more memory               |
| Row group size       | 128 MB  | Standard for analytics; 64 MB for smaller clusters           |
| Snappy compression   | On      | Use ZSTD for better ratio (slower writes, same read speed)   |

### Monitoring Export Performance

Spring Batch metrics are exposed via Actuator at `/actuator/prometheus`:

```
spring_batch_job_seconds_count{name="universalParquetExport"}
spring_batch_job_seconds_sum{name="universalParquetExport"}
spring_batch_chunk_read_count{name="exportAgreementsStep"}
spring_batch_chunk_write_count{name="exportAgreementsStep"}
```

Grafana dashboards can be configured to visualize these metrics alongside Jaeger traces for end-to-end observability.
