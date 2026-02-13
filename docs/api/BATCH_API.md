# Batch Export API

## Overview

The Batch API provides endpoints for triggering asynchronous data export jobs. Exports run as Spring Batch jobs, streaming data from PostgreSQL through Avro serialization into Parquet files, then uploading to the configured blob storage backend.

## Authentication

All requests require a valid tenant identifier via the `X-Tenant-ID` header. In multi-tenant deployments, row-level security ensures tenant isolation at the database layer.

## Endpoints

### Export Agreements to Parquet

Triggers an asynchronous batch job that exports current agreements to Parquet format and uploads the result to blob storage.

```
POST /api/v1/batch/export/parquet
```

#### Request Headers

| Header        | Required | Description                          |
|---------------|----------|--------------------------------------|
| Content-Type  | Yes      | `application/json`                   |
| X-Tenant-ID   | Yes      | Tenant identifier for data isolation |

#### Request Body

```json
{
  "tenantId": "tenant-123",
  "fromDate": "2024-01-01",
  "toDate": "2024-12-31"
}
```

| Field      | Type   | Required | Description                                   |
|------------|--------|----------|-----------------------------------------------|
| `tenantId` | String | Yes      | Tenant identifier. Must match `X-Tenant-ID`.  |
| `fromDate` | Date   | No       | Filter agreements created on or after this date (ISO 8601). |
| `toDate`   | Date   | No       | Filter agreements created on or before this date (ISO 8601). |

#### Response

**Success (200 OK)**

```json
{
  "jobExecutionId": 1,
  "jobName": "universalParquetExport",
  "status": "STARTED",
  "tenantId": "tenant-123",
  "outputPath": "tenant=tenant-123/object=agreement/date=2024-02-13/a1b2c3d4-e5f6-7890-1234-567890abcdef.parquet"
}
```

| Field            | Type    | Description                                          |
|------------------|---------|------------------------------------------------------|
| `jobExecutionId` | Long    | Unique Spring Batch job execution ID for tracking.   |
| `jobName`        | String  | Name of the batch job (`universalParquetExport`).    |
| `status`         | String  | Job status (`STARTED`, `COMPLETED`, `FAILED`).       |
| `tenantId`       | String  | The tenant whose data is being exported.             |
| `outputPath`     | String  | Blob storage path where the Parquet file is written. |

#### Output Path Convention

```
tenant={tenantId}/object=agreement/date={yyyy-MM-dd}/{uuid}.parquet
```

This Hive-style partitioning enables efficient querying by downstream analytics tools (Athena, Spark, Trino).

#### Error Responses

**400 Bad Request** -- Missing or invalid fields.

```json
{
  "timestamp": "2024-02-13T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "tenantId is required"
}
```

**500 Internal Server Error** -- Job launch failure.

```json
{
  "timestamp": "2024-02-13T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to launch export job"
}
```

## Examples

### cURL

```bash
# Export all current agreements for a tenant
curl -X POST http://localhost:8080/api/v1/batch/export/parquet \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-123" \
  -d '{
    "tenantId": "tenant-123"
  }'

# Export with date range filter
curl -X POST http://localhost:8080/api/v1/batch/export/parquet \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-123" \
  -d '{
    "tenantId": "tenant-123",
    "fromDate": "2024-01-01",
    "toDate": "2024-06-30"
  }'
```

### HTTPie

```bash
http POST localhost:8080/api/v1/batch/export/parquet \
  X-Tenant-ID:tenant-123 \
  tenantId=tenant-123 \
  fromDate=2024-01-01 \
  toDate=2024-06-30
```

## OpenAPI / Swagger

When the application is running, interactive API documentation is available at:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI spec:** http://localhost:8080/api-docs

The endpoint is annotated with `@Tag(name = "Batch")` and `@Operation` for full Swagger integration.

## Job Lifecycle

1. **STARTED** -- Job accepted and queued for execution.
2. **IN_PROGRESS** -- Reader is streaming agreements, processor is converting to Avro, writer is building Parquet.
3. **COMPLETED** -- Parquet file uploaded to blob storage. Download via signed URL.
4. **FAILED** -- An error occurred. Check application logs for the `jobExecutionId`.

Spring Batch persists job metadata to PostgreSQL (`BATCH_*` tables), so job status survives application restarts.

## Data Format

The exported Parquet file contains:

| Column                  | Type   | Source                              |
|-------------------------|--------|-------------------------------------|
| `id`                    | String | Agreement UUID                      |
| `agreement_number`      | String | e.g., `POL-2024-001234`            |
| `agreement_type_code`   | String | e.g., `AUTO_POLICY`                |
| `market_context`        | String | `RETAIL`, `COMMERCIAL`, `LONDON_MARKET` |
| `status`                | String | `DRAFT`, `ACTIVE`, `EXPIRED`, etc. |
| `valid_from`            | String | Bitemporal valid time start (ISO 8601) |
| `valid_to`              | String | Bitemporal valid time end           |
| `transaction_time`      | String | Audit transaction timestamp         |
| `tenant_id`             | String | Tenant identifier                   |
| `data_residency_region` | String | Data compliance region              |
| `created_by`            | String | Creator identifier                  |
| `updated_by`            | String | Last updater identifier             |
| `version`               | Long   | Optimistic locking version          |
| `created_at`            | String | Creation timestamp                  |
| `updated_at`            | String | Last update timestamp               |
| `attr_*`                | String | Dynamic JSONB attributes, flattened |

Dynamic attributes from the JSONB `attributes` column are flattened with the `attr_` prefix. For example, an attribute `{"vehicle_vin": "1HGBH41JXMN109186"}` becomes column `attr_vehicle_vin`.
