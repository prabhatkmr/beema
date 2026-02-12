-- Layout definitions table
CREATE TABLE IF NOT EXISTS sys_layouts (
    layout_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_name VARCHAR(255) NOT NULL,
    layout_type VARCHAR(100) NOT NULL,  -- 'form', 'table', 'detail', 'dashboard'
    context VARCHAR(100) NOT NULL,      -- 'policy', 'claim', 'agreement'
    object_type VARCHAR(100) NOT NULL,  -- 'motor_policy', 'property_claim', etc.
    market_context VARCHAR(50) NOT NULL, -- 'RETAIL', 'COMMERCIAL', 'LONDON_MARKET'
    role VARCHAR(100),                   -- 'underwriter', 'claims_handler', null for all
    tenant_id VARCHAR(100),              -- Tenant-specific override, null for default
    layout_schema JSONB NOT NULL,       -- The actual layout JSON
    version INTEGER NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 100, -- Lower priority wins
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,

    CONSTRAINT uq_layout_name UNIQUE (layout_name, tenant_id, version)
);

CREATE INDEX idx_layouts_context ON sys_layouts(context, object_type);
CREATE INDEX idx_layouts_market ON sys_layouts(market_context);
CREATE INDEX idx_layouts_role ON sys_layouts(role) WHERE role IS NOT NULL;
CREATE INDEX idx_layouts_tenant ON sys_layouts(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_layouts_enabled ON sys_layouts(enabled) WHERE enabled = true;

-- Layout field permissions
CREATE TABLE IF NOT EXISTS sys_layout_field_permissions (
    permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_id UUID NOT NULL REFERENCES sys_layouts(layout_id) ON DELETE CASCADE,
    field_path VARCHAR(255) NOT NULL,    -- JSON path: 'sections[0].fields[1]'
    role VARCHAR(100) NOT NULL,
    visible_if TEXT,                     -- JEXL expression
    editable_if TEXT,                    -- JEXL expression
    required_if TEXT,                    -- JEXL expression
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_field_perms_layout ON sys_layout_field_permissions(layout_id);
CREATE INDEX idx_field_perms_role ON sys_layout_field_permissions(role);

-- Sample layout for Motor Policy (Retail context)
INSERT INTO sys_layouts (
    layout_name, layout_type, context, object_type, market_context,
    role, tenant_id, layout_schema, created_by
) VALUES (
    'motor-policy-form',
    'form',
    'policy',
    'motor_comprehensive',
    'RETAIL',
    NULL,
    NULL,
    '{
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
      ]
    }'::jsonb,
    'system'
);
