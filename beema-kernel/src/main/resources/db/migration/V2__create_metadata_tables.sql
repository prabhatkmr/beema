-- V2: Metadata Tables
--
-- Creates the metadata registry that defines:
-- - Agreement types and their schemas
-- - Valid attributes per market context
-- - Validation rules

-- ============================================================================
-- METADATA: Agreement Types
-- ============================================================================
-- Defines types of agreements (e.g., "AUTO_POLICY", "HOMEOWNERS") with their
-- JSON schemas for validation.

CREATE TABLE metadata_agreement_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Unique identifier for the agreement type
    type_code VARCHAR(100) NOT NULL,

    -- Market context (RETAIL, COMMERCIAL, LONDON_MARKET)
    market_context VARCHAR(50) NOT NULL,

    -- Schema version (allows schema evolution over time)
    schema_version INTEGER NOT NULL DEFAULT 1,

    -- Human-readable name
    display_name VARCHAR(200) NOT NULL,

    -- Description
    description TEXT,

    -- JSON Schema defining required/optional attributes
    -- Example:
    -- {
    --   "type": "object",
    --   "required": ["vehicle_vin", "vehicle_year"],
    --   "properties": {
    --     "vehicle_vin": {"type": "string", "pattern": "^[A-HJ-NPR-Z0-9]{17}$"},
    --     "vehicle_year": {"type": "integer", "minimum": 1900, "maximum": 2100}
    --   }
    -- }
    attribute_schema JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Business validation rules (evaluated at runtime)
    -- Example:
    -- {
    --   "premium_calculation": {
    --     "base_rate": 500,
    --     "factors": ["driver_age", "vehicle_value"]
    --   },
    --   "underwriting_rules": {
    --     "max_vehicle_age": 15,
    --     "min_driver_age": 18
    --   }
    -- }
    validation_rules JSONB DEFAULT '{}'::jsonb,

    -- Active flag
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_by VARCHAR(100) NOT NULL DEFAULT CURRENT_USER,
    updated_by VARCHAR(100) NOT NULL DEFAULT CURRENT_USER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_agreement_type_context_version
        UNIQUE (type_code, market_context, schema_version),

    CONSTRAINT chk_market_context
        CHECK (market_context IN ('RETAIL', 'COMMERCIAL', 'LONDON_MARKET')),

    CONSTRAINT chk_schema_version_positive
        CHECK (schema_version > 0)
);

-- Indexes
CREATE INDEX idx_metadata_agreement_types_code ON metadata_agreement_types(type_code);
CREATE INDEX idx_metadata_agreement_types_context ON metadata_agreement_types(market_context);
CREATE INDEX idx_metadata_agreement_types_active ON metadata_agreement_types(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_metadata_agreement_types_schema_gin ON metadata_agreement_types USING GIN (attribute_schema);

-- Triggers
CREATE TRIGGER audit_metadata_agreement_types
    BEFORE INSERT OR UPDATE ON metadata_agreement_types
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE metadata_agreement_types IS 'Registry of agreement types with JSON schemas for validation';
COMMENT ON COLUMN metadata_agreement_types.type_code IS 'Unique code for agreement type (e.g., AUTO_POLICY, HOMEOWNERS)';
COMMENT ON COLUMN metadata_agreement_types.market_context IS 'Which market this type applies to';
COMMENT ON COLUMN metadata_agreement_types.attribute_schema IS 'JSON Schema defining valid attributes and their types';
COMMENT ON COLUMN metadata_agreement_types.validation_rules IS 'Business rules for underwriting and rating';

-- ============================================================================
-- METADATA: Attributes Catalog
-- ============================================================================
-- Registry of all possible attributes that can be used in agreements.

CREATE TABLE metadata_attributes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Unique attribute key (e.g., "vehicle_vin", "driver_age")
    attribute_key VARCHAR(100) NOT NULL,

    -- Market context (attribute may have different meaning per context)
    market_context VARCHAR(50) NOT NULL,

    -- Display name for UI
    display_name VARCHAR(200) NOT NULL,

    -- Description
    description TEXT,

    -- Data type (STRING, INTEGER, DECIMAL, BOOLEAN, DATE, ARRAY, OBJECT)
    data_type VARCHAR(50) NOT NULL,

    -- Validation pattern (regex for strings, range for numbers)
    validation_pattern VARCHAR(500),

    -- UI component hint (TEXT_INPUT, NUMBER_INPUT, DATE_PICKER, DROPDOWN, etc.)
    ui_component VARCHAR(50),

    -- Dropdown options (if applicable)
    -- Example: ["SEDAN", "SUV", "TRUCK"]
    options JSONB,

    -- Default value
    default_value JSONB,

    -- Required flag
    is_required BOOLEAN NOT NULL DEFAULT FALSE,

    -- Active flag
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_by VARCHAR(100) NOT NULL DEFAULT CURRENT_USER,
    updated_by VARCHAR(100) NOT NULL DEFAULT CURRENT_USER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_attribute_key_context
        UNIQUE (attribute_key, market_context),

    CONSTRAINT chk_attribute_market_context
        CHECK (market_context IN ('RETAIL', 'COMMERCIAL', 'LONDON_MARKET')),

    CONSTRAINT chk_data_type
        CHECK (data_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'ARRAY', 'OBJECT'))
);

-- Indexes
CREATE INDEX idx_metadata_attributes_key ON metadata_attributes(attribute_key);
CREATE INDEX idx_metadata_attributes_context ON metadata_attributes(market_context);
CREATE INDEX idx_metadata_attributes_active ON metadata_attributes(is_active) WHERE is_active = TRUE;

-- Triggers
CREATE TRIGGER audit_metadata_attributes
    BEFORE INSERT OR UPDATE ON metadata_attributes
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Comments
COMMENT ON TABLE metadata_attributes IS 'Catalog of all possible attributes across all market contexts';
COMMENT ON COLUMN metadata_attributes.attribute_key IS 'Unique identifier for the attribute';
COMMENT ON COLUMN metadata_attributes.data_type IS 'Data type for validation and UI rendering';
COMMENT ON COLUMN metadata_attributes.validation_pattern IS 'Regex or range for validation';
COMMENT ON COLUMN metadata_attributes.ui_component IS 'Hint for UI component to use';
COMMENT ON COLUMN metadata_attributes.options IS 'Dropdown options (if applicable)';

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.1.0', 'Metadata tables (agreement types, attributes)', CURRENT_USER);
