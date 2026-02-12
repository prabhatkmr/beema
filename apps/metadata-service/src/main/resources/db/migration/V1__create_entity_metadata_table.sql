-- Create entity_metadata table for bitemporal metadata storage
CREATE TABLE IF NOT EXISTS entity_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(255) NOT NULL UNIQUE,
    entity_name VARCHAR(255) NOT NULL,
    description TEXT,
    schema JSONB,
    attributes JSONB,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE,
    transaction_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for common queries
CREATE INDEX idx_entity_metadata_entity_type ON entity_metadata(entity_type);
CREATE INDEX idx_entity_metadata_valid_from ON entity_metadata(valid_from);
CREATE INDEX idx_entity_metadata_valid_to ON entity_metadata(valid_to);
CREATE INDEX idx_entity_metadata_transaction_time ON entity_metadata(transaction_time);

-- Create GIN indexes for JSONB columns
CREATE INDEX idx_entity_metadata_schema ON entity_metadata USING GIN (schema);
CREATE INDEX idx_entity_metadata_attributes ON entity_metadata USING GIN (attributes);

-- Add comments
COMMENT ON TABLE entity_metadata IS 'Stores bitemporal metadata for entities in the Beema platform';
COMMENT ON COLUMN entity_metadata.valid_from IS 'Start of validity period (business time)';
COMMENT ON COLUMN entity_metadata.valid_to IS 'End of validity period (business time), NULL for current';
COMMENT ON COLUMN entity_metadata.transaction_time IS 'When the record was inserted (system time)';
COMMENT ON COLUMN entity_metadata.schema IS 'JSON schema definition for the entity';
COMMENT ON COLUMN entity_metadata.attributes IS 'Additional metadata attributes';
