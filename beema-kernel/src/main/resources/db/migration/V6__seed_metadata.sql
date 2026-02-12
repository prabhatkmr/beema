-- V6: Seed Metadata
--
-- Populates sample metadata for all three market contexts:
-- - RETAIL: Auto insurance
-- - COMMERCIAL: General liability
-- - LONDON_MARKET: Marine cargo

-- ============================================================================
-- RETAIL: Auto Insurance Policy
-- ============================================================================

INSERT INTO metadata_agreement_types (
    id,
    type_code,
    market_context,
    schema_version,
    display_name,
    description,
    attribute_schema,
    validation_rules,
    is_active
) VALUES (
    uuid_generate_v4(),
    'AUTO_POLICY',
    'RETAIL',
    1,
    'Auto Insurance Policy',
    'Personal auto insurance coverage for individual vehicles',
    '{
      "type": "object",
      "required": ["vehicle_vin", "vehicle_year", "vehicle_make", "vehicle_model", "primary_driver_age"],
      "properties": {
        "vehicle_vin": {
          "type": "string",
          "pattern": "^[A-HJ-NPR-Z0-9]{17}$",
          "description": "17-character Vehicle Identification Number"
        },
        "vehicle_year": {
          "type": "integer",
          "minimum": 1900,
          "maximum": 2100,
          "description": "Model year of the vehicle"
        },
        "vehicle_make": {
          "type": "string",
          "minLength": 1,
          "maxLength": 100,
          "description": "Vehicle manufacturer (e.g., Honda, Toyota)"
        },
        "vehicle_model": {
          "type": "string",
          "minLength": 1,
          "maxLength": 100,
          "description": "Vehicle model (e.g., Accord, Camry)"
        },
        "vehicle_value": {
          "type": "number",
          "minimum": 0,
          "description": "Actual cash value of vehicle in USD"
        },
        "primary_driver_age": {
          "type": "integer",
          "minimum": 16,
          "maximum": 120,
          "description": "Age of primary driver"
        },
        "primary_driver_license_number": {
          "type": "string",
          "description": "Driver license number"
        },
        "annual_mileage": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100000,
          "description": "Estimated annual mileage"
        },
        "vehicle_usage": {
          "type": "string",
          "enum": ["PERSONAL", "BUSINESS", "RIDESHARE"],
          "description": "Primary use of vehicle"
        },
        "garage_location": {
          "type": "object",
          "properties": {
            "street": {"type": "string"},
            "city": {"type": "string"},
            "state": {"type": "string"},
            "zip": {"type": "string"}
          }
        }
      }
    }'::jsonb,
    '{
      "underwriting_rules": {
        "max_vehicle_age": 30,
        "min_driver_age": 16,
        "max_driver_age": 85
      },
      "rating_factors": {
        "base_rate": 500,
        "age_discount_threshold": 25,
        "experience_years_for_discount": 5
      }
    }'::jsonb,
    TRUE
);

-- ============================================================================
-- COMMERCIAL: General Liability Policy
-- ============================================================================

INSERT INTO metadata_agreement_types (
    id,
    type_code,
    market_context,
    schema_version,
    display_name,
    description,
    attribute_schema,
    validation_rules,
    is_active
) VALUES (
    uuid_generate_v4(),
    'GENERAL_LIABILITY',
    'COMMERCIAL',
    1,
    'Commercial General Liability',
    'General liability coverage for businesses',
    '{
      "type": "object",
      "required": ["business_name", "industry_code", "annual_revenue", "number_of_employees"],
      "properties": {
        "business_name": {
          "type": "string",
          "minLength": 1,
          "maxLength": 200,
          "description": "Legal name of business"
        },
        "doing_business_as": {
          "type": "string",
          "description": "DBA name if different"
        },
        "business_structure": {
          "type": "string",
          "enum": ["SOLE_PROPRIETOR", "PARTNERSHIP", "LLC", "CORPORATION", "S_CORP"],
          "description": "Legal structure of business"
        },
        "industry_code": {
          "type": "string",
          "pattern": "^[0-9]{4,6}$",
          "description": "NAICS industry classification code"
        },
        "industry_description": {
          "type": "string",
          "description": "Description of business operations"
        },
        "annual_revenue": {
          "type": "number",
          "minimum": 0,
          "description": "Gross annual revenue in USD"
        },
        "number_of_employees": {
          "type": "integer",
          "minimum": 0,
          "description": "Total number of employees"
        },
        "years_in_business": {
          "type": "integer",
          "minimum": 0,
          "description": "Years business has been operating"
        },
        "locations": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "location_type": {"type": "string", "enum": ["PRIMARY", "BRANCH", "WAREHOUSE"]},
              "address": {"type": "string"},
              "square_footage": {"type": "integer"}
            }
          }
        },
        "prior_claims_count": {
          "type": "integer",
          "minimum": 0,
          "description": "Number of claims in last 5 years"
        }
      }
    }'::jsonb,
    '{
      "underwriting_rules": {
        "max_revenue_without_inspection": 10000000,
        "min_years_in_business": 0,
        "high_risk_industries": ["5812", "7389", "8699"]
      },
      "rating_factors": {
        "base_premium_per_employee": 500,
        "revenue_multiplier": 0.001,
        "claims_penalty_percent": 15
      }
    }'::jsonb,
    TRUE
);

-- ============================================================================
-- LONDON_MARKET: Marine Cargo Policy
-- ============================================================================

INSERT INTO metadata_agreement_types (
    id,
    type_code,
    market_context,
    schema_version,
    display_name,
    description,
    attribute_schema,
    validation_rules,
    is_active
) VALUES (
    uuid_generate_v4(),
    'MARINE_CARGO',
    'LONDON_MARKET',
    1,
    'Marine Cargo Insurance',
    'Lloyd''s marine cargo coverage for international shipments',
    '{
      "type": "object",
      "required": ["cargo_type", "vessel_name", "voyage_from", "voyage_to", "cargo_value"],
      "properties": {
        "cargo_type": {
          "type": "string",
          "enum": ["GENERAL_CARGO", "CONTAINERIZED", "BULK", "PERISHABLES", "HAZARDOUS"],
          "description": "Type of cargo being shipped"
        },
        "cargo_description": {
          "type": "string",
          "description": "Detailed description of cargo"
        },
        "cargo_value": {
          "type": "number",
          "minimum": 0,
          "description": "Declared value of cargo in USD"
        },
        "currency": {
          "type": "string",
          "enum": ["USD", "GBP", "EUR"],
          "default": "USD"
        },
        "vessel_name": {
          "type": "string",
          "description": "Name of vessel"
        },
        "vessel_imo_number": {
          "type": "string",
          "pattern": "^IMO[0-9]{7}$",
          "description": "International Maritime Organization number"
        },
        "voyage_from": {
          "type": "string",
          "description": "Port of origin"
        },
        "voyage_to": {
          "type": "string",
          "description": "Port of destination"
        },
        "sailing_date": {
          "type": "string",
          "format": "date",
          "description": "Expected sailing date"
        },
        "arrival_date": {
          "type": "string",
          "format": "date",
          "description": "Expected arrival date"
        },
        "incoterms": {
          "type": "string",
          "enum": ["EXW", "FOB", "CIF", "DDP"],
          "description": "International commercial terms"
        },
        "packing_method": {
          "type": "string",
          "description": "How cargo is packed/containerized"
        },
        "special_conditions": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": ["REFRIGERATED", "ARMED_GUARD", "TRANSHIPMENT_ALLOWED", "DECK_CARGO"]
          }
        }
      }
    }'::jsonb,
    '{
      "underwriting_rules": {
        "max_single_vessel_value": 50000000,
        "high_risk_routes": ["SOMALIA", "GULF_OF_ADEN", "STRAIT_OF_MALACCA"],
        "restricted_cargo": ["WEAPONS", "NUCLEAR_MATERIAL"]
      },
      "rating_factors": {
        "base_rate_per_thousand": 2.5,
        "perishables_surcharge_percent": 25,
        "hazardous_surcharge_percent": 50,
        "war_risk_additional_premium": 0.5
      }
    }'::jsonb,
    TRUE
);

-- ============================================================================
-- Sample Attributes Catalog
-- ============================================================================

-- RETAIL attributes
INSERT INTO metadata_attributes (attribute_key, market_context, display_name, description, data_type, validation_pattern, ui_component, is_required)
VALUES
    ('vehicle_vin', 'RETAIL', 'Vehicle VIN', '17-character Vehicle Identification Number', 'STRING', '^[A-HJ-NPR-Z0-9]{17}$', 'TEXT_INPUT', TRUE),
    ('vehicle_year', 'RETAIL', 'Vehicle Year', 'Model year of the vehicle', 'INTEGER', NULL, 'NUMBER_INPUT', TRUE),
    ('vehicle_make', 'RETAIL', 'Vehicle Make', 'Manufacturer (e.g., Honda, Toyota)', 'STRING', NULL, 'TEXT_INPUT', TRUE),
    ('primary_driver_age', 'RETAIL', 'Primary Driver Age', 'Age of primary driver', 'INTEGER', NULL, 'NUMBER_INPUT', TRUE);

-- COMMERCIAL attributes
INSERT INTO metadata_attributes (attribute_key, market_context, display_name, description, data_type, ui_component, options, is_required)
VALUES
    ('business_name', 'COMMERCIAL', 'Business Name', 'Legal name of business', 'STRING', 'TEXT_INPUT', NULL, TRUE),
    ('industry_code', 'COMMERCIAL', 'Industry Code', 'NAICS classification code', 'STRING', 'TEXT_INPUT', NULL, TRUE),
    ('business_structure', 'COMMERCIAL', 'Business Structure', 'Legal structure', 'STRING', 'DROPDOWN',
     '["SOLE_PROPRIETOR", "PARTNERSHIP", "LLC", "CORPORATION", "S_CORP"]'::jsonb, TRUE);

-- LONDON_MARKET attributes
INSERT INTO metadata_attributes (attribute_key, market_context, display_name, description, data_type, ui_component, options, is_required)
VALUES
    ('cargo_type', 'LONDON_MARKET', 'Cargo Type', 'Type of cargo being shipped', 'STRING', 'DROPDOWN',
     '["GENERAL_CARGO", "CONTAINERIZED", "BULK", "PERISHABLES", "HAZARDOUS"]'::jsonb, TRUE),
    ('vessel_name', 'LONDON_MARKET', 'Vessel Name', 'Name of vessel', 'STRING', 'TEXT_INPUT', NULL, TRUE),
    ('cargo_value', 'LONDON_MARKET', 'Cargo Value', 'Declared value of cargo', 'DECIMAL', 'NUMBER_INPUT', NULL, TRUE);

-- ============================================================================
-- Verification Query
-- ============================================================================

-- Query to verify seed data
DO $$
DECLARE
    retail_count INTEGER;
    commercial_count INTEGER;
    london_count INTEGER;
BEGIN
    SELECT count(*) INTO retail_count FROM metadata_agreement_types WHERE market_context = 'RETAIL';
    SELECT count(*) INTO commercial_count FROM metadata_agreement_types WHERE market_context = 'COMMERCIAL';
    SELECT count(*) INTO london_count FROM metadata_agreement_types WHERE market_context = 'LONDON_MARKET';

    RAISE NOTICE 'Metadata seeded successfully:';
    RAISE NOTICE '  RETAIL agreement types: %', retail_count;
    RAISE NOTICE '  COMMERCIAL agreement types: %', commercial_count;
    RAISE NOTICE '  LONDON_MARKET agreement types: %', london_count;
END $$;

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.5.0', 'Seed metadata for all market contexts', CURRENT_USER);
