-- =============================================================================
-- V2__create_metadata_tables.sql
-- Beema Unified Platform - Metadata-Driven Configuration Tables
-- =============================================================================
-- The metadata layer drives the platform's schema-flexible architecture.
-- Agreement types define what data an agreement can carry (via JSON Schema),
-- while metadata attributes define individual field-level rules.
--
-- This approach allows new product types to be onboarded via configuration
-- rather than code changes, supporting Retail, Commercial, and London Market
-- contexts from the same schema.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. metadata_agreement_types
-- ---------------------------------------------------------------------------
-- Each row defines a type of agreement (e.g., "Motor Policy", "Property Risk",
-- "Marine Cargo"). The attribute_schema column holds a JSON Schema document
-- that describes the expected structure of the JSONB `attributes` column on
-- the agreements table. validation_rules holds additional business rules.

CREATE TABLE metadata_agreement_types (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL,
    type_code           VARCHAR(100)    NOT NULL,
    type_name           VARCHAR(255)    NOT NULL,
    description         TEXT,
    market_context      market_context_type NOT NULL,
    schema_version      INTEGER         NOT NULL DEFAULT 1,
    attribute_schema    JSONB           NOT NULL DEFAULT '{}'::JSONB,
    validation_rules    JSONB           NOT NULL DEFAULT '[]'::JSONB,
    ui_configuration    JSONB           NOT NULL DEFAULT '{}'::JSONB,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),

    CONSTRAINT pk_metadata_agreement_types PRIMARY KEY (id),
    CONSTRAINT uq_agreement_type_tenant UNIQUE (tenant_id, type_code, market_context, schema_version)
);

COMMENT ON TABLE metadata_agreement_types IS
    'Defines agreement types per market context with JSON Schema for attribute validation.';
COMMENT ON COLUMN metadata_agreement_types.attribute_schema IS
    'JSON Schema document describing the expected structure of agreement.attributes JSONB.';
COMMENT ON COLUMN metadata_agreement_types.validation_rules IS
    'Array of business validation rules applied beyond structural schema validation.';
COMMENT ON COLUMN metadata_agreement_types.ui_configuration IS
    'UI rendering hints (field ordering, grouping, conditional visibility).';

-- ---------------------------------------------------------------------------
-- 2. metadata_attributes
-- ---------------------------------------------------------------------------
-- Defines individual attribute metadata used across agreement types.
-- These provide field-level validation, display configuration, and
-- data-type enforcement that supplements the JSON Schema in agreement types.

CREATE TABLE metadata_attributes (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL,
    attribute_name      VARCHAR(255)    NOT NULL,
    display_name        VARCHAR(255)    NOT NULL,
    description         TEXT,
    data_type           VARCHAR(50)     NOT NULL,
    validation_pattern  VARCHAR(500),
    min_value           NUMERIC,
    max_value           NUMERIC,
    allowed_values      JSONB,
    default_value       JSONB,
    is_required         BOOLEAN         NOT NULL DEFAULT FALSE,
    is_searchable       BOOLEAN         NOT NULL DEFAULT FALSE,
    ui_component        VARCHAR(100),
    ui_order            INTEGER         NOT NULL DEFAULT 0,
    market_context      market_context_type NOT NULL,
    category            VARCHAR(100),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),

    CONSTRAINT pk_metadata_attributes PRIMARY KEY (id),
    CONSTRAINT uq_attribute_tenant UNIQUE (tenant_id, attribute_name, market_context)
);

COMMENT ON TABLE metadata_attributes IS
    'Field-level metadata for agreement attributes: data types, validation, and UI configuration.';
COMMENT ON COLUMN metadata_attributes.data_type IS
    'Logical data type: STRING, NUMBER, BOOLEAN, DATE, CURRENCY, PERCENTAGE, ENUM, OBJECT, ARRAY.';
COMMENT ON COLUMN metadata_attributes.validation_pattern IS
    'Regex pattern for string-type attribute validation.';
COMMENT ON COLUMN metadata_attributes.ui_component IS
    'Suggested UI component: text_input, dropdown, date_picker, currency_input, toggle, etc.';

-- ---------------------------------------------------------------------------
-- 3. metadata_agreement_type_attributes (join table)
-- ---------------------------------------------------------------------------
-- Links agreement types to their constituent attributes, allowing the same
-- attribute definition to be reused across multiple agreement types.

CREATE TABLE metadata_agreement_type_attributes (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    agreement_type_id       UUID        NOT NULL,
    attribute_id            UUID        NOT NULL,
    is_required_override    BOOLEAN,
    default_value_override  JSONB,
    ui_order_override       INTEGER,
    section_name            VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),

    CONSTRAINT pk_meta_type_attrs PRIMARY KEY (id),
    CONSTRAINT fk_type_attr_agreement_type
        FOREIGN KEY (agreement_type_id) REFERENCES metadata_agreement_types(id) ON DELETE CASCADE,
    CONSTRAINT fk_type_attr_attribute
        FOREIGN KEY (attribute_id) REFERENCES metadata_attributes(id) ON DELETE CASCADE,
    CONSTRAINT uq_type_attribute UNIQUE (agreement_type_id, attribute_id)
);

COMMENT ON TABLE metadata_agreement_type_attributes IS
    'Join table linking agreement types to their attributes, with per-type overrides.';

-- ---------------------------------------------------------------------------
-- 4. Indexes for metadata tables
-- ---------------------------------------------------------------------------

-- Agreement types lookup indexes
CREATE INDEX idx_meta_agreement_types_tenant ON metadata_agreement_types(tenant_id);
CREATE INDEX idx_meta_agreement_types_market ON metadata_agreement_types(market_context);
CREATE INDEX idx_meta_agreement_types_code ON metadata_agreement_types(type_code);
CREATE INDEX idx_meta_agreement_types_active ON metadata_agreement_types(tenant_id, is_active) WHERE is_active = TRUE;

-- GIN indexes for JSONB columns on metadata_agreement_types
CREATE INDEX idx_meta_agreement_types_attr_schema ON metadata_agreement_types USING GIN (attribute_schema);
CREATE INDEX idx_meta_agreement_types_validation ON metadata_agreement_types USING GIN (validation_rules);
CREATE INDEX idx_meta_agreement_types_ui_config ON metadata_agreement_types USING GIN (ui_configuration);

-- Attributes lookup indexes
CREATE INDEX idx_meta_attributes_tenant ON metadata_attributes(tenant_id);
CREATE INDEX idx_meta_attributes_market ON metadata_attributes(market_context);
CREATE INDEX idx_meta_attributes_name ON metadata_attributes(attribute_name);
CREATE INDEX idx_meta_attributes_searchable ON metadata_attributes(tenant_id, is_searchable) WHERE is_searchable = TRUE;
CREATE INDEX idx_meta_attributes_category ON metadata_attributes(category) WHERE category IS NOT NULL;

-- GIN indexes for JSONB columns on metadata_attributes
CREATE INDEX idx_meta_attributes_allowed_vals ON metadata_attributes USING GIN (allowed_values) WHERE allowed_values IS NOT NULL;

-- Join table indexes
CREATE INDEX idx_meta_type_attrs_type ON metadata_agreement_type_attributes(agreement_type_id);
CREATE INDEX idx_meta_type_attrs_attr ON metadata_agreement_type_attributes(attribute_id);

-- ---------------------------------------------------------------------------
-- 5. Updated_at trigger for metadata tables
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now() AT TIME ZONE 'UTC';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_meta_agreement_types_updated_at
    BEFORE UPDATE ON metadata_agreement_types
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_meta_attributes_updated_at
    BEFORE UPDATE ON metadata_attributes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
