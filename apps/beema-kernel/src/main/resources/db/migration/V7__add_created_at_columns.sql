-- =============================================================================
-- V7__add_created_at_columns.sql
-- Add missing created_at column to bitemporal tables
-- =============================================================================
-- The BitemporalEntity base class expects a created_at column for audit tracking.
-- This migration adds it to all agreement-related tables.
-- =============================================================================

-- Add created_at to agreements
ALTER TABLE agreements
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC');

COMMENT ON COLUMN agreements.created_at IS
    'Timestamp when this entity was first created (immutable).';

-- Add created_at to agreement_parties
ALTER TABLE agreement_parties
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC');

COMMENT ON COLUMN agreement_parties.created_at IS
    'Timestamp when this entity was first created (immutable).';

-- Add created_at to agreement_coverages
ALTER TABLE agreement_coverages
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC');

COMMENT ON COLUMN agreement_coverages.created_at IS
    'Timestamp when this entity was first created (immutable).';
