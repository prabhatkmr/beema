# Layout Security Trimming Guide

## Overview
Server-side evaluation of JEXL expressions to hide sections and fields based on user permissions and data context.

## How It Works

1. **Layout Resolution**: Layout is fetched from `sys_layouts` table
2. **Security Context**: User role, tenant, and data context collected
3. **JEXL Evaluation**: Each `visible_if` and `editable_if` expression evaluated
4. **Trimming**: Invisible sections/fields removed, non-editable fields marked `readonly`
5. **Clean Response**: JEXL expressions stripped from response (security)

## Expression Context

JEXL expressions have access to:

```javascript
{
  user: {
    id: "user-123",
    role: "underwriter",
    email: "user@example.com",
    tenantId: "acme-corp"
  },
  status: "DRAFT",  // Data context
  // ... other data fields
}
```

## Examples

### Hide Section for Non-Underwriters

```json
{
  "id": "premium-section",
  "visible_if": "user.role == \"underwriter\""
}
```

### Show Field Only in Draft Status

```json
{
  "id": "premium_field",
  "visible_if": "status == \"DRAFT\""
}
```

### Make Field Readonly After Approval

```json
{
  "id": "vehicle_make",
  "editable_if": "status == \"DRAFT\" || user.role == \"admin\""
}
```

### Complex Conditions

```json
{
  "visible_if": "(user.role == \"underwriter\" || user.role == \"admin\") && status != \"CANCELLED\""
}
```

## Security Benefits

- **Server-Side**: Cannot be bypassed by client
- **Expression Hiding**: JEXL expressions removed from response
- **Fail-Safe**: Expression errors default to hidden/readonly
- **Audit Trail**: All trimming logged

## Testing

```bash
# As underwriter
curl -H "X-User-Role: underwriter" \
  http://localhost:8080/api/v1/layouts/policy/motor_comprehensive

# As regular user
curl -H "X-User-Role: user" \
  http://localhost:8080/api/v1/layouts/policy/motor_comprehensive

# Compare responses - premium section should be hidden for user
```

## API Usage

### Basic Request (No Security Trimming)

```bash
GET /api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL
```

### Request with Security Trimming

```bash
POST /api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL
Headers:
  X-User-ID: user-123
  X-User-Role: underwriter
  X-User-Email: underwriter@example.com
  X-Tenant-ID: default
Body:
{
  "status": "DRAFT",
  "policyId": "POL-12345"
}
```

### Response Structure

```json
{
  "title": "Motor Comprehensive Policy",
  "sections": [
    {
      "id": "vehicle-section",
      "title": "Vehicle Details",
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Make",
          "widget": "text",
          "readonly": false
        }
      ]
    }
  ],
  "_metadata": {
    "layoutId": "uuid",
    "layoutName": "Motor Comprehensive",
    "version": 1,
    "context": "policy",
    "securityTrimmed": true
  }
}
```

## Configuration

### Layout Schema with Security Rules

```json
{
  "title": "Motor Policy",
  "sections": [
    {
      "id": "basic-info",
      "title": "Basic Information",
      "visible_if": "true",
      "fields": [
        {
          "id": "policy_number",
          "label": "Policy Number",
          "widget": "text",
          "visible_if": "true",
          "editable_if": "status == \"DRAFT\""
        }
      ]
    },
    {
      "id": "premium-section",
      "title": "Premium Calculation",
      "visible_if": "user.role == \"underwriter\" || user.role == \"admin\"",
      "fields": [
        {
          "id": "base_premium",
          "label": "Base Premium",
          "widget": "currency",
          "visible_if": "true",
          "editable_if": "user.role == \"underwriter\""
        }
      ]
    }
  ]
}
```

## Implementation Details

### LayoutSecurityService

Main service that applies security trimming:

- Evaluates `visible_if` expressions for sections and fields
- Evaluates `editable_if` expressions and sets `readonly` flag
- Removes JEXL expressions from client response
- Handles expression evaluation errors gracefully (fail-safe to hidden/readonly)

### LayoutResolutionService

Integrates security trimming into layout resolution:

- Calls LayoutSecurityService if user context is available
- Adds `securityTrimmed` flag to metadata
- Maintains backward compatibility (works without user context)

### LayoutController

Updated to accept user context headers:

- `X-User-ID`: User identifier
- `X-User-Role`: User role
- `X-User-Email`: User email
- `X-Tenant-ID`: Tenant identifier
- Request body: Optional data context for expression evaluation

## Error Handling

If a JEXL expression fails to evaluate:

- **Sections**: Hidden (fail-safe)
- **Fields**: Hidden (fail-safe)
- **Editable expressions**: Field marked readonly (fail-safe)
- **Logging**: Warning logged with expression and error

## Performance Considerations

- JEXL expressions are evaluated in-memory (fast)
- Security context is built once per request
- Sections and fields are filtered using Java streams
- Deep cloning of layout schema prevents mutation

## Best Practices

1. **Keep expressions simple**: Complex logic should be in backend code
2. **Test expressions**: Verify expressions work for all roles and states
3. **Use fail-safe defaults**: Design expressions to fail safely (hide/readonly)
4. **Document expressions**: Add comments in layout schema explaining logic
5. **Audit changes**: Log layout changes and expression modifications

## Migration Guide

### Existing Layouts

Layouts without security expressions continue to work:

- No `visible_if` = always visible
- No `editable_if` = always editable (unless other rules apply)

### Adding Security Rules

To add security trimming to existing layouts:

1. Add `visible_if` expressions to sections/fields
2. Add `editable_if` expressions to fields requiring conditional editability
3. Test with different user roles and data contexts
4. Update frontend to handle `readonly` flag

## Troubleshooting

### Section Not Visible

Check:
- `visible_if` expression syntax
- User role in request headers
- Data context in request body
- Server logs for expression errors

### Field Always Readonly

Check:
- `editable_if` expression
- User role and data context
- Expression evaluation errors in logs

### Expression Not Working

Verify:
- JEXL syntax is correct
- Variable names match context (user.role, status, etc.)
- String comparisons use double quotes
- Boolean operators are correct (&&, ||, !)

## Future Enhancements

Potential improvements:

- Expression validation on layout save
- Expression testing UI
- Role-based expression suggestions
- Expression performance monitoring
- Cached expression compilation
