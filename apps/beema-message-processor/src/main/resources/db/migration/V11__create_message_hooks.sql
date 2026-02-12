-- Message Hooks Table
-- Stores JEXL transformation rules for converting external messages to Beema's internal format

CREATE TABLE IF NOT EXISTS sys_message_hooks (
    hook_id BIGSERIAL PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL,
    message_type VARCHAR(100) NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    jexl_transform TEXT NOT NULL,
    field_mapping JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INT NOT NULL DEFAULT 100,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Ensure unique hook per message type and source
    CONSTRAINT uk_message_hooks_type_source UNIQUE (message_type, source_system, hook_name)
);

-- Index for efficient lookup during message processing
CREATE INDEX idx_message_hooks_lookup ON sys_message_hooks(message_type, source_system, enabled);
CREATE INDEX idx_message_hooks_enabled ON sys_message_hooks(enabled);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_message_hook_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_message_hook_timestamp
    BEFORE UPDATE ON sys_message_hooks
    FOR EACH ROW
    EXECUTE FUNCTION update_message_hook_timestamp();

-- Sample hooks for common message types

-- 1. Policy Created from Legacy System
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping, description, created_by)
VALUES (
    'legacy_policy_transform',
    'policy_created',
    'legacy_system',
    'message.policy.premium * 1.0',
    '{
        "policy_number": {"source": "policyRef", "jexl": "message.policyRef.toUpperCase()"},
        "policy_holder_name": {"source": "customerName", "jexl": "message.customer.firstName + '' '' + message.customer.lastName"},
        "premium_amount": {"source": "premium", "jexl": "message.policy.premium"},
        "currency": {"source": "currency", "jexl": "message.policy.currency != null ? message.policy.currency : ''GBP''"},
        "effective_date": {"source": "effectiveDate", "jexl": "message.policy.effectiveDate"},
        "expiry_date": {"source": "expiryDate", "jexl": "message.policy.expiryDate"},
        "product_code": {"source": "productCode", "jexl": "message.policy.productCode"},
        "status": {"source": "status", "jexl": "message.policy.status"}
    }'::jsonb,
    'Transforms legacy system policy creation messages to Beema internal format',
    'system'
);

-- 2. Claim Submitted from Partner API
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping, description, created_by)
VALUES (
    'partner_claim_transform',
    'claim_submitted',
    'partner_api',
    'message.claim.amount * 1.0',
    '{
        "claim_number": {"source": "claimId", "jexl": "message.claimId"},
        "policy_number": {"source": "policyNumber", "jexl": "message.policyNumber"},
        "claim_amount": {"source": "amount", "jexl": "message.claim.amount"},
        "claim_date": {"source": "submittedDate", "jexl": "message.claim.submittedDate"},
        "incident_date": {"source": "incidentDate", "jexl": "message.claim.incidentDate"},
        "claim_type": {"source": "type", "jexl": "message.claim.type"},
        "description": {"source": "description", "jexl": "message.claim.description"},
        "claimant_name": {"source": "claimantName", "jexl": "message.claimant.name"},
        "status": {"source": "status", "jexl": "''SUBMITTED''"}
    }'::jsonb,
    'Transforms partner API claim submissions to Beema internal format',
    'system'
);

-- 3. Premium Payment from Payment Gateway
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping, description, created_by)
VALUES (
    'payment_gateway_transform',
    'payment_received',
    'payment_gateway',
    'message.payment.amount',
    '{
        "transaction_id": {"source": "transactionId", "jexl": "message.transactionId"},
        "policy_number": {"source": "policyNumber", "jexl": "message.policyNumber"},
        "payment_amount": {"source": "amount", "jexl": "message.payment.amount"},
        "payment_date": {"source": "paymentDate", "jexl": "message.payment.timestamp"},
        "payment_method": {"source": "method", "jexl": "message.payment.method"},
        "currency": {"source": "currency", "jexl": "message.payment.currency"},
        "status": {"source": "status", "jexl": "message.payment.status == ''SUCCESS'' ? ''COMPLETED'' : ''PENDING''"}
    }'::jsonb,
    'Transforms payment gateway messages to Beema internal format',
    'system'
);

-- 4. Quote Request from Web Portal
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping, description, created_by)
VALUES (
    'web_quote_transform',
    'quote_requested',
    'web_portal',
    'message.coverage.sumInsured * message.risk.ratingFactor',
    '{
        "quote_reference": {"source": "quoteRef", "jexl": "message.quoteRef"},
        "product_code": {"source": "productCode", "jexl": "message.product.code"},
        "sum_insured": {"source": "sumInsured", "jexl": "message.coverage.sumInsured"},
        "risk_rating": {"source": "riskRating", "jexl": "message.risk.ratingFactor"},
        "customer_email": {"source": "email", "jexl": "message.customer.email.toLowerCase()"},
        "customer_phone": {"source": "phone", "jexl": "message.customer.phone"},
        "quote_date": {"source": "requestDate", "jexl": "message.requestDate"},
        "valid_until": {"source": "validUntil", "jexl": "message.validUntil"},
        "status": {"source": "status", "jexl": "''PENDING''"}
    }'::jsonb,
    'Transforms web portal quote requests to Beema internal format',
    'system'
);

-- 5. London Market Slip Creation
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping, description, created_by, priority)
VALUES (
    'london_market_slip_transform',
    'slip_created',
    'london_market',
    'message.slip.totalPremium',
    '{
        "slip_number": {"source": "slipNumber", "jexl": "message.slipNumber"},
        "umr": {"source": "umr", "jexl": "message.umr"},
        "lead_underwriter": {"source": "leadUnderwriter", "jexl": "message.slip.leadUnderwriter"},
        "total_premium": {"source": "totalPremium", "jexl": "message.slip.totalPremium"},
        "total_line": {"source": "totalLine", "jexl": "message.slip.totalLine"},
        "currency": {"source": "currency", "jexl": "message.slip.currency"},
        "inception_date": {"source": "inceptionDate", "jexl": "message.slip.inceptionDate"},
        "expiry_date": {"source": "expiryDate", "jexl": "message.slip.expiryDate"},
        "risk_code": {"source": "riskCode", "jexl": "message.slip.riskCode"},
        "broker": {"source": "broker", "jexl": "message.broker.name"},
        "status": {"source": "status", "jexl": "message.slip.status"}
    }'::jsonb,
    'Transforms London Market slip creation messages to Beema internal format (Unified Platform)',
    'system',
    50
);

-- 6. Commercial Policy Endorsement
INSERT INTO sys_message_hooks (hook_name, message_type, source_system, jexl_transform, field_mapping, description, created_by)
VALUES (
    'commercial_endorsement_transform',
    'endorsement_issued',
    'commercial_system',
    'message.endorsement.premiumAdjustment',
    '{
        "endorsement_number": {"source": "endorsementNumber", "jexl": "message.endorsementNumber"},
        "policy_number": {"source": "policyNumber", "jexl": "message.policyNumber"},
        "effective_date": {"source": "effectiveDate", "jexl": "message.endorsement.effectiveDate"},
        "premium_adjustment": {"source": "premiumAdjustment", "jexl": "message.endorsement.premiumAdjustment"},
        "reason": {"source": "reason", "jexl": "message.endorsement.reason"},
        "description": {"source": "description", "jexl": "message.endorsement.description"},
        "status": {"source": "status", "jexl": "''ACTIVE''"}
    }'::jsonb,
    'Transforms commercial policy endorsement messages to Beema internal format',
    'system'
);

-- Comment explaining the schema design
COMMENT ON TABLE sys_message_hooks IS 'JEXL-based message transformation hooks for converting external messages to Beema internal format. Supports Retail, Commercial, and London Market contexts.';
COMMENT ON COLUMN sys_message_hooks.jexl_transform IS 'Main JEXL expression for validation or calculation (e.g., premium * 1.05)';
COMMENT ON COLUMN sys_message_hooks.field_mapping IS 'JSONB mapping of internal fields to JEXL expressions for transformation';
COMMENT ON COLUMN sys_message_hooks.priority IS 'Lower values = higher priority. Used when multiple hooks match the same message type/source';
