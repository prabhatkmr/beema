# Layout API - Quick Start Guide

## Overview

Server-driven UI layout resolution system that returns JSON layout schemas based on context, object type, user role, and tenant.

## Quick Start

### 1. Apply Database Migration

The migration will run automatically on application startup via Flyway:

```bash
mvn spring-boot:run
```

This creates the `sys_layouts` and `sys_layout_field_permissions` tables with a sample motor policy layout.

### 2. Test the API

Use the provided test script:

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
./test-layout-api.sh
```

Or manually with curl:

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL" \
  -H "X-Tenant-ID: default" \
  -H "X-User-Role: user" \
  -H "Content-Type: application/json"
```

### 3. View Sample Layout

The migration includes a complete motor policy layout with:

- Vehicle Information section (4 fields)
- Driver Information section (4 fields)
- Premium Calculation section (3 fields - role-restricted)

### 4. Create Custom Layouts

Add your own layouts via SQL:

```sql
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type, market_context,
    role, tenant_id, layout_schema, created_by
) VALUES (
    'my-custom-layout',
    'form',
    'policy',
    'property_policy',
    'RETAIL',
    NULL,
    'my-tenant',
    '{
      "title": "Property Policy",
      "sections": [
        {
          "id": "property-details",
          "title": "Property Details",
          "layout": "grid",
          "columns": 2,
          "fields": [
            {
              "id": "address",
              "label": "Property Address",
              "widget": "TEXT_INPUT",
              "required": true
            }
          ]
        }
      ]
    }'::jsonb,
    'admin'
);
```

## API Endpoints

### Get Layout

```
GET /api/v1/layouts/{context}/{objectType}
```

**Parameters:**
- `context` - policy, claim, agreement
- `objectType` - motor_comprehensive, property_claim, etc.
- `marketContext` (query) - RETAIL, COMMERCIAL, LONDON_MARKET
- `X-Tenant-ID` (header) - Tenant identifier
- `X-User-Role` (header) - User role
- `X-User-ID` (header) - User ID (for security trimming)
- `X-User-Email` (header) - User email (for security trimming)

**Example:**

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL" \
  -H "X-Tenant-ID: acme-corp" \
  -H "X-User-Role: underwriter" \
  -H "X-User-ID: user123" \
  -H "X-User-Email: john@acme.com"
```

### List All Layouts

```
GET /api/v1/layouts/all?context=policy
```

**Example:**

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/all?context=policy"
```

### Health Check

```
GET /api/v1/layouts/health
```

**Example:**

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/health"
```

## Layout Schema Structure

```json
{
  "title": "Layout Title",
  "sections": [
    {
      "id": "section-id",
      "title": "Section Title",
      "visible_if": "true",
      "layout": "grid",
      "columns": 2,
      "fields": [
        {
          "id": "field_id",
          "label": "Field Label",
          "widget": "TEXT_INPUT",
          "required": true,
          "visible_if": "true",
          "editable_if": "user.role == 'admin'",
          "validation": {
            "minLength": 2,
            "maxLength": 50
          }
        }
      ]
    }
  ],
  "_metadata": {
    "layoutId": "uuid",
    "layoutName": "layout-name",
    "version": 1,
    "context": "policy",
    "objectType": "motor_comprehensive",
    "marketContext": "RETAIL",
    "securityTrimmed": true
  }
}
```

## Widget Types

Available widget types:

- `TEXT_INPUT` - Single-line text
- `TEXT_AREA` - Multi-line text
- `NUMBER_INPUT` - Numeric input
- `CURRENCY_INPUT` - Currency with formatting
- `PERCENTAGE_INPUT` - Percentage
- `DATE_INPUT` - Date picker
- `SELECT` - Dropdown
- `RADIO` - Radio buttons
- `CHECKBOX` - Checkbox
- `SWITCH` - Toggle switch

## Resolution Priority

Layouts are resolved in this order:

1. Tenant-specific + Role-specific
2. Tenant-specific + All roles
3. Default tenant + Role-specific
4. Default tenant + All roles
5. Lower priority number wins
6. Higher version wins

## JEXL Expressions

Control field behavior with expressions:

```json
{
  "visible_if": "user.role == 'underwriter'",
  "editable_if": "status == 'DRAFT'",
  "required_if": "marketContext == 'COMMERCIAL'"
}
```

Available context:
- `user.role` - Current user role
- `user.email` - Current user email
- `user.id` - Current user ID
- `status` - Data object status
- Custom data context

## Files Created

### Core Implementation

- `src/main/java/com/beema/kernel/domain/layout/Layout.java` - Domain model
- `src/main/java/com/beema/kernel/repository/layout/LayoutRepository.java` - Data access
- `src/main/java/com/beema/kernel/service/layout/LayoutResolutionService.java` - Business logic
- `src/main/java/com/beema/kernel/api/v1/layout/LayoutController.java` - REST API

### Database

- `src/main/resources/db/migration/V15__create_layout_system.sql` - Schema + sample data

### Tests

- `src/test/java/com/beema/kernel/service/layout/LayoutResolutionServiceTest.java` - Service tests
- `src/test/java/com/beema/kernel/api/v1/layout/LayoutControllerTest.java` - API tests

### Documentation

- `LAYOUT_RESOLUTION_GUIDE.md` - Comprehensive guide
- `LAYOUT_IMPLEMENTATION_SUMMARY.md` - Implementation details
- `LAYOUT_API_QUICK_START.md` - This document

### Tools

- `test-layout-api.sh` - API test script

## Verification

### 1. Compile

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

### 2. Run Tests

```bash
mvn test -Dtest=LayoutResolutionServiceTest
mvn test -Dtest=LayoutControllerTest
```

Note: Tests require database to be running

### 3. Start Application

```bash
mvn spring-boot:run
```

### 4. Test API

```bash
./test-layout-api.sh
```

## Sample Use Cases

### Use Case 1: Default Motor Policy

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive"
```

Returns the default motor policy layout with all sections visible to regular users.

### Use Case 2: Underwriter View

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive" \
  -H "X-User-Role: underwriter"
```

Returns motor policy layout with premium calculation section visible.

### Use Case 3: Tenant-Specific Layout

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive" \
  -H "X-Tenant-ID: acme-corp"
```

Returns ACME Corp's custom motor policy layout (if configured).

### Use Case 4: Commercial Market

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=COMMERCIAL"
```

Returns commercial market motor policy layout (if configured).

## Troubleshooting

### Issue: No layout found

**Symptom:** API returns default empty layout

**Solution:**
1. Check if layout exists in database
2. Verify context and objectType match
3. Check enabled flag is true

```sql
SELECT * FROM sys_layouts
WHERE context = 'policy'
  AND object_type = 'motor_comprehensive'
  AND enabled = true;
```

### Issue: Build fails

**Symptom:** Compilation errors

**Solution:**
1. Ensure all files are present
2. Check Java version (requires Java 21+)
3. Run `mvn clean compile`

### Issue: Tests fail

**Symptom:** Test execution errors

**Solution:**
1. Ensure database is running
2. Check database connection in application-test.properties
3. Verify migration has been applied

## Next Steps

1. Review the sample motor policy layout
2. Create layouts for your specific use cases
3. Test with different roles and tenants
4. Integrate with frontend application
5. Configure caching for performance

## Support

For detailed documentation:
- API Reference: `LAYOUT_RESOLUTION_GUIDE.md`
- Implementation Details: `LAYOUT_IMPLEMENTATION_SUMMARY.md`

For questions or issues, check application logs and database state.
