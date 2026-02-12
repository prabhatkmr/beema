-- V1: Base Schema - Extensions and Helper Functions
--
-- Sets up PostgreSQL extensions and utility functions needed by all tables.

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable JSONB indexing (GIN indexes)
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- Create schema version tracking
CREATE TABLE IF NOT EXISTS schema_version (
    version VARCHAR(50) PRIMARY KEY,
    description TEXT,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by VARCHAR(100) NOT NULL DEFAULT CURRENT_USER
);

INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.0.0', 'Base schema with extensions and functions', CURRENT_USER);

-- Helper function: Get current transaction time
-- Returns current timestamp with timezone for bitemporal tracking
CREATE OR REPLACE FUNCTION get_transaction_time()
RETURNS TIMESTAMPTZ AS $$
BEGIN
    RETURN CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Helper function: Get default valid_to (far future)
CREATE OR REPLACE FUNCTION get_default_valid_to()
RETURNS TIMESTAMPTZ AS $$
BEGIN
    RETURN '9999-12-31 23:59:59+00'::TIMESTAMPTZ;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Helper function: Validate temporal range
-- Ensures valid_from < valid_to
CREATE OR REPLACE FUNCTION validate_temporal_range()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.valid_from >= NEW.valid_to THEN
        RAISE EXCEPTION 'valid_from must be before valid_to: % >= %',
            NEW.valid_from, NEW.valid_to;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Audit trigger function: Set created_by and updated_by
-- This will be used by all bitemporal tables
CREATE OR REPLACE FUNCTION audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_by := COALESCE(NEW.created_by, CURRENT_USER);
        NEW.updated_by := COALESCE(NEW.updated_by, CURRENT_USER);
        NEW.created_at := CURRENT_TIMESTAMP;
        NEW.updated_at := CURRENT_TIMESTAMP;
    ELSIF TG_OP = 'UPDATE' THEN
        NEW.created_by := OLD.created_by;  -- Preserve original creator
        NEW.created_at := OLD.created_at;  -- Preserve original timestamp
        NEW.updated_by := COALESCE(NEW.updated_by, CURRENT_USER);
        NEW.updated_at := CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- JSONB validation function: Check against JSON Schema
-- This will be enhanced in later migrations with actual validation logic
CREATE OR REPLACE FUNCTION validate_jsonb_schema(data JSONB, schema JSONB)
RETURNS BOOLEAN AS $$
BEGIN
    -- Placeholder: Implement JSON Schema validation
    -- For now, just check that data is valid JSONB
    RETURN data IS NOT NULL;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Comments
COMMENT ON EXTENSION "uuid-ossp" IS 'UUID generation functions';
COMMENT ON EXTENSION "btree_gin" IS 'GIN indexing support for JSONB';
COMMENT ON FUNCTION get_transaction_time() IS 'Returns current timestamp for bitemporal tracking';
COMMENT ON FUNCTION get_default_valid_to() IS 'Returns far-future date (9999-12-31) for open-ended validity';
COMMENT ON FUNCTION validate_temporal_range() IS 'Trigger function ensuring valid_from < valid_to';
COMMENT ON FUNCTION audit_trigger() IS 'Trigger function for automatic audit field population';
COMMENT ON FUNCTION validate_jsonb_schema(JSONB, JSONB) IS 'Validates JSONB data against JSON Schema';
