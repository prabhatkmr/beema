-- V5: Row-Level Security (RLS)
--
-- Implements multi-tenancy isolation using PostgreSQL Row-Level Security.
-- Each table gets a policy enforcing: WHERE tenant_id = current_setting('app.current_tenant')

-- ============================================================================
-- Enable Row-Level Security on all tables
-- ============================================================================

ALTER TABLE agreements ENABLE ROW LEVEL SECURITY;
ALTER TABLE agreement_parties ENABLE ROW LEVEL SECURITY;
ALTER TABLE agreement_coverages ENABLE ROW LEVEL SECURITY;
ALTER TABLE metadata_agreement_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE metadata_attributes ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- Create RLS Policies
-- ============================================================================

-- Function to get current tenant from session variable
-- Application sets this via: SET LOCAL app.current_tenant = 'tenant-123';
CREATE OR REPLACE FUNCTION get_current_tenant()
RETURNS VARCHAR AS $$
BEGIN
    RETURN current_setting('app.current_tenant', TRUE);
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================================================
-- AGREEMENTS - Tenant isolation
-- ============================================================================

-- Policy: Users can only see/modify their tenant's data
CREATE POLICY tenant_isolation_agreements ON agreements
    USING (tenant_id = get_current_tenant())
    WITH CHECK (tenant_id = get_current_tenant());

-- Superuser bypass policy (for admin operations)
CREATE POLICY admin_all_agreements ON agreements
    TO beema
    USING (true)
    WITH CHECK (true);

-- ============================================================================
-- AGREEMENT_PARTIES - Tenant isolation
-- ============================================================================

CREATE POLICY tenant_isolation_agreement_parties ON agreement_parties
    USING (tenant_id = get_current_tenant())
    WITH CHECK (tenant_id = get_current_tenant());

CREATE POLICY admin_all_agreement_parties ON agreement_parties
    TO beema
    USING (true)
    WITH CHECK (true);

-- ============================================================================
-- AGREEMENT_COVERAGES - Tenant isolation
-- ============================================================================

CREATE POLICY tenant_isolation_agreement_coverages ON agreement_coverages
    USING (tenant_id = get_current_tenant())
    WITH CHECK (tenant_id = get_current_tenant());

CREATE POLICY admin_all_agreement_coverages ON agreement_coverages
    TO beema
    USING (true)
    WITH CHECK (true);

-- ============================================================================
-- METADATA - Shared across all tenants (no tenant_id column)
-- ============================================================================
-- Metadata is global and shared. No tenant isolation needed.
-- However, we still enable RLS and create permissive policies.

-- Disable RLS for metadata tables (they're shared resources)
ALTER TABLE metadata_agreement_types DISABLE ROW LEVEL SECURITY;
ALTER TABLE metadata_attributes DISABLE ROW LEVEL SECURITY;

-- Alternative approach if we want fine-grained metadata access:
-- We could add tenant_id to metadata tables and allow multi-tenant access
-- or implement read-only policies. For now, metadata is global.

-- ============================================================================
-- Helper Functions for Application Layer
-- ============================================================================

-- Function to set tenant context (called by application on each request)
CREATE OR REPLACE FUNCTION set_tenant_context(tenant VARCHAR)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_tenant', tenant, TRUE);  -- TRUE = local to transaction
END;
$$ LANGUAGE plpgsql;

-- Function to clear tenant context (cleanup)
CREATE OR REPLACE FUNCTION clear_tenant_context()
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_tenant', '', TRUE);
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Verification Views
-- ============================================================================

-- View to check RLS status
CREATE OR REPLACE VIEW v_rls_status AS
SELECT
    schemaname,
    tablename,
    rowsecurity AS rls_enabled,
    (SELECT count(*) FROM pg_policies WHERE tablename = t.tablename) AS policy_count
FROM pg_tables t
WHERE schemaname = 'public'
  AND tablename IN ('agreements', 'agreement_parties', 'agreement_coverages',
                    'metadata_agreement_types', 'metadata_attributes')
ORDER BY tablename;

-- View to list all RLS policies
CREATE OR REPLACE VIEW v_rls_policies AS
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual AS using_expression,
    with_check AS with_check_expression
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;

-- ============================================================================
-- Testing Functions (for development/testing)
-- ============================================================================

-- Test function: Simulate different tenant contexts
CREATE OR REPLACE FUNCTION test_tenant_isolation(test_tenant VARCHAR)
RETURNS TABLE (
    current_tenant VARCHAR,
    agreements_count BIGINT,
    parties_count BIGINT,
    coverages_count BIGINT
) AS $$
BEGIN
    -- Set tenant context
    PERFORM set_tenant_context(test_tenant);

    -- Count visible records
    RETURN QUERY
    SELECT
        get_current_tenant() AS current_tenant,
        (SELECT count(*) FROM agreements WHERE is_current = TRUE) AS agreements_count,
        (SELECT count(*) FROM agreement_parties WHERE is_current = TRUE) AS parties_count,
        (SELECT count(*) FROM agreement_coverages WHERE is_current = TRUE) AS coverages_count;

    -- Clear context
    PERFORM clear_tenant_context();
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON FUNCTION get_current_tenant() IS 'Returns tenant_id from session variable app.current_tenant';
COMMENT ON FUNCTION set_tenant_context(VARCHAR) IS 'Sets tenant context for RLS (call at start of request)';
COMMENT ON FUNCTION clear_tenant_context() IS 'Clears tenant context (call at end of request)';
COMMENT ON FUNCTION test_tenant_isolation(VARCHAR) IS 'Test function to verify tenant isolation';
COMMENT ON VIEW v_rls_status IS 'Shows RLS status for all tables';
COMMENT ON VIEW v_rls_policies IS 'Lists all RLS policies';

-- ============================================================================
-- Grant Permissions
-- ============================================================================

-- Grant execute on helper functions to application role
-- GRANT EXECUTE ON FUNCTION set_tenant_context(VARCHAR) TO beema_app_role;
-- GRANT EXECUTE ON FUNCTION clear_tenant_context() TO beema_app_role;
-- GRANT EXECUTE ON FUNCTION get_current_tenant() TO beema_app_role;

-- Update schema version
INSERT INTO schema_version (version, description, applied_by)
VALUES ('1.4.0', 'Row-Level Security for multi-tenancy', CURRENT_USER);
