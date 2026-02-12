-- =====================================================
-- Beema Kernel: Workflow Hooks Schema
-- =====================================================
-- Temporal Workflow DSL Configuration
-- Stores workflow hooks that define triggers, conditions, and actions
-- for event-driven workflows in the insurance platform

-- Create workflow_hooks table
CREATE TABLE sys_workflow_hooks (
    hook_id BIGSERIAL PRIMARY KEY,
    hook_name VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    trigger_condition TEXT NOT NULL, -- JEXL expression for condition evaluation
    action_type VARCHAR(100) NOT NULL, -- webhook, snapshot, custom_logic
    action_config JSONB NOT NULL, -- Configuration for the action
    execution_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Create indexes
CREATE INDEX idx_workflow_hooks_event_type ON sys_workflow_hooks(event_type);
CREATE INDEX idx_workflow_hooks_enabled ON sys_workflow_hooks(enabled);
CREATE INDEX idx_workflow_hooks_execution_order ON sys_workflow_hooks(execution_order);

-- Create workflow execution results table
CREATE TABLE sys_workflow_executions (
    execution_id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    agreement_id BIGINT,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    input_data JSONB,
    result_data JSONB,
    status VARCHAR(50) NOT NULL, -- RUNNING, COMPLETED, FAILED
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT unique_workflow_run UNIQUE (workflow_id, run_id)
);

-- Create indexes for workflow executions
CREATE INDEX idx_workflow_executions_workflow_id ON sys_workflow_executions(workflow_id);
CREATE INDEX idx_workflow_executions_agreement_id ON sys_workflow_executions(agreement_id);
CREATE INDEX idx_workflow_executions_status ON sys_workflow_executions(status);
CREATE INDEX idx_workflow_executions_event_type ON sys_workflow_executions(event_type);

-- =====================================================
-- Sample Workflow Hooks
-- =====================================================

-- Hook 1: Capture policy snapshot when agreement is created
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order,
    enabled,
    description,
    created_by
) VALUES (
    'capture_snapshot_on_agreement_created',
    'agreement.created',
    'agreement != null && agreement.agreementType != null',
    'snapshot',
    '{
        "endpoint": "/mock-policy-api/snapshots",
        "method": "POST",
        "timeout": 5000,
        "retryPolicy": {
            "maxAttempts": 3,
            "backoffMultiplier": 2.0
        }
    }'::jsonb,
    10,
    true,
    'Captures a policy snapshot when a new agreement is created',
    'system'
);

-- Hook 2: Send webhook notification for high-value agreements
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order,
    enabled,
    description,
    created_by
) VALUES (
    'notify_high_value_agreement',
    'agreement.created',
    'agreement.premiumAmount != null && agreement.premiumAmount > 100000',
    'webhook',
    '{
        "url": "https://webhook.site/high-value-notification",
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "X-Beema-Event": "high-value-agreement"
        },
        "timeout": 10000,
        "retryPolicy": {
            "maxAttempts": 3,
            "backoffMultiplier": 2.0
        }
    }'::jsonb,
    20,
    true,
    'Sends webhook notification for high-value agreements (> £100,000)',
    'system'
);

-- Hook 3: Evaluate risk score on agreement update
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order,
    enabled,
    description,
    created_by
) VALUES (
    'evaluate_risk_on_update',
    'agreement.updated',
    'agreement.status == "PENDING_REVIEW"',
    'expression',
    '{
        "expression": "agreement.riskFactors.size() * 10 + (agreement.premiumAmount / 1000)",
        "resultField": "calculatedRiskScore",
        "description": "Calculate risk score based on risk factors and premium amount"
    }'::jsonb,
    15,
    true,
    'Evaluates risk score when agreement status changes to PENDING_REVIEW',
    'system'
);

-- Hook 4: Webhook for London Market placement
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order,
    enabled,
    description,
    created_by
) VALUES (
    'london_market_placement_notification',
    'agreement.created',
    'agreement.marketType == "LONDON_MARKET" && agreement.placementType == "DIRECT"',
    'webhook',
    '{
        "url": "https://webhook.site/london-market-placement",
        "method": "POST",
        "headers": {
            "Content-Type": "application/json",
            "X-Market-Type": "LONDON_MARKET"
        },
        "payload": {
            "agreementId": "${agreement.agreementId}",
            "placementType": "${agreement.placementType}",
            "leadUnderwriter": "${agreement.leadUnderwriter}"
        },
        "timeout": 15000
    }'::jsonb,
    25,
    true,
    'Notifies external system of London Market placements',
    'system'
);

-- Hook 5: Commercial policy validation
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order,
    enabled,
    description,
    created_by
) VALUES (
    'validate_commercial_policy',
    'agreement.created',
    'agreement.lineOfBusiness == "COMMERCIAL" && agreement.premiumAmount > 50000',
    'expression',
    '{
        "expression": "agreement.coverageLimit >= agreement.premiumAmount * 10",
        "resultField": "isValidCoverageRatio",
        "description": "Validates that coverage limit is at least 10x premium for commercial policies"
    }'::jsonb,
    5,
    true,
    'Validates coverage ratio for commercial policies',
    'system'
);

-- Hook 6: Capture snapshot on agreement endorsement
INSERT INTO sys_workflow_hooks (
    hook_name,
    event_type,
    trigger_condition,
    action_type,
    action_config,
    execution_order,
    enabled,
    description,
    created_by
) VALUES (
    'capture_snapshot_on_endorsement',
    'agreement.endorsed',
    'agreement != null && endorsement != null',
    'snapshot',
    '{
        "endpoint": "/mock-policy-api/snapshots",
        "method": "POST",
        "includeEndorsement": true,
        "timeout": 5000,
        "retryPolicy": {
            "maxAttempts": 3,
            "backoffMultiplier": 2.0
        }
    }'::jsonb,
    30,
    true,
    'Captures policy snapshot when an endorsement is applied',
    'system'
);

-- Create trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_workflow_hooks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_workflow_hooks_updated_at
    BEFORE UPDATE ON sys_workflow_hooks
    FOR EACH ROW
    EXECUTE FUNCTION update_workflow_hooks_updated_at();

-- Add comments
COMMENT ON TABLE sys_workflow_hooks IS 'Stores workflow hook configurations for Temporal workflows';
COMMENT ON COLUMN sys_workflow_hooks.hook_name IS 'Unique identifier for the hook';
COMMENT ON COLUMN sys_workflow_hooks.event_type IS 'Event that triggers this hook (e.g., agreement.created, agreement.updated)';
COMMENT ON COLUMN sys_workflow_hooks.trigger_condition IS 'JEXL expression to evaluate whether action should be executed';
COMMENT ON COLUMN sys_workflow_hooks.action_type IS 'Type of action: webhook, snapshot, expression, custom_logic';
COMMENT ON COLUMN sys_workflow_hooks.action_config IS 'JSON configuration for the action execution';
COMMENT ON COLUMN sys_workflow_hooks.execution_order IS 'Order in which hooks are executed (lower numbers run first)';

COMMENT ON TABLE sys_workflow_executions IS 'Stores workflow execution history and results';
COMMENT ON COLUMN sys_workflow_executions.workflow_id IS 'Temporal workflow ID';
COMMENT ON COLUMN sys_workflow_executions.run_id IS 'Temporal run ID';
