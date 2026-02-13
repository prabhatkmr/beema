# Quick Start: Analytics Export Pipeline

Get the Beema analytics export pipeline running locally in 5 minutes.

## 1. Start Infrastructure

```bash
# From the project root
docker-compose up -d postgres minio
```

Wait for both services to report healthy:

```bash
docker-compose ps
```

Expected output:

```
NAME             STATUS
beema-postgres   Up (healthy)
beema-minio      Up (healthy)
```

## 2. Create Storage Buckets

```bash
./scripts/init-minio.sh
```

Expected output:

```
Configuring MinIO client alias...
Created bucket: beema-exports
Created bucket: beema-documents
Created bucket: beema-attachments

MinIO initialized successfully!
  API endpoint:  http://localhost:9000
  Console:       http://localhost:9001
  Credentials:   admin / password123
  Buckets:       beema-exports beema-documents beema-attachments
```

## 3. Run the Application

```bash
cd beema-kernel
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Wait until you see:

```
Started BeemaKernelApplication in X.XXX seconds
```

## 4. Trigger an Export

```bash
curl -X POST http://localhost:8080/api/v1/batch/export/parquet \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{"tenantId": "test-tenant"}'
```

Expected response:

```json
{
  "jobExecutionId": 1,
  "jobName": "universalParquetExport",
  "status": "STARTED",
  "tenantId": "test-tenant",
  "outputPath": "tenant=test-tenant/object=agreement/date=2024-02-13/abc123.parquet"
}
```

## 5. Browse Exported Files

Open the MinIO Console at http://localhost:9001

- **Username:** `admin`
- **Password:** `password123`
- Navigate to the `beema-exports` bucket to see Parquet files organized by tenant and date.

## What's Next?

| Topic                     | Document                                                        |
|---------------------------|-----------------------------------------------------------------|
| API reference             | [Batch API](api/BATCH_API.md)                                  |
| Architecture deep-dive    | [Analytics Layer Architecture](architecture/ANALYTICS_LAYER.md) |
| Production deployment     | [Deployment Guide](deployment/ANALYTICS_DEPLOYMENT.md)          |

## Storage Backend Options

| Backend    | Config                     | Use Case                     |
|------------|----------------------------|------------------------------|
| Filesystem | `beema.storage.type=filesystem` | Simplest, no dependencies   |
| MinIO      | `beema.storage.type=s3` + local profile | Local S3 simulation  |
| AWS S3     | `beema.storage.type=s3`   | Production on AWS             |
| Azure Blob | `beema.storage.type=azure` | Production on Azure          |

Switch backends by changing a single property -- no code changes required.
