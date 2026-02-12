# Dynamic OpenAPI Generator - User Guide

## Overview

The **Dynamic OpenAPI Generator** automatically creates OpenAPI v3.0 specifications by introspecting the Beema Metadata Registry. This eliminates the need to manually maintain API documentation and ensures your API docs are always in sync with your metadata definitions.

## Why Dynamic OpenAPI?

**Problem:** In a metadata-driven platform, we don't have static Java Controllers for every business object. Instead, we use a generic `DynamicDataController` that handles all object types. Traditional OpenAPI generation tools can't document these dynamic endpoints.

**Solution:** Generate OpenAPI specs on-the-fly by:
1. Querying the Metadata Registry for all registered object types
2. Converting field definitions into OpenAPI schemas
3. Creating CRUD endpoints for each object type
4. Exposing the specification through a REST endpoint

## Key Features

✅ **Automatic Discovery** - Finds all registered object types from metadata
✅ **Type Mapping** - Converts Beema data types to OpenAPI types
✅ **Bitemporal Support** - Includes system fields (id, valid_from, valid_to, etc.)
✅ **Security** - Excludes internal-only fields from API docs
✅ **Validation** - Includes constraints (min/max, patterns, enums)
✅ **Multi-Tenant** - Filters by tenant ID
✅ **Market Context** - Filters by RETAIL, COMMERCIAL, or LONDON_MARKET
✅ **Import Ready** - Works with Postman, Swagger UI, and API clients

## Architecture

### Components

1. **DynamicSchemaGenerator** (`service/openapi/DynamicSchemaGenerator.java`)
   - Converts `CompiledObjectDefinition` → OpenAPI `Schema`
   - Maps field types to OpenAPI types
   - Adds bitemporal system fields
   - Applies validation constraints

2. **DynamicOpenApiController** (`api/v1/openapi/DynamicOpenApiController.java`)
   - REST endpoint: `GET /api/v1/docs/openapi.json`
   - Fetches all objects from Metadata Registry
   - Generates paths and operations
   - Returns complete OpenAPI v3.0 spec

### Data Flow

```
User Request
    ↓
GET /api/v1/docs/openapi.json
    ↓
DynamicOpenApiController
    ↓
Query MetadataRegistry (all active types)
    ↓
For each CompiledObjectDefinition:
    ↓
DynamicSchemaGenerator (create Schema)
    ↓
Build PathItem (POST, GET operations)
    ↓
Assemble OpenAPI object
    ↓
Return JSON
```

## Data Type Mapping

The schema generator maps Beema data types to OpenAPI types:

| Beema Type | OpenAPI Type | Format | Example |
|------------|--------------|--------|---------|
| STRING, TEXT | string | - | "John Doe" |
| CURRENCY, DECIMAL | number | double | 1234.56 |
| INTEGER, INT, LONG | integer | - | 42 |
| BOOLEAN | boolean | - | true |
| DATE | string | date | "2024-01-15" |
| DATETIME, TIMESTAMP | string | date-time | "2024-01-15T10:30:00Z" |
| UUID | string | uuid | "550e8400-e29b-41d4-a716-446655440000" |
| JSON, JSONB, OBJECT | object | - | { ... } |
| ARRAY | array | - | [...] |

## Bitemporal System Fields

Every object schema automatically includes these read-only fields:

```json
{
  "id": {
    "type": "string",
    "format": "uuid",
    "readOnly": true,
    "description": "Unique identifier"
  },
  "valid_from": {
    "type": "string",
    "format": "date-time",
    "readOnly": true,
    "description": "Start of validity period"
  },
  "valid_to": {
    "type": "string",
    "format": "date-time",
    "readOnly": true,
    "description": "End of validity period"
  },
  "transaction_time": {
    "type": "string",
    "format": "date-time",
    "readOnly": true,
    "description": "Transaction timestamp"
  },
  "is_current": {
    "type": "boolean",
    "readOnly": true,
    "description": "Whether this is the current version"
  },
  "version": {
    "type": "integer",
    "readOnly": true,
    "description": "Version number"
  },
  "tenant_id": {
    "type": "string",
    "readOnly": true,
    "description": "Tenant identifier"
  },
  "created_by": {
    "type": "string",
    "readOnly": true,
    "description": "User who created this record"
  },
  "updated_by": {
    "type": "string",
    "readOnly": true,
    "description": "User who last updated this record"
  }
}
```

## Usage

### 1. View in Browser

```bash
# View raw JSON
curl http://localhost:8080/api/v1/docs/openapi.json | jq .

# Save to file
curl http://localhost:8080/api/v1/docs/openapi.json -o beema-api.json
```

### 2. Import to Swagger UI

**Option A: Direct URL**
```
http://localhost:8080/swagger-ui/index.html?url=/api/v1/docs/openapi.json
```

**Option B: Swagger Editor**
1. Go to https://editor.swagger.io
2. File → Import URL
3. Paste: `http://localhost:8080/api/v1/docs/openapi.json`

### 3. Import to Postman

**Steps:**
1. Open Postman
2. Click **Import** button
3. Select **Link** tab
4. Paste: `http://localhost:8080/api/v1/docs/openapi.json`
5. Click **Continue** → **Import**

Postman will create a collection with all endpoints!

### 4. Filter by Tenant

```bash
# Only show objects for specific tenant
curl "http://localhost:8080/api/v1/docs/openapi.json?tenantId=550e8400-e29b-41d4-a716-446655440000"
```

### 5. Filter by Market Context

```bash
# Only RETAIL objects
curl "http://localhost:8080/api/v1/docs/openapi.json?marketContext=RETAIL"

# Only COMMERCIAL objects
curl "http://localhost:8080/api/v1/docs/openapi.json?marketContext=COMMERCIAL"

# Only LONDON_MARKET objects
curl "http://localhost:8080/api/v1/docs/openapi.json?marketContext=LONDON_MARKET"
```

## Generated Endpoints

For each registered object type, the generator creates:

### POST - Create Object

**Endpoint:** `/api/v1/data/{object-name}`

**Request Body:** Object schema (without read-only fields)

**Response:** 201 Created with full object (including bitemporal fields)

**Example:**
```json
POST /api/v1/data/motor-personal

{
  "policy_number": "POL-2024-001",
  "vehicle_make": "Toyota",
  "vehicle_model": "Camry",
  "vehicle_year": 2024,
  "premium_amount": 1200.00
}

Response (201):
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "policy_number": "POL-2024-001",
  "vehicle_make": "Toyota",
  "vehicle_model": "Camry",
  "vehicle_year": 2024,
  "premium_amount": 1200.00,
  "valid_from": "2024-01-15T10:30:00Z",
  "valid_to": "9999-12-31T23:59:59Z",
  "transaction_time": "2024-01-15T10:30:00Z",
  "is_current": true,
  "version": 1,
  "tenant_id": "default",
  "created_by": "user@example.com",
  "updated_by": "user@example.com"
}
```

### GET - List Objects

**Endpoint:** `/api/v1/data/{object-name}`

**Query Parameters:**
- `page` (integer) - Page number (default: 0)
- `size` (integer) - Page size (default: 20, max: 100)
- `valid_time` (date-time) - Point-in-time for validity
- `transaction_time` (date-time) - Point-in-time for transaction

**Response:** 200 OK with array of objects

**Example:**
```bash
GET /api/v1/data/motor-personal?page=0&size=10

Response (200):
[
  {
    "id": "550e8400-...",
    "policy_number": "POL-2024-001",
    ...
  },
  {
    "id": "660e8400-...",
    "policy_number": "POL-2024-002",
    ...
  }
]
```

## Validation Constraints

The schema generator includes validation rules from metadata:

### Numeric Constraints

```json
{
  "premium_amount": {
    "type": "number",
    "format": "double",
    "minimum": 0,
    "maximum": 1000000
  }
}
```

### String Patterns

```json
{
  "policy_number": {
    "type": "string",
    "pattern": "^POL-[0-9]{4}-[0-9]{3}$"
  }
}
```

### Enums

```json
{
  "vehicle_type": {
    "type": "string",
    "enum": ["SEDAN", "SUV", "TRUCK", "COUPE"]
  }
}
```

### Required Fields

```json
{
  "required": ["policy_number", "vehicle_make", "premium_amount"]
}
```

## Testing

### Run Test Script

```bash
cd apps/beema-kernel
./test-dynamic-openapi.sh
```

**What it tests:**
- ✅ beema-kernel is running
- ✅ OpenAPI endpoint is accessible
- ✅ OpenAPI structure is valid
- ✅ Info, paths, and servers sections exist
- ✅ Market context filtering works
- ✅ CRUD operations are defined

### Manual Testing

```bash
# 1. Check health
curl http://localhost:8080/actuator/health

# 2. Fetch OpenAPI spec
curl http://localhost:8080/api/v1/docs/openapi.json | jq .

# 3. Validate with spectral (optional)
npm install -g @stoplight/spectral-cli
spectral lint http://localhost:8080/api/v1/docs/openapi.json
```

## Example Output

```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "Beema Kernel - Dynamic API",
    "description": "Metadata-driven insurance platform API...",
    "version": "1.0.0-DYNAMIC",
    "contact": {
      "name": "Beema Platform Team",
      "email": "support@beema.io"
    }
  },
  "servers": [
    {
      "url": "http://localhost:8080",
      "description": "Local development server"
    }
  ],
  "paths": {
    "/api/v1/data/motor-personal": {
      "post": {
        "operationId": "createMotorPersonal",
        "summary": "Create new Motor Personal Policy",
        "tags": ["RETAIL", "MOTOR_PERSONAL"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "required": ["policy_number", "vehicle_make"],
                "properties": {
                  "policy_number": { "type": "string" },
                  "vehicle_make": { "type": "string" },
                  "premium_amount": {
                    "type": "number",
                    "format": "double"
                  }
                }
              }
            }
          }
        },
        "responses": {
          "201": {
            "description": "Successfully created"
          }
        }
      },
      "get": {
        "operationId": "listMotorPersonal",
        "summary": "List Motor Personal Policy objects",
        "parameters": [
          {
            "name": "page",
            "in": "query",
            "schema": { "type": "integer", "default": 0 }
          }
        ],
        "responses": {
          "200": {
            "description": "Successfully retrieved list"
          }
        }
      }
    }
  }
}
```

## Security Considerations

### Internal-Only Fields

Fields marked as `isInternalOnly()` in metadata are **excluded** from the OpenAPI spec for security reasons.

### Authentication

All endpoints require OAuth2/JWT authentication:
```
Authorization: Bearer <token>
```

### Multi-Tenancy

Include tenant ID in headers:
```
X-Tenant-ID: <tenant-uuid>
```

## Troubleshooting

### No paths generated

**Cause:** No active agreement types in metadata
**Solution:** Check `metadata_agreement_types` table:
```sql
SELECT type_code, is_active FROM metadata_agreement_types;
```

### Schema fields missing

**Cause:** Metadata fields not loaded
**Solution:** Refresh metadata cache:
```bash
curl -X POST http://localhost:8080/api/v1/metadata/cache/refresh
```

### Endpoint returns 404

**Cause:** Controller not registered
**Solution:** Check Spring component scan:
```java
@ComponentScan(basePackages = "com.beema.kernel")
```

## Advanced Usage

### Custom OpenAPI Extensions

Add custom extensions to the spec:

```java
// In DynamicOpenApiController
openAPI.addExtension("x-beema-version", "1.0.0");
openAPI.addExtension("x-generated-at", Instant.now().toString());
```

### Generate Client SDKs

Use OpenAPI Generator to create client libraries:

```bash
# Install OpenAPI Generator
npm install @openapitools/openapi-generator-cli -g

# Generate TypeScript client
openapi-generator-cli generate \
  -i http://localhost:8080/api/v1/docs/openapi.json \
  -g typescript-axios \
  -o ./client/typescript

# Generate Java client
openapi-generator-cli generate \
  -i http://localhost:8080/api/v1/docs/openapi.json \
  -g java \
  -o ./client/java
```

## Next Steps

1. **Add UPDATE/DELETE operations** - Currently only POST/GET
2. **Add schema components** - Reusable schema definitions
3. **Add security schemes** - OAuth2 flow documentation
4. **Add examples** - Sample request/response bodies
5. **Add webhooks** - Document event-driven endpoints

## References

- [OpenAPI Specification v3.0](https://swagger.io/specification/)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)

---

**Generated by:** Beema Dynamic OpenAPI Generator
**Version:** 1.0.0
**Last Updated:** 2026-02-12
