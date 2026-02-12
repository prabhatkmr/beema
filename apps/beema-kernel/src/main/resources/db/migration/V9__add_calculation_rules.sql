-- =============================================================================
-- V9__add_calculation_rules.sql
-- Add calculationRules JSONB column to metadata_agreement_types
-- =============================================================================
-- Stores expression-based calculation rules for computed/virtual fields.
-- Each rule specifies a target field and a JEXL expression that computes its
-- value from other agreement fields and attributes.
--
-- Example rule:
-- {"targetField":"net_premium","expression":"totalPremium * (1 - commission_rate / 100)","resultType":"CURRENCY","scale":4,"order":1}

ALTER TABLE metadata_agreement_types
ADD COLUMN IF NOT EXISTS calculation_rules JSONB NOT NULL DEFAULT '[]'::JSONB;

COMMENT ON COLUMN metadata_agreement_types.calculation_rules IS
    'Array of calculation rules: [{targetField, expression, resultType, scale, order}]. Evaluated during agreement create/update.';
