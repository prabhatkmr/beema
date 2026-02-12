-- =============================================================================
-- V5__enable_row_level_security.sql
-- Beema Unified Platform - Row-Level Security for Tenant Isolation
-- =============================================================================
-- Implements PostgreSQL Row-Level Security (RLS) to enforce tenant isolation
-- at the database level. This provides defense-in-depth: even if the
-- application layer has a bug, one tenant's data cannot leak to another.
--
-- STRATEGY:
-- 1. The application sets a session variable: SET app.current_tenant = '<uuid>'
-- 2. RLS policies filter all SELECT/INSERT/UPDATE/DELETE by tenant_id
-- 3. The Flyway migration user (superuser) bypasses RLS for migrations
-- 4. The application database user has RLS enforced
--
-- NOTE: RLS is enforced per-table. Tables without tenant_id (if any) are
-- excluded. Superusers bypass RLS by default.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Helper function: get current tenant from session
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION get_current_tenant_id()
RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.current_tenant', TRUE)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_current_tenant_id() IS
    'Returns the current tenant UUID from the session variable app.current_tenant. Returns NULL if not set.';

-- ---------------------------------------------------------------------------
-- 1. agreements - RLS
-- ---------------------------------------------------------------------------
ALTER TABLE agreements ENABLE ROW LEVEL SECURITY;
ALTER TABLE agreements FORCE ROW LEVEL SECURITY;

CREATE POLICY agreements_tenant_isolation_select ON agreements
    FOR SELECT
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY agreements_tenant_isolation_insert ON agreements
    FOR INSERT
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY agreements_tenant_isolation_update ON agreements
    FOR UPDATE
    USING (tenant_id = get_current_tenant_id())
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY agreements_tenant_isolation_delete ON agreements
    FOR DELETE
    USING (tenant_id = get_current_tenant_id());

-- ---------------------------------------------------------------------------
-- 2. agreement_parties - RLS
-- ---------------------------------------------------------------------------
ALTER TABLE agreement_parties ENABLE ROW LEVEL SECURITY;
ALTER TABLE agreement_parties FORCE ROW LEVEL SECURITY;

CREATE POLICY agreement_parties_tenant_isolation_select ON agreement_parties
    FOR SELECT
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY agreement_parties_tenant_isolation_insert ON agreement_parties
    FOR INSERT
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY agreement_parties_tenant_isolation_update ON agreement_parties
    FOR UPDATE
    USING (tenant_id = get_current_tenant_id())
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY agreement_parties_tenant_isolation_delete ON agreement_parties
    FOR DELETE
    USING (tenant_id = get_current_tenant_id());

-- ---------------------------------------------------------------------------
-- 3. agreement_coverages - RLS
-- ---------------------------------------------------------------------------
ALTER TABLE agreement_coverages ENABLE ROW LEVEL SECURITY;
ALTER TABLE agreement_coverages FORCE ROW LEVEL SECURITY;

CREATE POLICY agreement_coverages_tenant_isolation_select ON agreement_coverages
    FOR SELECT
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY agreement_coverages_tenant_isolation_insert ON agreement_coverages
    FOR INSERT
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY agreement_coverages_tenant_isolation_update ON agreement_coverages
    FOR UPDATE
    USING (tenant_id = get_current_tenant_id())
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY agreement_coverages_tenant_isolation_delete ON agreement_coverages
    FOR DELETE
    USING (tenant_id = get_current_tenant_id());

-- ---------------------------------------------------------------------------
-- 4. metadata_agreement_types - RLS
-- ---------------------------------------------------------------------------
ALTER TABLE metadata_agreement_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE metadata_agreement_types FORCE ROW LEVEL SECURITY;

CREATE POLICY meta_agreement_types_tenant_isolation_select ON metadata_agreement_types
    FOR SELECT
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY meta_agreement_types_tenant_isolation_insert ON metadata_agreement_types
    FOR INSERT
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY meta_agreement_types_tenant_isolation_update ON metadata_agreement_types
    FOR UPDATE
    USING (tenant_id = get_current_tenant_id())
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY meta_agreement_types_tenant_isolation_delete ON metadata_agreement_types
    FOR DELETE
    USING (tenant_id = get_current_tenant_id());

-- ---------------------------------------------------------------------------
-- 5. metadata_attributes - RLS
-- ---------------------------------------------------------------------------
ALTER TABLE metadata_attributes ENABLE ROW LEVEL SECURITY;
ALTER TABLE metadata_attributes FORCE ROW LEVEL SECURITY;

CREATE POLICY meta_attributes_tenant_isolation_select ON metadata_attributes
    FOR SELECT
    USING (tenant_id = get_current_tenant_id());

CREATE POLICY meta_attributes_tenant_isolation_insert ON metadata_attributes
    FOR INSERT
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY meta_attributes_tenant_isolation_update ON metadata_attributes
    FOR UPDATE
    USING (tenant_id = get_current_tenant_id())
    WITH CHECK (tenant_id = get_current_tenant_id());

CREATE POLICY meta_attributes_tenant_isolation_delete ON metadata_attributes
    FOR DELETE
    USING (tenant_id = get_current_tenant_id());

-- ---------------------------------------------------------------------------
-- Usage Instructions (for application configuration)
-- ---------------------------------------------------------------------------
-- In the Spring Boot application, before any tenant-scoped query, execute:
--
--   SET LOCAL app.current_tenant = '<tenant-uuid>';
--
-- SET LOCAL scopes the setting to the current transaction only.
-- This is typically done in a TenantContextFilter or AOP interceptor.
--
-- For the Flyway migration user (superuser), RLS is bypassed automatically.
-- For the application database user, ensure it is NOT a superuser so that
-- RLS policies are enforced.
