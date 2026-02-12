# Layout Resolution System - Implementation Summary

## Overview

This document summarizes the implementation of the server-driven UI layout resolution system in beema-kernel. The system resolves JSON layouts based on context, object type, user role, market context, and tenant.

## Deliverables Checklist

- [x] Database schema for `sys_layouts` and `sys_layout_field_permissions`
- [x] Layout domain model with JSONB support
- [x] LayoutRepository with priority-based query
- [x] LayoutResolutionService with resolution algorithm
- [x] LayoutSecurityService integration for security trimming
- [x] REST endpoint `GET /api/v1/layouts/{context}/{objectType}`
- [x] Administrative endpoints (`/all`, `/health`)
- [x] Sample motor policy layout
- [x] Unit tests
- [x] Integration tests
- [x] Documentation
- [x] Test script

## Files Created

### Database Migration

**File:** `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/main/resources/db/migration/V15__create_layout_system.sql`

Creates:
- `sys_layouts` table - Stores layout definitions with JSONB schema
- `sys_layout_field_permissions` table - Stores field-level permissions
- Indexes for efficient query performance
- Sample motor policy layout for testing

### Domain Model

**File:** `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/main/java/com/beema/kernel/domain/layout/Layout.java`

JPA entity mapping to `sys_layouts` table with:
- UUID primary key
- JSONB layout schema using JsonbConverter
- Metadata fields (version, priority, enabled)
- Context, object type, market context fields
- Tenant and role filtering fields

### Repository Layer

**File:** `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/main/java/com/beema/kernel/repository/layout/LayoutRepository.java`

Spring Data JPA repository with custom queries:
- `findMatchingLayouts()` - Priority-based layout resolution
- `findAllByContext()` - Filter layouts by context
- `findAllEnabled()` - List all enabled layouts

### Service Layer

**File:** `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/main/java/com/beema/kernel/service/layout/LayoutResolutionService.java`

Core business logic for layout resolution:
- `resolveLayout()` - Main resolution algorithm
- `getAllLayouts()` - List available layouts
- `getDefaultLayout()` - Fallback for missing layouts
- Integration with LayoutSecurityService for JEXL-based security trimming

### API Controller

**File:** `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/main/java/com/beema/kernel/api/v1/layout/LayoutController.java`

REST API endpoints:
- `GET /api/v1/layouts/{context}/{objectType}` - Resolve layout
- `GET /api/v1/layouts/all` - List all layouts
- `GET /api/v1/layouts/health` - Health check

### Tests

**Files:**
- `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/test/java/com/beema/kernel/service/layout/LayoutResolutionServiceTest.java`
- `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/src/test/java/com/beema/kernel/api/v1/layout/LayoutControllerTest.java`

Test coverage:
- Layout resolution with different roles
- Default layout fallback
- Section and field validation
- Context filtering
- API endpoint testing with MockMvc

### Documentation

**Files:**
- `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/LAYOUT_RESOLUTION_GUIDE.md` - Comprehensive user guide
- `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/LAYOUT_IMPLEMENTATION_SUMMARY.md` - This document

### Test Script

**File:** `/Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel/test-layout-api.sh`

Executable bash script for testing the API endpoints with curl.

## Architecture

### Layout Resolution Flow

```
Client Request
    |
    v
LayoutController
    |
    v
LayoutResolutionService
    |
    +-- LayoutRepository.findMatchingLayouts()
    |       |
    |       v
    |   PostgreSQL (sys_layouts table)
    |
    +-- LayoutSecurityService.applySecurityTrimming()
    |       |
    |       v
    |   JEXL Expression Evaluation
    |
    v
JSON Layout Response
```

### Priority-Based Resolution

The system uses a multi-level priority algorithm:

1. **Tenant Match**: Tenant-specific layouts override defaults
2. **Role Match**: Role-specific layouts override generic ones
3. **Priority Number**: Lower priority value wins
4. **Version**: Higher version wins for same priority

### Database Schema

#### sys_layouts

| Column | Type | Description |
|--------|------|-------------|
| layout_id | UUID | Primary key |
| layout_name | VARCHAR(255) | Unique name with tenant/version |
| layout_type | VARCHAR(100) | form, table, detail, dashboard |
| context | VARCHAR(100) | policy, claim, agreement |
| object_type | VARCHAR(100) | motor_policy, property_claim, etc. |
| market_context | VARCHAR(50) | RETAIL, COMMERCIAL, LONDON_MARKET |
| role | VARCHAR(100) | User role (null = all) |
| tenant_id | VARCHAR(100) | Tenant ID (null = default) |
| layout_schema | JSONB | Layout JSON |
| version | INTEGER | Version number |
| enabled | BOOLEAN | Active flag |
| priority | INTEGER | Priority (lower wins) |
| created_at | TIMESTAMPTZ | Creation time |
| updated_at | TIMESTAMPTZ | Update time |
| created_by | VARCHAR(100) | Creator |

## API Examples

### Get Layout

```bash
curl -X GET "http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL" \
  -H "X-Tenant-ID: acme-corp" \
  -H "X-User-Role: underwriter" \
  -H "X-User-ID: user123" \
  -H "X-User-Email: underwriter@acme.com"
```

### Response

```json
{
  "title": "Motor Policy",
  "sections": [
    {
      "id": "vehicle-info",
      "title": "Vehicle Information",
      "layout": "grid",
      "columns": 2,
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Make",
          "widget": "TEXT_INPUT",
          "required": true,
          "editable_if": "user.role == \"underwriter\" || status == \"DRAFT\"",
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
    "layoutName": "motor-policy-form",
    "version": 1,
    "context": "policy",
    "objectType": "motor_comprehensive",
    "marketContext": "RETAIL",
    "securityTrimmed": true
  }
}
```

## Features

### 1. Multi-Tenant Support

Layouts can be customized per tenant:

```sql
INSERT INTO sys_layouts (
    layout_name, context, object_type, market_context,
    role, tenant_id, layout_schema, created_by
) VALUES (
    'acme-motor-policy',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    NULL,
    'acme-corp',
    '{ ... }'::jsonb,
    'admin'
);
```

### 2. Role-Based Layouts

Different views for different user roles:

```sql
-- Underwriter view
INSERT INTO sys_layouts (
    layout_name, context, object_type, market_context,
    role, tenant_id, layout_schema, created_by
) VALUES (
    'motor-policy-underwriter',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    'underwriter',
    NULL,
    '{ ... }'::jsonb,
    'admin'
);
```

### 3. Market Context Support

Separate layouts for Retail, Commercial, and London Market:

```sql
INSERT INTO sys_layouts (
    layout_name, context, object_type, market_context,
    role, tenant_id, layout_schema, created_by
) VALUES (
    'motor-policy-commercial',
    'policy',
    'motor_comprehensive',
    'COMMERCIAL',
    NULL,
    NULL,
    '{ ... }'::jsonb,
    'admin'
);
```

### 4. JEXL Expression Support

Field visibility, editability, and requirements can be controlled with JEXL expressions:

```json
{
  "id": "base_premium",
  "label": "Base Premium",
  "widget": "CURRENCY_INPUT",
  "visible_if": "user.role == 'underwriter'",
  "editable_if": "user.role == 'underwriter' && status == 'DRAFT'",
  "required_if": "status == 'ACTIVE'"
}
```

### 5. Security Trimming

When user context is provided, layouts are automatically trimmed based on:
- Field-level permissions
- JEXL expressions (visible_if, editable_if, required_if)
- User role and tenant context

### 6. Versioning

Layouts support versioning for controlled rollouts:

```sql
-- Version 1
INSERT INTO sys_layouts (layout_name, version, ...) VALUES ('motor-policy', 1, ...);

-- Version 2 (new version with changes)
INSERT INTO sys_layouts (layout_name, version, priority, ...)
VALUES ('motor-policy', 2, 50, ...);
```

## Testing

### Compile

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
mvn clean compile
```

### Run Unit Tests

```bash
mvn test -Dtest=LayoutResolutionServiceTest
```

### Run Integration Tests

```bash
mvn test -Dtest=LayoutControllerTest
```

### Run API Tests

```bash
./test-layout-api.sh
```

## Database Migration

To apply the migration:

```bash
# Using Flyway (automatic on application startup)
mvn spring-boot:run

# Or manually with Flyway CLI
flyway migrate
```

The migration creates:
- Tables: `sys_layouts`, `sys_layout_field_permissions`
- Indexes for performance
- Sample motor policy layout

## Configuration

No additional configuration required. The system uses existing Spring Boot and PostgreSQL configuration.

Optional environment variables:
- `BASE_URL` - API base URL for test script (default: http://localhost:8080)

## Integration Points

### Existing Services

The layout system integrates with:

1. **LayoutSecurityService** - For JEXL-based security trimming
2. **JsonbConverter** - For JSONB serialization/deserialization
3. **PostgreSQL** - For layout storage and querying

### Future Integrations

Potential future integrations:
- Metadata system for schema validation
- Workflow engine for dynamic layouts
- Caching layer for performance
- Analytics for layout usage tracking

## Sample Layout Schema

The sample motor policy layout includes:

**Sections:**
1. Vehicle Information (4 fields)
   - Make, Model, Year, Value
2. Driver Information (4 fields)
   - Name, Age, License, Experience
3. Premium Calculation (3 fields - underwriter only)
   - Base Premium, Discount, Final Premium (computed)

**Features Demonstrated:**
- Grid layout with 2 columns
- Required/optional fields
- Input validation (min/max, length)
- Role-based visibility
- Computed fields
- Different widget types (TEXT_INPUT, NUMBER_INPUT, CURRENCY_INPUT, PERCENTAGE_INPUT)

## Performance Considerations

### Indexes

The following indexes are created for optimal query performance:

```sql
CREATE INDEX idx_layouts_context ON sys_layouts(context, object_type);
CREATE INDEX idx_layouts_market ON sys_layouts(market_context);
CREATE INDEX idx_layouts_role ON sys_layouts(role) WHERE role IS NOT NULL;
CREATE INDEX idx_layouts_tenant ON sys_layouts(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_layouts_enabled ON sys_layouts(enabled) WHERE enabled = true;
```

### Query Optimization

The repository query uses:
- Conditional ordering with CASE statements
- Filtered indexes (WHERE enabled = true)
- Composite filtering for efficient lookups

### Caching Strategy

Consider adding caching for frequently accessed layouts:

```java
@Cacheable(value = "layouts", key = "#context + '-' + #objectType + '-' + #marketContext")
public Map<String, Object> resolveLayout(...) {
    // ...
}
```

## Security

### Field-Level Security

Fields can be secured using:
1. `visible_if` - Controls field visibility
2. `editable_if` - Controls field editability
3. `required_if` - Controls field requirement

### JEXL Expressions

The system evaluates JEXL expressions with context:
- `user.role` - Current user role
- `user.email` - Current user email
- `user.id` - Current user ID
- `status` - Data object status
- Custom data context passed by client

### Tenant Isolation

Layouts are tenant-isolated by default:
- Tenant-specific layouts override defaults
- Each tenant can have custom layouts
- Default layouts (null tenant_id) serve as fallback

## Error Handling

### Missing Layouts

When no layout is found:
1. System logs a warning
2. Returns a default empty layout
3. Includes metadata indicating default fallback

### Invalid Expressions

When JEXL expressions fail:
1. Field is hidden (fail-safe)
2. Error is logged
3. User sees reduced layout

## Monitoring

### Logging

The service logs:
- Layout resolution requests (INFO level)
- Layout selection (INFO level)
- Missing layouts (WARN level)

### Metrics

Consider adding metrics:
- Layout resolution count by context
- Cache hit/miss ratio
- Resolution time percentiles

## Next Steps

1. **Deploy Migration**: Run database migration in target environment
2. **Create Custom Layouts**: Add organization-specific layouts
3. **Test API**: Use provided test script to verify functionality
4. **Monitor Performance**: Track resolution times and cache effectiveness
5. **Gather Feedback**: Collect user feedback on layout UX

## Support

For questions or issues:
1. Check logs for ERROR/WARN messages
2. Verify database migration applied successfully
3. Test with sample layout first
4. Review LAYOUT_RESOLUTION_GUIDE.md for detailed documentation

## Related Documentation

- [LAYOUT_RESOLUTION_GUIDE.md](LAYOUT_RESOLUTION_GUIDE.md) - Detailed API and usage guide
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Overall project documentation
- [METADATA_CACHE.md](METADATA_CACHE.md) - Metadata system documentation
