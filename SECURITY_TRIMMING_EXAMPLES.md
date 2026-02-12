# Layout Security Trimming Examples

## Overview

This document provides practical examples of using the JEXL Expression Engine for server-side security trimming of layout JSON.

## Quick Start

### Example 1: Hide Premium Section from Non-Underwriters

**Layout Configuration:**

```json
{
  "title": "Motor Policy",
  "sections": [
    {
      "id": "basic-info",
      "title": "Basic Information",
      "visible_if": "true",
      "fields": [...]
    },
    {
      "id": "premium-section",
      "title": "Premium Calculation",
      "visible_if": "user.role == \"underwriter\" || user.role == \"admin\"",
      "fields": [
        {
          "id": "base_premium",
          "label": "Base Premium",
          "widget": "currency"
        }
      ]
    }
  ]
}
```

**API Request as Regular User:**

```bash
curl -X GET \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: user-123' \
  -H 'X-User-Role: user' \
  -H 'X-User-Email: user@example.com' \
  -H 'X-Tenant-ID: default'
```

**Response (Premium section hidden):**

```json
{
  "title": "Motor Policy",
  "sections": [
    {
      "id": "basic-info",
      "title": "Basic Information",
      "fields": [...]
    }
  ],
  "_metadata": {
    "layoutId": "...",
    "layoutName": "motor-policy-form",
    "version": 1,
    "context": "policy",
    "securityTrimmed": true
  }
}
```

**API Request as Underwriter:**

```bash
curl -X GET \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: uw-456' \
  -H 'X-User-Role: underwriter' \
  -H 'X-User-Email: underwriter@example.com' \
  -H 'X-Tenant-ID: default'
```

**Response (Premium section visible):**

```json
{
  "title": "Motor Policy",
  "sections": [
    {
      "id": "basic-info",
      "title": "Basic Information",
      "fields": [...]
    },
    {
      "id": "premium-section",
      "title": "Premium Calculation",
      "fields": [
        {
          "id": "base_premium",
          "label": "Base Premium",
          "widget": "currency"
        }
      ]
    }
  ],
  "_metadata": {
    "layoutId": "...",
    "layoutName": "motor-policy-form",
    "version": 1,
    "context": "policy",
    "securityTrimmed": true
  }
}
```

---

### Example 2: Make Fields Readonly After Approval

**Layout Configuration:**

```json
{
  "sections": [
    {
      "id": "vehicle-details",
      "title": "Vehicle Details",
      "visible_if": "true",
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Vehicle Make",
          "widget": "text",
          "visible_if": "true",
          "editable_if": "status == \"DRAFT\" || user.role == \"admin\""
        },
        {
          "id": "vehicle_model",
          "label": "Vehicle Model",
          "widget": "text",
          "visible_if": "true",
          "editable_if": "status == \"DRAFT\" || user.role == \"admin\""
        }
      ]
    }
  ]
}
```

**API Request (Regular user, DRAFT status):**

```bash
curl -X POST \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: user-123' \
  -H 'X-User-Role: user' \
  -H 'X-User-Email: user@example.com' \
  -H 'X-Tenant-ID: default' \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "DRAFT",
    "policyId": "POL-12345"
  }'
```

**Response (Fields editable):**

```json
{
  "sections": [
    {
      "id": "vehicle-details",
      "title": "Vehicle Details",
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Vehicle Make",
          "widget": "text",
          "readonly": false
        },
        {
          "id": "vehicle_model",
          "label": "Vehicle Model",
          "widget": "text",
          "readonly": false
        }
      ]
    }
  ]
}
```

**API Request (Regular user, APPROVED status):**

```bash
curl -X POST \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: user-123' \
  -H 'X-User-Role: user' \
  -H 'X-User-Email: user@example.com' \
  -H 'X-Tenant-ID: default' \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "APPROVED",
    "policyId": "POL-12345"
  }'
```

**Response (Fields readonly):**

```json
{
  "sections": [
    {
      "id": "vehicle-details",
      "title": "Vehicle Details",
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Vehicle Make",
          "widget": "text",
          "readonly": true
        },
        {
          "id": "vehicle_model",
          "label": "Vehicle Model",
          "widget": "text",
          "readonly": true
        }
      ]
    }
  ]
}
```

**API Request (Admin user, APPROVED status):**

```bash
curl -X POST \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: admin-789' \
  -H 'X-User-Role: admin' \
  -H 'X-User-Email: admin@example.com' \
  -H 'X-Tenant-ID: default' \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "APPROVED",
    "policyId": "POL-12345"
  }'
```

**Response (Fields editable for admin):**

```json
{
  "sections": [
    {
      "id": "vehicle-details",
      "title": "Vehicle Details",
      "fields": [
        {
          "id": "vehicle_make",
          "label": "Vehicle Make",
          "widget": "text",
          "readonly": false
        },
        {
          "id": "vehicle_model",
          "label": "Vehicle Model",
          "widget": "text",
          "readonly": false
        }
      ]
    }
  ]
}
```

---

### Example 3: Complex Visibility Rules

**Layout Configuration:**

```json
{
  "sections": [
    {
      "id": "claims-section",
      "title": "Claims History",
      "visible_if": "(user.role == \"underwriter\" || user.role == \"claims_handler\") && status != \"CANCELLED\"",
      "fields": [
        {
          "id": "claim_count",
          "label": "Number of Claims",
          "widget": "number",
          "visible_if": "claim_count > 0",
          "editable_if": "user.role == \"claims_handler\""
        }
      ]
    }
  ]
}
```

**API Request (Claims handler, active policy with claims):**

```bash
curl -X POST \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: ch-999' \
  -H 'X-User-Role: claims_handler' \
  -H 'X-User-Email: claims@example.com' \
  -H 'X-Tenant-ID: default' \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "ACTIVE",
    "policyId": "POL-12345",
    "claim_count": 2
  }'
```

**Response (Section and field visible and editable):**

```json
{
  "sections": [
    {
      "id": "claims-section",
      "title": "Claims History",
      "fields": [
        {
          "id": "claim_count",
          "label": "Number of Claims",
          "widget": "number",
          "readonly": false
        }
      ]
    }
  ]
}
```

---

### Example 4: Tenant-Specific Visibility

**Layout Configuration:**

```json
{
  "sections": [
    {
      "id": "regulatory-section",
      "title": "Regulatory Information",
      "visible_if": "user.tenantId == \"uk-branch\" && (user.role == \"compliance\" || user.role == \"admin\")",
      "fields": [
        {
          "id": "fca_reference",
          "label": "FCA Reference Number",
          "widget": "text"
        }
      ]
    }
  ]
}
```

**API Request (UK branch compliance officer):**

```bash
curl -X GET \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: comp-123' \
  -H 'X-User-Role: compliance' \
  -H 'X-User-Email: compliance@uk.example.com' \
  -H 'X-Tenant-ID: uk-branch'
```

**Response (Regulatory section visible):**

```json
{
  "sections": [
    {
      "id": "regulatory-section",
      "title": "Regulatory Information",
      "fields": [
        {
          "id": "fca_reference",
          "label": "FCA Reference Number",
          "widget": "text"
        }
      ]
    }
  ]
}
```

**API Request (US branch compliance officer):**

```bash
curl -X GET \
  'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL' \
  -H 'X-User-ID: comp-456' \
  -H 'X-User-Role: compliance' \
  -H 'X-User-Email: compliance@us.example.com' \
  -H 'X-Tenant-ID: us-branch'
```

**Response (Regulatory section hidden):**

```json
{
  "sections": []
}
```

---

## Common JEXL Expressions

### User Role Checks

```javascript
// Single role
user.role == "underwriter"

// Multiple roles
user.role == "underwriter" || user.role == "admin"

// Role negation
user.role != "guest"
```

### Status Checks

```javascript
// Specific status
status == "DRAFT"

// Multiple statuses
status == "DRAFT" || status == "PENDING"

// Status exclusion
status != "CANCELLED"
```

### Combined Conditions

```javascript
// Role AND status
user.role == "underwriter" && status == "DRAFT"

// Complex boolean logic
(user.role == "underwriter" || user.role == "admin") && status != "CANCELLED"
```

### Data Value Checks

```javascript
// Numeric comparison
claim_count > 0

// String equality
market == "LONDON_MARKET"

// Null checks
premium != null
```

### Tenant Checks

```javascript
// Specific tenant
user.tenantId == "uk-branch"

// Multiple tenants
user.tenantId == "uk-branch" || user.tenantId == "eu-branch"
```

---

## Security Considerations

1. **Server-Side Only**: All JEXL expressions are evaluated on the server. Client cannot bypass security.

2. **Expression Removal**: JEXL expressions (`visible_if`, `editable_if`, `required_if`) are removed from the response for security.

3. **Fail-Safe Defaults**:
   - If `visible_if` expression fails: Section/field is hidden
   - If `editable_if` expression fails: Field is marked readonly
   - This ensures security is never compromised by expression errors

4. **Audit Logging**: All security trimming operations are logged for audit purposes.

---

## Testing Security Trimming

### Unit Tests

See `LayoutSecurityServiceTest.java` for comprehensive test examples.

### Integration Testing

Use curl or Postman to test different user roles and data contexts:

```bash
# Test 1: Verify section hidden for regular user
curl -X GET 'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive' \
  -H 'X-User-Role: user'

# Test 2: Verify section visible for underwriter
curl -X GET 'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive' \
  -H 'X-User-Role: underwriter'

# Test 3: Verify field readonly after approval
curl -X POST 'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive' \
  -H 'X-User-Role: user' \
  -H 'Content-Type: application/json' \
  -d '{"status": "APPROVED"}'
```

---

## Troubleshooting

### Section Not Appearing

1. Check server logs for JEXL expression errors
2. Verify user role in request headers
3. Verify data context in request body
4. Test expression syntax in isolation

### Field Always Readonly

1. Check `editable_if` expression
2. Verify user role and data context values
3. Check server logs for expression evaluation errors

### JEXL Expression Errors

Common errors:

- **Syntax Error**: Missing quotes around strings
- **Variable Not Found**: Typo in variable name (e.g., `user.roles` instead of `user.role`)
- **Type Mismatch**: Comparing incompatible types (e.g., `claim_count == "2"` instead of `claim_count == 2`)

---

## Advanced Patterns

### Conditional Field Groups

```json
{
  "id": "london-market-section",
  "visible_if": "market == \"LONDON_MARKET\"",
  "fields": [
    {
      "id": "lloyd_syndicate",
      "visible_if": "user.role == \"underwriter\""
    }
  ]
}
```

### Dynamic Editability Based on Workflow

```json
{
  "id": "approval_notes",
  "editable_if": "(status == \"PENDING_APPROVAL\" && user.role == \"approver\") || (status == \"DRAFT\" && user.role == \"underwriter\")"
}
```

### Multi-Tenant Field Visibility

```json
{
  "id": "tax_rate",
  "visible_if": "user.tenantId == \"uk-branch\" || user.tenantId == \"eu-branch\"",
  "editable_if": "user.role == \"admin\""
}
```
