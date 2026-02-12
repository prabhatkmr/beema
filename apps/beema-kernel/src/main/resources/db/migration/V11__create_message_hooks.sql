-- =====================================================
-- Beema Kernel: Message Hooks Schema
-- =====================================================
-- Message transformation hooks with pre/post processing
-- Supports JEXL scripts for flexible message transformation pipeline
-- Includes validation, enrichment, transformation, and post-processing

-- Create sys_message_hooks table
CREATE TABLE sys_message_hooks (
    hook_id BIGSERIAL PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL UNIQUE,
    message_type VARCHAR(100) NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    target_system VARCHAR(100),

    -- Pre-processing: validation, normalization, enrichment
    preprocessing_jexl TEXT,
    preprocessing_order INTEGER DEFAULT 0,

    -- Main transformation: field mapping and conversion
    transformation_jexl TEXT NOT NULL,
    transformation_order INTEGER DEFAULT 100,

    -- Post-processing: calculated fields, audit, notifications
    postprocessing_jexl TEXT,
    postprocessing_order INTEGER DEFAULT 200,

    -- Error handling configuration
    error_handling_strategy VARCHAR(50) NOT NULL DEFAULT 'fail_fast',
    retry_config JSONB DEFAULT '{"maxAttempts": 3, "backoffMs": 1000, "backoffMultiplier": 2.0}'::jsonb,

    -- Metadata
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Status and audit
    enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Constraints
    CONSTRAINT chk_error_handling CHECK (error_handling_strategy IN ('fail_fast', 'log_continue', 'retry')),
    CONSTRAINT chk_message_type_source UNIQUE (message_type, source_system, hook_name)
);

-- Create indexes for efficient queries
CREATE INDEX idx_message_hooks_type_source ON sys_message_hooks(message_type, source_system);
CREATE INDEX idx_message_hooks_preprocessing_order ON sys_message_hooks(preprocessing_order) WHERE preprocessing_jexl IS NOT NULL;
CREATE INDEX idx_message_hooks_postprocessing_order ON sys_message_hooks(postprocessing_order) WHERE postprocessing_jexl IS NOT NULL;
CREATE INDEX idx_message_hooks_enabled ON sys_message_hooks(enabled);

-- Create message processing executions table for audit trail
CREATE TABLE sys_message_processing_executions (
    execution_id BIGSERIAL PRIMARY KEY,
    hook_id BIGINT NOT NULL REFERENCES sys_message_hooks(hook_id),
    message_type VARCHAR(100) NOT NULL,
    source_system VARCHAR(100) NOT NULL,

    -- Processing stage: preprocessing, transformation, postprocessing
    processing_stage VARCHAR(50) NOT NULL,

    -- Input/Output data
    input_data JSONB,
    output_data JSONB,

    -- Execution result
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILED, RETRYING
    error_message TEXT,
    error_stacktrace TEXT,

    -- Timing
    execution_time_ms INTEGER,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    -- Retry tracking
    attempt_number INTEGER DEFAULT 1,
    max_attempts INTEGER,

    CONSTRAINT chk_processing_stage CHECK (processing_stage IN ('preprocessing', 'transformation', 'postprocessing')),
    CONSTRAINT chk_execution_status CHECK (status IN ('SUCCESS', 'FAILED', 'RETRYING'))
);

-- Create indexes for execution queries
CREATE INDEX idx_message_executions_hook_id ON sys_message_processing_executions(hook_id);
CREATE INDEX idx_message_executions_status ON sys_message_processing_executions(status);
CREATE INDEX idx_message_executions_message_type ON sys_message_processing_executions(message_type);
CREATE INDEX idx_message_executions_started_at ON sys_message_processing_executions(started_at);

-- Create trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_message_hooks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_message_hooks_updated_at
    BEFORE UPDATE ON sys_message_hooks
    FOR EACH ROW
    EXECUTE FUNCTION update_message_hooks_updated_at();

-- =====================================================
-- Sample Message Hooks
-- =====================================================

-- Hook 1: Retail Policy Creation - Full Pipeline
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    target_system,
    preprocessing_jexl,
    preprocessing_order,
    transformation_jexl,
    transformation_order,
    postprocessing_jexl,
    postprocessing_order,
    error_handling_strategy,
    retry_config,
    metadata,
    enabled,
    description,
    created_by
) VALUES (
    'retail_policy_creation',
    'policy.created',
    'retail_system',
    'beema_core',
    -- Pre-processing: validation and normalization
    'if (message.policyNumber == null || message.policyNumber.trim().isEmpty()) {
        throw "Missing required field: policyNumber";
    }
    if (message.customerName == null || message.customerName.trim().isEmpty()) {
        throw "Missing required field: customerName";
    }
    message.policyNumber = message.policyNumber.trim().toUpperCase();
    message.customerName = message.customerName.trim();
    message.email = message.email != null ? message.email.trim().toLowerCase() : null;
    message.normalizedAt = new("java.time.Instant").now().toString();',
    10,
    -- Main transformation: field mapping
    '{
        "agreementId": message.policyNumber,
        "agreementType": "POLICY",
        "marketContext": "RETAIL",
        "customer": {
            "name": message.customerName,
            "email": message.email,
            "phone": message.phone
        },
        "coverage": {
            "type": message.coverageType,
            "limit": message.coverageLimit,
            "deductible": message.deductible
        },
        "premium": {
            "amount": message.premiumAmount,
            "currency": message.currency != null ? message.currency : "GBP",
            "frequency": message.paymentFrequency != null ? message.paymentFrequency : "ANNUAL"
        },
        "effectiveDate": message.effectiveDate,
        "expiryDate": message.expiryDate
    }',
    100,
    -- Post-processing: calculated fields and audit
    'result.processedAt = new("java.time.Instant").now().toString();
    result.processedBy = "beema-kernel";
    result.sourceSystem = "retail_system";

    // Calculate annual premium if not provided
    if (result.premium.frequency == "MONTHLY") {
        result.premium.annualAmount = result.premium.amount * 12;
    } else if (result.premium.frequency == "QUARTERLY") {
        result.premium.annualAmount = result.premium.amount * 4;
    } else {
        result.premium.annualAmount = result.premium.amount;
    }

    // Add validation metadata
    result.validation = {
        "validated": true,
        "validatedAt": new("java.time.Instant").now().toString()
    };',
    200,
    'retry',
    '{"maxAttempts": 3, "backoffMs": 1000, "backoffMultiplier": 2.0}'::jsonb,
    '{"market": "retail", "priority": "high", "sla": "2s"}'::jsonb,
    true,
    'Full pipeline for retail policy creation with validation, transformation, and enrichment',
    'system'
);

-- Hook 2: Commercial Policy - High Value Validation
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    target_system,
    preprocessing_jexl,
    preprocessing_order,
    transformation_jexl,
    transformation_order,
    postprocessing_jexl,
    postprocessing_order,
    error_handling_strategy,
    retry_config,
    metadata,
    enabled,
    description,
    created_by
) VALUES (
    'commercial_policy_high_value',
    'policy.created',
    'commercial_system',
    'beema_core',
    -- Pre-processing: enhanced validation for commercial
    'if (message.policyNumber == null) {
        throw "Missing policyNumber";
    }
    if (message.premiumAmount == null || message.premiumAmount <= 0) {
        throw "Invalid premium amount";
    }
    if (message.premiumAmount > 100000) {
        // High value policy - require additional fields
        if (message.underwriterApproval == null) {
            throw "High value policy requires underwriter approval";
        }
        if (message.riskAssessment == null) {
            throw "High value policy requires risk assessment";
        }
    }
    message.policyNumber = message.policyNumber.trim().toUpperCase();
    message.validated = true;',
    10,
    -- Main transformation
    '{
        "agreementId": message.policyNumber,
        "agreementType": "POLICY",
        "marketContext": "COMMERCIAL",
        "lineOfBusiness": message.lineOfBusiness,
        "premium": {
            "amount": message.premiumAmount,
            "currency": "GBP"
        },
        "underwriter": {
            "id": message.underwriterId,
            "approval": message.underwriterApproval,
            "approvalDate": message.approvalDate
        },
        "riskScore": message.riskAssessment.score,
        "riskFactors": message.riskAssessment.factors
    }',
    100,
    -- Post-processing: notifications for high value
    'result.processedAt = new("java.time.Instant").now().toString();

    if (result.premium.amount > 100000) {
        result.notifications = [{
            "type": "high_value_policy",
            "recipient": "underwriting_team",
            "priority": "high",
            "message": "New high-value commercial policy created: " + result.agreementId
        }];
    }

    // Add risk categorization
    if (result.riskScore < 30) {
        result.riskCategory = "LOW";
    } else if (result.riskScore < 70) {
        result.riskCategory = "MEDIUM";
    } else {
        result.riskCategory = "HIGH";
    }',
    200,
    'fail_fast',
    '{"maxAttempts": 1}'::jsonb,
    '{"market": "commercial", "requiresApproval": true}'::jsonb,
    true,
    'Commercial policy with enhanced validation and high-value notifications',
    'system'
);

-- Hook 3: London Market Placement
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    target_system,
    preprocessing_jexl,
    preprocessing_order,
    transformation_jexl,
    transformation_order,
    postprocessing_jexl,
    postprocessing_order,
    error_handling_strategy,
    retry_config,
    metadata,
    enabled,
    description,
    created_by
) VALUES (
    'london_market_placement',
    'placement.submitted',
    'london_market_system',
    'beema_core',
    -- Pre-processing: London Market specific validation
    'if (message.umr == null || message.umr.trim().isEmpty()) {
        throw "Missing Unique Market Reference (UMR)";
    }
    if (message.placementType == null) {
        throw "Missing placement type";
    }
    if (message.leadUnderwriter == null) {
        throw "Missing lead underwriter";
    }
    message.umr = message.umr.trim().toUpperCase();
    message.marketType = "LONDON_MARKET";',
    10,
    -- Main transformation: London Market structure
    '{
        "agreementId": message.umr,
        "agreementType": "PLACEMENT",
        "marketContext": "LONDON_MARKET",
        "placement": {
            "umr": message.umr,
            "placementType": message.placementType,
            "leadUnderwriter": message.leadUnderwriter,
            "followingMarket": message.followingMarket
        },
        "syndicate": {
            "number": message.syndicateNumber,
            "share": message.syndicateShare
        },
        "coverage": {
            "class": message.classOfBusiness,
            "limit": message.coverageLimit,
            "attachment": message.attachmentPoint
        },
        "premium": {
            "amount": message.premiumAmount,
            "currency": message.currency != null ? message.currency : "USD"
        }
    }',
    100,
    -- Post-processing: London Market notifications
    'result.processedAt = new("java.time.Instant").now().toString();
    result.processedBy = "beema-kernel";

    // Add London Market specific metadata
    result.marketMetadata = {
        "submittedToLloyds": true,
        "lloydsReference": "LLO-" + result.placement.umr,
        "submissionDate": new("java.time.Instant").now().toString()
    };

    // Notify relevant parties
    result.notifications = [{
        "type": "placement_submitted",
        "recipient": "london_market_team",
        "reference": result.placement.umr,
        "leadUnderwriter": result.placement.leadUnderwriter
    }];',
    200,
    'retry',
    '{"maxAttempts": 5, "backoffMs": 2000, "backoffMultiplier": 2.0}'::jsonb,
    '{"market": "london", "priority": "critical", "requiresLloydsSync": true}'::jsonb,
    true,
    'London Market placement processing with UMR validation and Lloyd''s notifications',
    'system'
);

-- Hook 4: Data Enrichment - Product Lookup
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    target_system,
    preprocessing_jexl,
    preprocessing_order,
    transformation_jexl,
    transformation_order,
    postprocessing_jexl,
    postprocessing_order,
    error_handling_strategy,
    retry_config,
    metadata,
    enabled,
    description,
    created_by
) VALUES (
    'product_enrichment',
    'policy.created',
    'any',
    'beema_core',
    -- Pre-processing: product code validation and enrichment
    'if (message.productCode == null) {
        throw "Missing product code";
    }
    message.productCode = message.productCode.trim().toUpperCase();

    // Simulate product lookup (in real system, this would be an external call)
    var productDb = {
        "HOME-001": {"name": "Home Insurance Standard", "category": "PROPERTY", "basePremium": 500},
        "AUTO-001": {"name": "Auto Insurance Standard", "category": "MOTOR", "basePremium": 800},
        "COMM-001": {"name": "Commercial Property", "category": "COMMERCIAL", "basePremium": 5000}
    };

    var product = productDb[message.productCode];
    if (product == null) {
        throw "Invalid product code: " + message.productCode;
    }

    message.productDetails = product;
    message.enriched = true;',
    10,
    -- Main transformation
    '{
        "agreementId": message.policyNumber,
        "product": {
            "code": message.productCode,
            "name": message.productDetails.name,
            "category": message.productDetails.category,
            "basePremium": message.productDetails.basePremium
        },
        "customer": message.customer,
        "premium": {
            "amount": message.premiumAmount != null ? message.premiumAmount : message.productDetails.basePremium
        }
    }',
    100,
    -- Post-processing
    'result.enrichmentMetadata = {
        "enriched": true,
        "enrichedAt": new("java.time.Instant").now().toString(),
        "enrichmentSource": "product_database"
    };',
    200,
    'log_continue',
    '{"maxAttempts": 2, "backoffMs": 500}'::jsonb,
    '{"enrichmentType": "product_lookup"}'::jsonb,
    true,
    'Product enrichment hook that validates and enriches messages with product details',
    'system'
);

-- Hook 5: Error Logging - Audit Trail
INSERT INTO sys_message_hooks (
    hook_name,
    message_type,
    source_system,
    target_system,
    preprocessing_jexl,
    preprocessing_order,
    transformation_jexl,
    transformation_order,
    postprocessing_jexl,
    postprocessing_order,
    error_handling_strategy,
    retry_config,
    metadata,
    enabled,
    description,
    created_by
) VALUES (
    'audit_trail_logger',
    'any',
    'any',
    'audit_system',
    NULL,
    NULL,
    -- Main transformation: pass-through with audit
    'message',
    100,
    -- Post-processing: comprehensive audit logging
    'result.audit = {
        "timestamp": new("java.time.Instant").now().toString(),
        "processor": "beema-kernel",
        "messageType": context.messageType,
        "sourceSystem": context.sourceSystem,
        "processingDuration": context.executionTime,
        "messageHash": new("java.lang.String", new("java.lang.String", message).getBytes()).hashCode()
    };

    result.auditTrail = {
        "received": context.receivedAt,
        "processed": new("java.time.Instant").now().toString(),
        "status": "SUCCESS"
    };',
    200,
    'log_continue',
    '{"maxAttempts": 1}'::jsonb,
    '{"purpose": "audit", "compliance": true}'::jsonb,
    true,
    'Audit trail logger that captures processing metadata for compliance',
    'system'
);

-- Add comments for documentation
COMMENT ON TABLE sys_message_hooks IS 'Message transformation hooks with pre-processing, transformation, and post-processing JEXL scripts';
COMMENT ON COLUMN sys_message_hooks.preprocessing_jexl IS 'JEXL script for pre-processing: validation, normalization, enrichment';
COMMENT ON COLUMN sys_message_hooks.transformation_jexl IS 'JEXL script for main transformation: field mapping and conversion';
COMMENT ON COLUMN sys_message_hooks.postprocessing_jexl IS 'JEXL script for post-processing: calculated fields, audit, notifications';
COMMENT ON COLUMN sys_message_hooks.error_handling_strategy IS 'Error handling: fail_fast (stop on error), log_continue (log and continue), retry (retry with backoff)';
COMMENT ON COLUMN sys_message_hooks.retry_config IS 'JSON configuration for retry policy: maxAttempts, backoffMs, backoffMultiplier';
COMMENT ON COLUMN sys_message_hooks.metadata IS 'Additional metadata for hook configuration and categorization';

COMMENT ON TABLE sys_message_processing_executions IS 'Audit trail of message processing executions';
COMMENT ON COLUMN sys_message_processing_executions.processing_stage IS 'Stage of processing: preprocessing, transformation, postprocessing';
COMMENT ON COLUMN sys_message_processing_executions.execution_time_ms IS 'Execution time in milliseconds';
