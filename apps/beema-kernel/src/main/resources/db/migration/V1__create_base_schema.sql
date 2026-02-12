-- =============================================================================
-- V1__create_base_schema.sql
-- Beema Unified Platform - Base Schema Setup
-- =============================================================================
-- Establishes PostgreSQL extensions, enum types, and utility functions
-- required by all subsequent migrations. This is the foundation for the
-- bitemporal, metadata-driven, multi-tenant architecture.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Extensions
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- UUID v4 generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- Cryptographic functions (gen_random_uuid, encryption)

-- ---------------------------------------------------------------------------
-- 2. Enum Types
-- ---------------------------------------------------------------------------

-- Market context differentiates the three business verticals handled by the
-- unified platform: Retail (personal lines), Commercial (business lines),
-- and London Market (specialty / Lloyd's).
CREATE TYPE market_context_type AS ENUM (
    'RETAIL',
    'COMMERCIAL',
    'LONDON_MARKET'
);

-- Party roles define the relationship a party has to an agreement.
-- The same party can appear with different roles across agreements.
CREATE TYPE party_role_type AS ENUM (
    'POLICYHOLDER',
    'INSURER',
    'BROKER',
    'COVERHOLDER',
    'LEAD_UNDERWRITER',
    'FOLLOW_UNDERWRITER',
    'REINSURER',
    'CLAIMS_HANDLER',
    'THIRD_PARTY',
    'BENEFICIARY'
);

-- Agreement status tracks the lifecycle of a policy / agreement.
CREATE TYPE agreement_status_type AS ENUM (
    'DRAFT',
    'QUOTED',
    'BOUND',
    'ACTIVE',
    'ENDORSED',
    'RENEWED',
    'CANCELLED',
    'EXPIRED',
    'LAPSED'
);

-- ---------------------------------------------------------------------------
-- 3. Utility Functions for Bitemporal Operations
-- ---------------------------------------------------------------------------

-- BITEMPORAL PATTERN OVERVIEW
-- ===========================
-- Every business table carries four temporal columns:
--   valid_from       TIMESTAMPTZ  - start of business-validity period
--   valid_to         TIMESTAMPTZ  - end of business-validity period (infinity = open)
--   transaction_time TIMESTAMPTZ  - when this row version was recorded (immutable)
--   is_current       BOOLEAN      - TRUE for the latest transaction-time version
--
-- Composite primary key: (id, valid_from, transaction_time)
-- This allows multiple "corrections" of the same validity period to coexist,
-- each distinguishable by transaction_time.

-- make_bitemporal_current
-- -----------------------
-- Supersedes previous transaction-time rows for the same entity by setting
-- is_current = FALSE, then inserts the new version with is_current = TRUE.
-- Call this from application code via a trigger or service layer.

CREATE OR REPLACE FUNCTION make_bitemporal_current()
RETURNS TRIGGER AS $$
BEGIN
    -- Mark all prior versions of this entity (same id) as no longer current
    EXECUTE format(
        'UPDATE %I.%I SET is_current = FALSE WHERE id = $1 AND is_current = TRUE AND ctid <> $2',
        TG_TABLE_SCHEMA, TG_TABLE_NAME
    ) USING NEW.id, NEW.ctid;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION make_bitemporal_current() IS
    'Trigger function: marks prior transaction-time versions as non-current when a new version is inserted.';

-- get_current_timestamp_utc
-- -------------------------
-- Returns the current UTC timestamp, used as default for transaction_time.
CREATE OR REPLACE FUNCTION get_current_timestamp_utc()
RETURNS TIMESTAMPTZ AS $$
BEGIN
    RETURN now() AT TIME ZONE 'UTC';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- validate_valid_time_range
-- -------------------------
-- Ensures valid_from < valid_to on INSERT/UPDATE. Raises an exception if
-- the range is invalid.
CREATE OR REPLACE FUNCTION validate_valid_time_range()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.valid_to IS NOT NULL AND NEW.valid_to <> 'infinity'::TIMESTAMPTZ
       AND NEW.valid_from >= NEW.valid_to THEN
        RAISE EXCEPTION 'valid_from (%) must be before valid_to (%)',
            NEW.valid_from, NEW.valid_to;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION validate_valid_time_range() IS
    'Trigger function: validates that valid_from is strictly before valid_to.';

-- set_transaction_time
-- --------------------
-- Automatically stamps transaction_time on INSERT to the current UTC time.
-- transaction_time is immutable once written.
CREATE OR REPLACE FUNCTION set_transaction_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.transaction_time := now() AT TIME ZONE 'UTC';
    NEW.is_current := TRUE;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION set_transaction_time() IS
    'Trigger function: sets transaction_time to current UTC and marks row as current on INSERT.';

-- attach_bitemporal_triggers
-- --------------------------
-- Helper to attach all standard bitemporal triggers to a given table.
-- Usage: SELECT attach_bitemporal_triggers('public', 'agreements');
CREATE OR REPLACE FUNCTION attach_bitemporal_triggers(schema_name TEXT, table_name TEXT)
RETURNS VOID AS $$
BEGIN
    -- Trigger: auto-set transaction_time and is_current on INSERT
    EXECUTE format(
        'CREATE TRIGGER trg_%I_set_txn_time
         BEFORE INSERT ON %I.%I
         FOR EACH ROW EXECUTE FUNCTION set_transaction_time()',
        table_name, schema_name, table_name
    );

    -- Trigger: validate valid_from < valid_to
    EXECUTE format(
        'CREATE TRIGGER trg_%I_validate_valid_range
         BEFORE INSERT OR UPDATE ON %I.%I
         FOR EACH ROW EXECUTE FUNCTION validate_valid_time_range()',
        table_name, schema_name, table_name
    );

    -- Trigger: supersede prior versions
    EXECUTE format(
        'CREATE TRIGGER trg_%I_make_current
         AFTER INSERT ON %I.%I
         FOR EACH ROW EXECUTE FUNCTION make_bitemporal_current()',
        table_name, schema_name, table_name
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION attach_bitemporal_triggers(TEXT, TEXT) IS
    'Attaches standard bitemporal triggers (transaction_time, validity, currency) to the specified table.';
