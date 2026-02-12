-- =============================================================================
-- V8__extend_metadata_for_registry.sql
-- Beema Unified Platform - Metadata Registry Extensions
-- =============================================================================
-- Extends metadata_attributes to support calculated/virtual fields and
-- dependency tracking for the MetadataRegistry cache layer.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Add field_type to distinguish standard vs calculated fields
-- ---------------------------------------------------------------------------
ALTER TABLE metadata_attributes
    ADD COLUMN field_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

COMMENT ON COLUMN metadata_attributes.field_type IS
    'Field classification: STANDARD (user-entered), CALCULATED (computed via script), DERIVED (lookup-based).';

-- ---------------------------------------------------------------------------
-- 2. Add calculation_script for computed/virtual fields
-- ---------------------------------------------------------------------------
ALTER TABLE metadata_attributes
    ADD COLUMN calculation_script TEXT;

COMMENT ON COLUMN metadata_attributes.calculation_script IS
    'Expression script for CALCULATED fields, e.g. "base_premium * loading_factor". Evaluated by the expression engine.';

-- ---------------------------------------------------------------------------
-- 3. Add depends_on for field dependency tracking
-- ---------------------------------------------------------------------------
ALTER TABLE metadata_attributes
    ADD COLUMN depends_on TEXT[];

COMMENT ON COLUMN metadata_attributes.depends_on IS
    'Array of attribute_name values this calculated field depends on. Used for topological sort of evaluation order.';

-- ---------------------------------------------------------------------------
-- 4. Index for field_type lookups (partial index for calculated fields)
-- ---------------------------------------------------------------------------
CREATE INDEX idx_meta_attributes_field_type
    ON metadata_attributes(tenant_id, field_type)
    WHERE field_type != 'STANDARD';

-- ---------------------------------------------------------------------------
-- 5. Constraint: calculation_script required for CALCULATED fields
-- ---------------------------------------------------------------------------
ALTER TABLE metadata_attributes
    ADD CONSTRAINT chk_calculated_has_script
    CHECK (field_type != 'CALCULATED' OR calculation_script IS NOT NULL);
