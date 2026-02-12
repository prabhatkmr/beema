# Layout Resolution Guide

## Overview
Server-driven UI system that resolves layouts based on context, user role, and tenant.

## API Endpoint

```
GET /api/v1/layouts/{context}/{objectType}
```

### Parameters

- **Path:**
  - `context` - policy, claim, agreement
  - `objectType` - motor_policy, property_claim, etc.

- **Query:**
  - `marketContext` - RETAIL, COMMERCIAL, LONDON_MARKET (default: RETAIL)

- **Headers:**
  - `X-Tenant-ID` - Tenant identifier (default: default)
  - `X-User-Role` - User role for permission filtering (default: user)

### Example

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL" \
  -H "X-Tenant-ID: acme-corp" \
  -H "X-User-Role: underwriter"
```

### Response

```json
{
  "title": "Motor Policy",
  "sections": [
    {
      "id": "vehicle-info",
      "title": "Vehicle Information",
      "visible_if": "true",
      "layout": "grid",
      "columns": 2,
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Make",
          "widget": "TEXT_INPUT",
          "required": true,
          "visible_if": "true",
          "editable_if": "user.role == \"underwriter\" || status == \"DRAFT\"",
          "validation": {
            "minLength": 2,
            "maxLength": 50
          }
        },
        {
          "id": "vehicle_model",
          "label": "Model",
          "widget": "TEXT_INPUT",
          "required": true,
          "visible_if": "true"
        },
        {
          "id": "vehicle_year",
          "label": "Year",
          "widget": "NUMBER_INPUT",
          "required": true,
          "visible_if": "true",
          "validation": {
            "min": 1990,
            "max": 2026
          }
        },
        {
          "id": "vehicle_value",
          "label": "Vehicle Value",
          "widget": "CURRENCY_INPUT",
          "required": true,
          "visible_if": "true"
        }
      ]
    },
    {
      "id": "driver-info",
      "title": "Driver Information",
      "visible_if": "true",
      "layout": "grid",
      "columns": 2,
      "fields": [
        {
          "id": "driver_name",
          "label": "Driver Name",
          "widget": "TEXT_INPUT",
          "required": true,
          "visible_if": "true"
        },
        {
          "id": "driver_age",
          "label": "Driver Age",
          "widget": "NUMBER_INPUT",
          "required": true,
          "visible_if": "true",
          "validation": {
            "min": 18,
            "max": 100
          }
        },
        {
          "id": "driver_license",
          "label": "License Number",
          "widget": "TEXT_INPUT",
          "required": true,
          "visible_if": "true"
        },
        {
          "id": "driving_experience",
          "label": "Years of Experience",
          "widget": "NUMBER_INPUT",
          "required": false,
          "visible_if": "true"
        }
      ]
    },
    {
      "id": "premium-info",
      "title": "Premium Calculation",
      "visible_if": "user.role == \"underwriter\" || user.role == \"admin\"",
      "layout": "grid",
      "columns": 2,
      "fields": [
        {
          "id": "base_premium",
          "label": "Base Premium",
          "widget": "CURRENCY_INPUT",
          "required": true,
          "visible_if": "user.role == \"underwriter\"",
          "editable_if": "user.role == \"underwriter\""
        },
        {
          "id": "discount",
          "label": "Discount %",
          "widget": "PERCENTAGE_INPUT",
          "required": false,
          "visible_if": "user.role == \"underwriter\""
        },
        {
          "id": "final_premium",
          "label": "Final Premium",
          "widget": "CURRENCY_INPUT",
          "required": false,
          "visible_if": "true",
          "editable_if": "false",
          "computed": "base_premium * (1 - discount / 100)"
        }
      ]
    }
  ],
  "_metadata": {
    "layoutId": "uuid",
    "layoutName": "motor-policy-form",
    "version": 1,
    "context": "policy",
    "objectType": "motor_comprehensive",
    "marketContext": "RETAIL"
  }
}
```

## Resolution Algorithm

The layout resolution service follows a priority-based algorithm to find the best matching layout:

1. **Query** `sys_layouts` for matching layouts
2. **Filter by:**
   - `context` (policy, claim, agreement)
   - `objectType` (motor_comprehensive, property_claim, etc.)
   - `marketContext` (RETAIL, COMMERCIAL, LONDON_MARKET)
   - `enabled = true`
3. **Match tenant:**
   - Tenant-specific layouts (matching tenantId) first
   - Default layouts (null tenantId) second
4. **Match role:**
   - Role-specific layouts (matching role) first
   - All roles (null role) second
5. **Order by priority:**
   - Lower priority number wins
   - Higher version wins for same priority
6. **Return** the highest priority match

## Priority Rules

Layouts are resolved in the following order:

1. **Tenant-specific** > Default (null tenant)
2. **Role-specific** > All roles (null role)
3. **Lower priority number** > Higher priority number
4. **Higher version** > Lower version

### Example Priority Scenarios

Given these layouts:

| Layout | Tenant | Role | Priority | Version |
|--------|--------|------|----------|---------|
| A | acme-corp | underwriter | 100 | 1 |
| B | null | underwriter | 100 | 1 |
| C | acme-corp | null | 100 | 1 |
| D | null | null | 100 | 1 |

For a user from `acme-corp` with role `underwriter`:
- **Selected:** Layout A (tenant + role match)
- Order: A > C > B > D

For a user from `other-corp` with role `underwriter`:
- **Selected:** Layout B (role match, default tenant)
- Order: B > D

## Adding New Layouts

### SQL Insert

```sql
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type,
    market_context, role, tenant_id, layout_schema, created_by
) VALUES (
    'custom-policy-form',
    'form',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    'underwriter',
    'acme-corp',
    '{
      "title": "Custom Motor Policy",
      "sections": [...]
    }'::jsonb,
    'admin'
);
```

### Field Attributes

Each field in a section can have:

- **id** - Unique field identifier
- **label** - Display label
- **widget** - Input widget type (TEXT_INPUT, NUMBER_INPUT, CURRENCY_INPUT, etc.)
- **required** - Boolean, whether field is required
- **visible_if** - JEXL expression for visibility
- **editable_if** - JEXL expression for editability
- **validation** - Validation rules (minLength, maxLength, min, max, pattern, etc.)
- **computed** - Expression for computed fields

### Widget Types

Available widget types:

- `TEXT_INPUT` - Single-line text input
- `TEXT_AREA` - Multi-line text input
- `NUMBER_INPUT` - Numeric input
- `CURRENCY_INPUT` - Currency input with formatting
- `PERCENTAGE_INPUT` - Percentage input
- `DATE_INPUT` - Date picker
- `SELECT` - Dropdown select
- `RADIO` - Radio button group
- `CHECKBOX` - Checkbox
- `SWITCH` - Toggle switch

## Layout Types

Supported layout types:

- **form** - Data entry forms
- **table** - Tabular data display
- **detail** - Read-only detail view
- **dashboard** - Dashboard/summary view

## Administrative Endpoints

### List All Layouts

```bash
GET /api/v1/layouts/all
```

Returns metadata for all enabled layouts.

### Filter by Context

```bash
GET /api/v1/layouts/all?context=policy
```

Returns only layouts for the specified context.

### Health Check

```bash
GET /api/v1/layouts/health
```

Returns service health status.

## Testing

### Unit Tests

Run unit tests for the layout resolution service:

```bash
mvn test -Dtest=LayoutResolutionServiceTest
```

### Integration Tests

Run integration tests for the layout controller:

```bash
mvn test -Dtest=LayoutControllerTest
```

## Database Schema

### sys_layouts Table

| Column | Type | Description |
|--------|------|-------------|
| layout_id | UUID | Primary key |
| layout_name | VARCHAR(255) | Unique name (with tenant/version) |
| layout_type | VARCHAR(100) | form, table, detail, dashboard |
| context | VARCHAR(100) | policy, claim, agreement |
| object_type | VARCHAR(100) | motor_policy, property_claim, etc. |
| market_context | VARCHAR(50) | RETAIL, COMMERCIAL, LONDON_MARKET |
| role | VARCHAR(100) | User role (null = all roles) |
| tenant_id | VARCHAR(100) | Tenant ID (null = default) |
| layout_schema | JSONB | The actual layout JSON |
| version | INTEGER | Layout version |
| enabled | BOOLEAN | Whether layout is active |
| priority | INTEGER | Priority (lower wins) |
| created_at | TIMESTAMPTZ | Creation timestamp |
| updated_at | TIMESTAMPTZ | Last update timestamp |
| created_by | VARCHAR(100) | Creator identifier |

### sys_layout_field_permissions Table

| Column | Type | Description |
|--------|------|-------------|
| permission_id | UUID | Primary key |
| layout_id | UUID | Foreign key to sys_layouts |
| field_path | VARCHAR(255) | JSON path to field |
| role | VARCHAR(100) | User role |
| visible_if | TEXT | JEXL expression for visibility |
| editable_if | TEXT | JEXL expression for editability |
| required_if | TEXT | JEXL expression for requirement |
| created_at | TIMESTAMPTZ | Creation timestamp |

## Use Cases

### 1. Tenant-Specific Customization

A tenant wants a custom motor policy form:

```sql
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type,
    market_context, role, tenant_id, layout_schema, created_by, priority
) VALUES (
    'acme-motor-policy-form',
    'form',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    NULL,
    'acme-corp',
    '{ "title": "ACME Motor Policy", ... }'::jsonb,
    'admin',
    50  -- Higher priority than default (100)
);
```

### 2. Role-Specific Views

Different layout for underwriters vs. customers:

```sql
-- Underwriter view (more fields)
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type,
    market_context, role, tenant_id, layout_schema, created_by, priority
) VALUES (
    'motor-policy-underwriter',
    'form',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    'underwriter',
    NULL,
    '{ "title": "Motor Policy (Underwriter)", ... }'::jsonb,
    'admin',
    80
);

-- Customer view (limited fields)
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type,
    market_context, role, tenant_id, layout_schema, created_by, priority
) VALUES (
    'motor-policy-customer',
    'form',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    'customer',
    NULL,
    '{ "title": "Motor Policy (Customer)", ... }'::jsonb,
    'admin',
    80
);
```

### 3. Market Context Variations

Different layouts for retail vs. commercial:

```sql
-- Commercial market layout
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type,
    market_context, role, tenant_id, layout_schema, created_by
) VALUES (
    'motor-policy-commercial',
    'form',
    'policy',
    'motor_comprehensive',
    'COMMERCIAL',
    NULL,
    NULL,
    '{ "title": "Commercial Motor Policy", ... }'::jsonb,
    'admin'
);
```

## Best Practices

1. **Versioning**: Increment version number when updating existing layouts
2. **Priority**: Use priority to control override behavior (lower = higher priority)
3. **Defaults**: Always create a default layout (null tenant, null role) as fallback
4. **Testing**: Test layout resolution with different tenant/role combinations
5. **Performance**: Use appropriate indexes for query performance
6. **JEXL Expressions**: Keep expressions simple and well-documented
7. **Validation**: Define comprehensive validation rules in layout schema

## Troubleshooting

### No Layout Found

If no layout is returned for a specific context:

1. Check if a layout exists for that context/objectType:
   ```sql
   SELECT * FROM sys_layouts WHERE context = 'policy' AND object_type = 'motor_comprehensive';
   ```

2. Verify the layout is enabled:
   ```sql
   SELECT * FROM sys_layouts WHERE enabled = true;
   ```

3. Check tenant and role filters:
   ```sql
   SELECT * FROM sys_layouts
   WHERE (tenant_id = 'acme-corp' OR tenant_id IS NULL)
     AND (role = 'underwriter' OR role IS NULL);
   ```

### Wrong Layout Returned

If the wrong layout is selected:

1. Review priority values (lower priority wins)
2. Check tenant and role matching logic
3. Verify version numbers

### Performance Issues

If queries are slow:

1. Verify indexes are present:
   ```sql
   \d sys_layouts
   ```

2. Analyze query execution:
   ```sql
   EXPLAIN ANALYZE SELECT * FROM sys_layouts WHERE ...;
   ```

## Future Enhancements

Potential future improvements:

- Layout versioning and rollback
- Layout inheritance and composition
- Dynamic field permissions based on data context
- Layout templates and theme support
- Visual layout builder UI
- A/B testing for layouts
- Layout analytics and usage tracking

## Related Documentation

- [Beema Metadata System](METADATA_CACHE.md)
- [Agreement Management](IMPLEMENTATION_SUMMARY.md)
- [Event Publishing Guide](EVENT_PUBLISHING_GUIDE.md)
