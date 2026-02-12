-- =============================================================================
-- V6__seed_metadata.sql
-- Beema Unified Platform - Seed Metadata for All Market Contexts
-- =============================================================================
-- Inserts sample metadata for RETAIL, COMMERCIAL, and LONDON_MARKET contexts.
-- This enables immediate testing of the metadata-driven architecture.
--
-- NOTE: This seed data uses a fixed tenant_id for the demo/development tenant.
-- In production, each tenant would configure their own metadata.
-- =============================================================================

-- Demo tenant UUID (consistent across all seed data)
-- In production, this would come from the tenant provisioning system.
DO $$
DECLARE
    v_tenant_id UUID := 'a0000000-0000-0000-0000-000000000001'::UUID;

    -- Agreement type IDs
    v_motor_type_id         UUID;
    v_home_type_id          UUID;
    v_commercial_prop_id    UUID;
    v_liability_type_id     UUID;
    v_marine_cargo_id       UUID;
    v_property_treaty_id    UUID;

    -- Attribute IDs
    v_attr_vehicle_reg      UUID;
    v_attr_vehicle_make     UUID;
    v_attr_vehicle_year     UUID;
    v_attr_property_addr    UUID;
    v_attr_property_type    UUID;
    v_attr_rebuild_cost     UUID;
    v_attr_business_name    UUID;
    v_attr_employee_count   UUID;
    v_attr_revenue          UUID;
    v_attr_sic_code         UUID;
    v_attr_umr              UUID;
    v_attr_placing_basis    UUID;
    v_attr_vessel_name      UUID;
    v_attr_voyage_from      UUID;
    v_attr_voyage_to        UUID;
    v_attr_treaty_type      UUID;

BEGIN

-- =========================================================================
-- RETAIL CONTEXT - Agreement Types
-- =========================================================================

INSERT INTO metadata_agreement_types (id, tenant_id, type_code, type_name, description, market_context, schema_version, attribute_schema, validation_rules, ui_configuration)
VALUES (
    gen_random_uuid(), v_tenant_id, 'MOTOR_PERSONAL', 'Personal Motor Insurance',
    'Standard personal motor insurance policy for private vehicles.',
    'RETAIL', 1,
    '{
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "vehicle_registration": { "type": "string", "pattern": "^[A-Z0-9 ]+$" },
            "vehicle_make": { "type": "string" },
            "vehicle_model": { "type": "string" },
            "vehicle_year": { "type": "integer", "minimum": 1900, "maximum": 2030 },
            "cover_type": { "type": "string", "enum": ["COMPREHENSIVE", "THIRD_PARTY_FIRE_THEFT", "THIRD_PARTY_ONLY"] },
            "no_claims_bonus_years": { "type": "integer", "minimum": 0, "maximum": 20 },
            "annual_mileage": { "type": "integer", "minimum": 0 }
        },
        "required": ["vehicle_registration", "vehicle_make", "vehicle_year", "cover_type"]
    }'::JSONB,
    '[
        {"rule": "inception_before_expiry", "message": "Policy inception must be before expiry"},
        {"rule": "vehicle_year_not_future", "message": "Vehicle year cannot be in the future"},
        {"rule": "ncb_requires_prior_policy", "message": "NCB > 0 requires proof of prior policy"}
    ]'::JSONB,
    '{"sections": ["Vehicle Details", "Cover Options", "Claims History"], "layout": "wizard"}'::JSONB
) RETURNING id INTO v_motor_type_id;

INSERT INTO metadata_agreement_types (id, tenant_id, type_code, type_name, description, market_context, schema_version, attribute_schema, validation_rules, ui_configuration)
VALUES (
    gen_random_uuid(), v_tenant_id, 'HOME_STANDARD', 'Standard Home Insurance',
    'Combined buildings and contents home insurance policy.',
    'RETAIL', 1,
    '{
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "property_address": { "type": "object", "properties": {
                "line1": { "type": "string" }, "line2": { "type": "string" },
                "city": { "type": "string" }, "postcode": { "type": "string" },
                "country": { "type": "string", "default": "GB" }
            }, "required": ["line1", "city", "postcode"] },
            "property_type": { "type": "string", "enum": ["DETACHED", "SEMI_DETACHED", "TERRACED", "FLAT", "BUNGALOW"] },
            "year_built": { "type": "integer" },
            "number_of_bedrooms": { "type": "integer", "minimum": 1 },
            "rebuild_cost": { "type": "number", "minimum": 0 },
            "contents_value": { "type": "number", "minimum": 0 },
            "flood_risk_zone": { "type": "string", "enum": ["LOW", "MEDIUM", "HIGH"] },
            "security_features": { "type": "array", "items": { "type": "string" } }
        },
        "required": ["property_address", "property_type", "rebuild_cost"]
    }'::JSONB,
    '[
        {"rule": "rebuild_cost_minimum", "params": {"min": 50000}, "message": "Rebuild cost must be at least 50,000"},
        {"rule": "flood_zone_referral", "params": {"zone": "HIGH"}, "message": "High flood risk requires manual underwriting referral"}
    ]'::JSONB,
    '{"sections": ["Property Details", "Cover Amounts", "Security"], "layout": "tabbed"}'::JSONB
) RETURNING id INTO v_home_type_id;

-- =========================================================================
-- COMMERCIAL CONTEXT - Agreement Types
-- =========================================================================

INSERT INTO metadata_agreement_types (id, tenant_id, type_code, type_name, description, market_context, schema_version, attribute_schema, validation_rules, ui_configuration)
VALUES (
    gen_random_uuid(), v_tenant_id, 'COMMERCIAL_PROPERTY', 'Commercial Property Insurance',
    'Property insurance for commercial buildings and business premises.',
    'COMMERCIAL', 1,
    '{
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "business_name": { "type": "string" },
            "sic_code": { "type": "string", "pattern": "^[0-9]{4,5}$" },
            "property_address": { "type": "object", "properties": {
                "line1": { "type": "string" }, "city": { "type": "string" },
                "postcode": { "type": "string" }, "country": { "type": "string" }
            }},
            "building_value": { "type": "number", "minimum": 0 },
            "contents_value": { "type": "number", "minimum": 0 },
            "stock_value": { "type": "number", "minimum": 0 },
            "business_interruption_period": { "type": "integer", "minimum": 0, "maximum": 36 },
            "annual_revenue": { "type": "number", "minimum": 0 },
            "employee_count": { "type": "integer", "minimum": 1 },
            "construction_type": { "type": "string", "enum": ["STANDARD", "NON_STANDARD", "TIMBER_FRAME"] },
            "sprinkler_system": { "type": "boolean" }
        },
        "required": ["business_name", "sic_code", "building_value"]
    }'::JSONB,
    '[
        {"rule": "bi_requires_revenue", "message": "Business interruption cover requires annual revenue"},
        {"rule": "large_risk_referral", "params": {"threshold": 5000000}, "message": "Risks over 5M require senior underwriter approval"}
    ]'::JSONB,
    '{"sections": ["Business Details", "Property", "Cover Options", "Risk Assessment"], "layout": "tabbed"}'::JSONB
) RETURNING id INTO v_commercial_prop_id;

INSERT INTO metadata_agreement_types (id, tenant_id, type_code, type_name, description, market_context, schema_version, attribute_schema, validation_rules, ui_configuration)
VALUES (
    gen_random_uuid(), v_tenant_id, 'PUBLIC_LIABILITY', 'Public Liability Insurance',
    'Third-party liability cover for businesses.',
    'COMMERCIAL', 1,
    '{
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "business_name": { "type": "string" },
            "sic_code": { "type": "string" },
            "annual_turnover": { "type": "number", "minimum": 0 },
            "employee_count": { "type": "integer", "minimum": 0 },
            "indemnity_limit": { "type": "number", "enum": [1000000, 2000000, 5000000, 10000000] },
            "prior_claims_count": { "type": "integer", "minimum": 0 },
            "hazardous_activities": { "type": "boolean" },
            "work_at_height": { "type": "boolean" },
            "public_facing": { "type": "boolean" }
        },
        "required": ["business_name", "annual_turnover", "indemnity_limit"]
    }'::JSONB,
    '[
        {"rule": "hazardous_referral", "message": "Hazardous activities require specialist underwriter review"},
        {"rule": "claims_loading", "params": {"threshold": 2}, "message": "3+ prior claims triggers premium loading"}
    ]'::JSONB,
    '{"sections": ["Business Details", "Cover Requirements", "Risk Profile"], "layout": "wizard"}'::JSONB
) RETURNING id INTO v_liability_type_id;

-- =========================================================================
-- LONDON MARKET CONTEXT - Agreement Types
-- =========================================================================

INSERT INTO metadata_agreement_types (id, tenant_id, type_code, type_name, description, market_context, schema_version, attribute_schema, validation_rules, ui_configuration)
VALUES (
    gen_random_uuid(), v_tenant_id, 'MARINE_CARGO', 'Marine Cargo Insurance',
    'London Market marine cargo policy covering goods in transit.',
    'LONDON_MARKET', 1,
    '{
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "umr": { "type": "string", "pattern": "^B[0-9]{4}[A-Z][0-9A-Z]+$" },
            "placing_basis": { "type": "string", "enum": ["OPEN_MARKET", "LINESLIP", "BINDING_AUTHORITY", "CONSORTIUM"] },
            "vessel_name": { "type": "string" },
            "vessel_imo": { "type": "string" },
            "voyage_from": { "type": "string" },
            "voyage_to": { "type": "string" },
            "cargo_description": { "type": "string" },
            "cargo_value": { "type": "number", "minimum": 0 },
            "shipping_method": { "type": "string", "enum": ["SEA", "AIR", "ROAD", "RAIL", "MULTIMODAL"] },
            "institute_clauses": { "type": "string", "enum": ["ICC_A", "ICC_B", "ICC_C"] },
            "war_risk": { "type": "boolean" },
            "strikes_risk": { "type": "boolean" },
            "lead_underwriter_share": { "type": "number", "minimum": 0, "maximum": 100 }
        },
        "required": ["umr", "placing_basis", "cargo_description", "cargo_value", "institute_clauses"]
    }'::JSONB,
    '[
        {"rule": "umr_format_valid", "message": "UMR must follow Lloyds format (B + 4 digits + letter + alphanumeric)"},
        {"rule": "lead_share_minimum", "params": {"min": 5}, "message": "Lead underwriter share must be at least 5%"},
        {"rule": "total_line_shares", "message": "Total of all line shares must not exceed 100%"}
    ]'::JSONB,
    '{"sections": ["Slip Details", "Voyage", "Cargo", "Underwriting Lines"], "layout": "tabbed"}'::JSONB
) RETURNING id INTO v_marine_cargo_id;

INSERT INTO metadata_agreement_types (id, tenant_id, type_code, type_name, description, market_context, schema_version, attribute_schema, validation_rules, ui_configuration)
VALUES (
    gen_random_uuid(), v_tenant_id, 'PROPERTY_TREATY', 'Property Treaty Reinsurance',
    'London Market property treaty reinsurance contract.',
    'LONDON_MARKET', 1,
    '{
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
            "umr": { "type": "string" },
            "placing_basis": { "type": "string", "enum": ["OPEN_MARKET", "FACILITY"] },
            "treaty_type": { "type": "string", "enum": ["QUOTA_SHARE", "SURPLUS", "EXCESS_OF_LOSS", "STOP_LOSS"] },
            "ceding_company": { "type": "string" },
            "territory": { "type": "array", "items": { "type": "string" } },
            "retained_line": { "type": "number", "minimum": 0, "maximum": 100 },
            "cession_percentage": { "type": "number", "minimum": 0, "maximum": 100 },
            "occurrence_limit": { "type": "number", "minimum": 0 },
            "aggregate_limit": { "type": "number", "minimum": 0 },
            "reinstatements": { "type": "integer", "minimum": 0 },
            "loss_corridor": { "type": "object", "properties": {
                "attachment_point": { "type": "number" },
                "exit_point": { "type": "number" }
            }}
        },
        "required": ["umr", "treaty_type", "ceding_company"]
    }'::JSONB,
    '[
        {"rule": "cession_within_capacity", "message": "Cession percentage must not exceed available capacity"},
        {"rule": "territory_sanctions_check", "message": "Treaty territories must pass sanctions screening"}
    ]'::JSONB,
    '{"sections": ["Treaty Structure", "Ceding Company", "Limits & Retention", "Territory"], "layout": "tabbed"}'::JSONB
) RETURNING id INTO v_property_treaty_id;

-- =========================================================================
-- METADATA ATTRIBUTES - Retail
-- =========================================================================

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, validation_pattern, is_required, is_searchable, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'vehicle_registration', 'Vehicle Registration', 'STRING', '^[A-Z0-9 ]+$', TRUE, TRUE, 'text_input', 1, 'RETAIL', 'Vehicle')
    RETURNING id INTO v_attr_vehicle_reg;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, is_required, is_searchable, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'vehicle_make', 'Vehicle Make', 'STRING', TRUE, TRUE, 'dropdown', 2, 'RETAIL', 'Vehicle')
    RETURNING id INTO v_attr_vehicle_make;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, min_value, max_value, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'vehicle_year', 'Year of Manufacture', 'NUMBER', 1900, 2030, TRUE, 'number_input', 3, 'RETAIL', 'Vehicle')
    RETURNING id INTO v_attr_vehicle_year;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'property_address', 'Property Address', 'OBJECT', TRUE, 'address_input', 1, 'RETAIL', 'Property')
    RETURNING id INTO v_attr_property_addr;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, allowed_values, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'property_type', 'Property Type', 'ENUM', '["DETACHED","SEMI_DETACHED","TERRACED","FLAT","BUNGALOW"]'::JSONB, TRUE, 'dropdown', 2, 'RETAIL', 'Property')
    RETURNING id INTO v_attr_property_type;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, min_value, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'rebuild_cost', 'Rebuild Cost', 'CURRENCY', 0, TRUE, 'currency_input', 3, 'RETAIL', 'Property')
    RETURNING id INTO v_attr_rebuild_cost;

-- =========================================================================
-- METADATA ATTRIBUTES - Commercial
-- =========================================================================

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, is_required, is_searchable, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'business_name', 'Business Name', 'STRING', TRUE, TRUE, 'text_input', 1, 'COMMERCIAL', 'Business')
    RETURNING id INTO v_attr_business_name;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, min_value, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'employee_count', 'Number of Employees', 'NUMBER', 0, TRUE, 'number_input', 2, 'COMMERCIAL', 'Business')
    RETURNING id INTO v_attr_employee_count;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, min_value, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'annual_revenue', 'Annual Revenue', 'CURRENCY', 0, TRUE, 'currency_input', 3, 'COMMERCIAL', 'Business')
    RETURNING id INTO v_attr_revenue;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, validation_pattern, is_required, is_searchable, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'sic_code', 'SIC Code', 'STRING', '^[0-9]{4,5}$', TRUE, TRUE, 'text_input', 4, 'COMMERCIAL', 'Business')
    RETURNING id INTO v_attr_sic_code;

-- =========================================================================
-- METADATA ATTRIBUTES - London Market
-- =========================================================================

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, description, data_type, validation_pattern, is_required, is_searchable, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'umr', 'Unique Market Reference', 'Lloyds Unique Market Reference for the slip', 'STRING', '^B[0-9]{4}[A-Z][0-9A-Z]+$', TRUE, TRUE, 'text_input', 1, 'LONDON_MARKET', 'Slip')
    RETURNING id INTO v_attr_umr;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, allowed_values, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'placing_basis', 'Placing Basis', 'ENUM', '["OPEN_MARKET","LINESLIP","BINDING_AUTHORITY","CONSORTIUM","FACILITY"]'::JSONB, TRUE, 'dropdown', 2, 'LONDON_MARKET', 'Slip')
    RETURNING id INTO v_attr_placing_basis;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, is_searchable, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'vessel_name', 'Vessel Name', 'STRING', TRUE, 'text_input', 3, 'LONDON_MARKET', 'Marine')
    RETURNING id INTO v_attr_vessel_name;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'voyage_from', 'Voyage Origin', 'STRING', 'text_input', 4, 'LONDON_MARKET', 'Marine')
    RETURNING id INTO v_attr_voyage_from;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'voyage_to', 'Voyage Destination', 'STRING', 'text_input', 5, 'LONDON_MARKET', 'Marine')
    RETURNING id INTO v_attr_voyage_to;

INSERT INTO metadata_attributes (id, tenant_id, attribute_name, display_name, data_type, allowed_values, is_required, ui_component, ui_order, market_context, category)
VALUES
    (gen_random_uuid(), v_tenant_id, 'treaty_type', 'Treaty Type', 'ENUM', '["QUOTA_SHARE","SURPLUS","EXCESS_OF_LOSS","STOP_LOSS"]'::JSONB, TRUE, 'dropdown', 6, 'LONDON_MARKET', 'Treaty')
    RETURNING id INTO v_attr_treaty_type;

-- =========================================================================
-- LINK ATTRIBUTES TO AGREEMENT TYPES
-- =========================================================================

-- Motor -> Vehicle attributes
INSERT INTO metadata_agreement_type_attributes (agreement_type_id, attribute_id, section_name, is_required_override)
VALUES
    (v_motor_type_id, v_attr_vehicle_reg, 'Vehicle Details', TRUE),
    (v_motor_type_id, v_attr_vehicle_make, 'Vehicle Details', TRUE),
    (v_motor_type_id, v_attr_vehicle_year, 'Vehicle Details', TRUE);

-- Home -> Property attributes
INSERT INTO metadata_agreement_type_attributes (agreement_type_id, attribute_id, section_name, is_required_override)
VALUES
    (v_home_type_id, v_attr_property_addr, 'Property Details', TRUE),
    (v_home_type_id, v_attr_property_type, 'Property Details', TRUE),
    (v_home_type_id, v_attr_rebuild_cost, 'Cover Amounts', TRUE);

-- Commercial Property -> Business + Property attributes
INSERT INTO metadata_agreement_type_attributes (agreement_type_id, attribute_id, section_name, is_required_override)
VALUES
    (v_commercial_prop_id, v_attr_business_name, 'Business Details', TRUE),
    (v_commercial_prop_id, v_attr_sic_code, 'Business Details', TRUE),
    (v_commercial_prop_id, v_attr_employee_count, 'Business Details', FALSE),
    (v_commercial_prop_id, v_attr_revenue, 'Business Details', FALSE);

-- Public Liability -> Business attributes
INSERT INTO metadata_agreement_type_attributes (agreement_type_id, attribute_id, section_name, is_required_override)
VALUES
    (v_liability_type_id, v_attr_business_name, 'Business Details', TRUE),
    (v_liability_type_id, v_attr_employee_count, 'Business Details', TRUE),
    (v_liability_type_id, v_attr_revenue, 'Risk Profile', TRUE),
    (v_liability_type_id, v_attr_sic_code, 'Business Details', TRUE);

-- Marine Cargo -> London Market + Marine attributes
INSERT INTO metadata_agreement_type_attributes (agreement_type_id, attribute_id, section_name, is_required_override)
VALUES
    (v_marine_cargo_id, v_attr_umr, 'Slip Details', TRUE),
    (v_marine_cargo_id, v_attr_placing_basis, 'Slip Details', TRUE),
    (v_marine_cargo_id, v_attr_vessel_name, 'Voyage', FALSE),
    (v_marine_cargo_id, v_attr_voyage_from, 'Voyage', FALSE),
    (v_marine_cargo_id, v_attr_voyage_to, 'Voyage', FALSE);

-- Property Treaty -> London Market + Treaty attributes
INSERT INTO metadata_agreement_type_attributes (agreement_type_id, attribute_id, section_name, is_required_override)
VALUES
    (v_property_treaty_id, v_attr_umr, 'Treaty Structure', TRUE),
    (v_property_treaty_id, v_attr_placing_basis, 'Treaty Structure', TRUE),
    (v_property_treaty_id, v_attr_treaty_type, 'Treaty Structure', TRUE);

END $$;
