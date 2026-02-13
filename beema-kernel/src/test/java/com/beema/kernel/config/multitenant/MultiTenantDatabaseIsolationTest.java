package com.beema.kernel.config.multitenant;

import com.beema.kernel.service.tenant.TenantContext;
import com.beema.kernel.service.tenant.TenantContextService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test proving physical multi-tenant database isolation.
 *
 * Spins up 3 PostgreSQL containers (master, tenant-a, tenant-b) via Testcontainers,
 * configures TenantRoutingDataSource, and verifies that data written for one tenant
 * is physically absent from the other tenant's database.
 *
 * Gracefully skips if Docker is not available.
 *
 * Test Categories:
 * 1. Tenant A Isolation - data exists in A, absent in B
 * 2. Tenant B Isolation - data exists in B, absent in A
 * 3. Connection Routing - physical connections go to different databases
 * 4. Direct JDBC Verification - raw JDBC proves physical separation
 * 5. Null/Default Tenant - falls back to master
 * 6. Performance - routing overhead is acceptable
 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiTenantDatabaseIsolationTest {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantDatabaseIsolationTest.class);

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String DEFAULT_TENANT = "default";

    @Container
    static PostgreSQLContainer<?> masterDb = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beema_master")
            .withUsername("beema")
            .withPassword("beema");

    @Container
    static PostgreSQLContainer<?> tenantADb = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beema_tenant_a")
            .withUsername("beema")
            .withPassword("beema");

    @Container
    static PostgreSQLContainer<?> tenantBDb = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beema_tenant_b")
            .withUsername("beema")
            .withPassword("beema");

    private TenantContextService tenantContextService;
    private TenantDatasourceMappingService mappingService;
    private TenantRoutingDataSource routingDataSource;

    // Direct JDBC connections for verification
    private DataSource masterDataSource;
    private DataSource tenantADataSource;
    private DataSource tenantBDataSource;

    @BeforeEach
    void setUp() {
        // Run Flyway migrations on all databases
        runMigrations(masterDb);
        runMigrations(tenantADb);
        runMigrations(tenantBDb);

        // Build properties
        TenantDatasourceProperties properties = buildProperties();

        // Create services
        tenantContextService = new TenantContextService();
        mappingService = new TenantDatasourceMappingService(properties);

        // Build individual datasources
        masterDataSource = buildHikariDataSource("master", masterDb);
        tenantADataSource = buildHikariDataSource("tenant-a-ds", tenantADb);
        tenantBDataSource = buildHikariDataSource("tenant-b-ds", tenantBDb);

        // Build routing datasource
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("tenant-a-ds", tenantADataSource);
        targetDataSources.put("tenant-b-ds", tenantBDataSource);

        routingDataSource = new TenantRoutingDataSource(tenantContextService, mappingService);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.afterPropertiesSet();

        // Clean test data
        cleanTestData(masterDataSource);
        cleanTestData(tenantADataSource);
        cleanTestData(tenantBDataSource);
    }

    @AfterEach
    void tearDown() {
        tenantContextService.clear();
    }

    // =========================================================================
    // Test 1: Tenant A Isolation
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Data created for tenant-a exists in tenant-a database only")
    void tenantA_dataExistsOnlyInTenantADatabase() throws SQLException {
        // Given: Tenant-A context is set
        setTenantContext(TENANT_A);

        // When: Insert a record via routing datasource
        String policyId = UUID.randomUUID().toString();
        insertAgreementViaRoutingDs(policyId, TENANT_A, "POL-A-001");

        // Then: Record exists in tenant-A database
        assertThat(recordExistsInDatabase(tenantADataSource, policyId))
                .as("Record should exist in tenant-A database")
                .isTrue();

        // And: Record does NOT exist in tenant-B database
        assertThat(recordExistsInDatabase(tenantBDataSource, policyId))
                .as("Record should NOT exist in tenant-B database")
                .isFalse();

        // And: Record does NOT exist in master database
        assertThat(recordExistsInDatabase(masterDataSource, policyId))
                .as("Record should NOT exist in master database")
                .isFalse();
    }

    // =========================================================================
    // Test 2: Tenant B Isolation
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Data created for tenant-b exists in tenant-b database only")
    void tenantB_dataExistsOnlyInTenantBDatabase() throws SQLException {
        // Given: Tenant-B context is set
        setTenantContext(TENANT_B);

        // When: Insert a record via routing datasource
        String policyId = UUID.randomUUID().toString();
        insertAgreementViaRoutingDs(policyId, TENANT_B, "POL-B-001");

        // Then: Record exists in tenant-B database
        assertThat(recordExistsInDatabase(tenantBDataSource, policyId))
                .as("Record should exist in tenant-B database")
                .isTrue();

        // And: Record does NOT exist in tenant-A database
        assertThat(recordExistsInDatabase(tenantADataSource, policyId))
                .as("Record should NOT exist in tenant-A database")
                .isFalse();

        // And: Record does NOT exist in master database
        assertThat(recordExistsInDatabase(masterDataSource, policyId))
                .as("Record should NOT exist in master database")
                .isFalse();
    }

    // =========================================================================
    // Test 3: Connection Routing - Different Physical Databases
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Connections route to physically different database catalogs")
    void connectionsRouteToPhysicallyDifferentDatabases() throws SQLException {
        // Get connection for tenant-A
        setTenantContext(TENANT_A);
        String tenantACatalog;
        try (Connection connA = routingDataSource.getConnection()) {
            tenantACatalog = connA.getCatalog();
            log.info("Tenant-A connection catalog: {}", tenantACatalog);
        }

        // Get connection for tenant-B
        setTenantContext(TENANT_B);
        String tenantBCatalog;
        try (Connection connB = routingDataSource.getConnection()) {
            tenantBCatalog = connB.getCatalog();
            log.info("Tenant-B connection catalog: {}", tenantBCatalog);
        }

        // Get connection for default/master
        tenantContextService.clear();
        String masterCatalog;
        try (Connection connMaster = routingDataSource.getConnection()) {
            masterCatalog = connMaster.getCatalog();
            log.info("Master connection catalog: {}", masterCatalog);
        }

        // Assert: All three catalogs are different
        assertThat(tenantACatalog).isNotEqualTo(tenantBCatalog);
        assertThat(tenantACatalog).isNotEqualTo(masterCatalog);
        assertThat(tenantBCatalog).isNotEqualTo(masterCatalog);

        // Verify expected database names
        assertThat(tenantACatalog).isEqualTo("beema_tenant_a");
        assertThat(tenantBCatalog).isEqualTo("beema_tenant_b");
        assertThat(masterCatalog).isEqualTo("beema_master");
    }

    // =========================================================================
    // Test 4: Direct JDBC Verification - Physical Separation Proof
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Direct JDBC insert in tenant-A is invisible from tenant-B JDBC")
    void directJdbcProvesPhysicalSeparation() throws SQLException {
        String policyId = UUID.randomUUID().toString();

        // Insert directly via JDBC into tenant-A's database
        try (Connection conn = tenantADataSource.getConnection()) {
            insertAgreementDirect(conn, policyId, TENANT_A, "DIRECT-A-001");
        }

        // Query directly via JDBC in tenant-B's database
        boolean existsInB;
        try (Connection conn = tenantBDataSource.getConnection()) {
            existsInB = queryAgreementExists(conn, policyId);
        }

        // Verify: absolutely no data leakage
        assertThat(existsInB)
                .as("Direct JDBC query in tenant-B should NOT find tenant-A's data")
                .isFalse();

        // Confirm it does exist in tenant-A
        boolean existsInA;
        try (Connection conn = tenantADataSource.getConnection()) {
            existsInA = queryAgreementExists(conn, policyId);
        }
        assertThat(existsInA)
                .as("Direct JDBC query in tenant-A should find its own data")
                .isTrue();
    }

    // =========================================================================
    // Test 5: Bidirectional Isolation
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Both tenants can store data simultaneously without cross-contamination")
    void bidirectionalIsolation() throws SQLException {
        String policyIdA = UUID.randomUUID().toString();
        String policyIdB = UUID.randomUUID().toString();

        // Insert for tenant-A
        setTenantContext(TENANT_A);
        insertAgreementViaRoutingDs(policyIdA, TENANT_A, "BI-A-001");

        // Insert for tenant-B
        setTenantContext(TENANT_B);
        insertAgreementViaRoutingDs(policyIdB, TENANT_B, "BI-B-001");

        // Verify tenant-A's record
        assertThat(recordExistsInDatabase(tenantADataSource, policyIdA)).isTrue();
        assertThat(recordExistsInDatabase(tenantADataSource, policyIdB)).isFalse();

        // Verify tenant-B's record
        assertThat(recordExistsInDatabase(tenantBDataSource, policyIdB)).isTrue();
        assertThat(recordExistsInDatabase(tenantBDataSource, policyIdA)).isFalse();

        // Verify master has neither
        assertThat(recordExistsInDatabase(masterDataSource, policyIdA)).isFalse();
        assertThat(recordExistsInDatabase(masterDataSource, policyIdB)).isFalse();
    }

    // =========================================================================
    // Test 6: Null Tenant Context Routes to Master
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Null tenant context routes to master database")
    void nullTenantContextRoutesToMaster() throws SQLException {
        // Given: No tenant context (cleared)
        tenantContextService.clear();

        // When: Get connection via routing datasource
        String catalog;
        try (Connection conn = routingDataSource.getConnection()) {
            catalog = conn.getCatalog();
        }

        // Then: Routes to master
        assertThat(catalog).isEqualTo("beema_master");
    }

    // =========================================================================
    // Test 7: Unknown Tenant Routes to Master (Default Fallback)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Unknown tenant ID falls back to master database")
    void unknownTenantFallsBackToMaster() throws SQLException {
        // Given: An unmapped tenant ID
        setTenantContext("unknown-tenant-xyz");

        // When: Get connection
        String catalog;
        try (Connection conn = routingDataSource.getConnection()) {
            catalog = conn.getCatalog();
        }

        // Then: Falls back to master
        assertThat(catalog).isEqualTo("beema_master");
    }

    // =========================================================================
    // Test 8: Dynamic Tenant Migration
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Tenant migration: dynamic mapping change redirects tenant to new database")
    void tenantMigrationScenario() throws SQLException {
        // Given: tenant-a initially routes to tenant-a-ds
        setTenantContext(TENANT_A);
        try (Connection conn = routingDataSource.getConnection()) {
            assertThat(conn.getCatalog()).isEqualTo("beema_tenant_a");
        }

        // When: Dynamically remap tenant-a to tenant-b's database
        mappingService.addMapping(TENANT_A, "tenant-b-ds");

        // Then: tenant-a now routes to tenant-b's database
        try (Connection conn = routingDataSource.getConnection()) {
            assertThat(conn.getCatalog()).isEqualTo("beema_tenant_b");
        }

        // Cleanup: restore original mapping
        mappingService.addMapping(TENANT_A, "tenant-a-ds");
    }

    // =========================================================================
    // Test 9: Concurrent Requests with Different Tenants
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Concurrent requests with different tenants maintain isolation")
    void concurrentTenantRequestsMaintainIsolation() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String tenantId = (i % 2 == 0) ? TENANT_A : TENANT_B;
            String expectedCatalog = (i % 2 == 0) ? "beema_tenant_a" : "beema_tenant_b";
            int threadIndex = i;

            futures.add(executor.submit(() -> {
                try {
                    // Each thread sets its own tenant context (ThreadLocal)
                    TenantContextService localService = tenantContextService;
                    localService.setContext(new TenantContext(tenantId, "user-" + threadIndex, "US"));

                    try (Connection conn = routingDataSource.getConnection()) {
                        String catalog = conn.getCatalog();
                        results.put("thread-" + threadIndex, catalog);

                        if (!expectedCatalog.equals(catalog)) {
                            results.put("error-" + threadIndex,
                                    "Expected " + expectedCatalog + " but got " + catalog);
                        }
                    }
                } catch (Exception e) {
                    results.put("error-" + threadIndex, e.getMessage());
                } finally {
                    tenantContextService.clear();
                    latch.countDown();
                }
            }));
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify no errors
        results.forEach((key, value) -> {
            if (key.startsWith("error-")) {
                Assertions.fail("Thread routing error: " + key + " = " + value);
            }
        });

        // Verify correct routing for all threads
        for (int i = 0; i < threadCount; i++) {
            String expectedCatalog = (i % 2 == 0) ? "beema_tenant_a" : "beema_tenant_b";
            assertThat(results.get("thread-" + i))
                    .as("Thread %d should route to %s", i, expectedCatalog)
                    .isEqualTo(expectedCatalog);
        }
    }

    // =========================================================================
    // Test 10: Performance - Routing Overhead
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("1000 tenant-switching operations complete within acceptable time")
    void routingPerformanceIsAcceptable() throws SQLException {
        int iterations = 1000;
        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            String tenantId = (i % 2 == 0) ? TENANT_A : TENANT_B;
            setTenantContext(tenantId);

            try (Connection conn = routingDataSource.getConnection()) {
                // Execute a simple query to validate the connection works
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    rs.next();
                }
            }
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        log.info("Performance: {} routing operations in {}ms (avg: {}ms/op)",
                iterations, elapsedMs, elapsedMs / (double) iterations);

        // Should complete well within 5 seconds
        assertThat(elapsedMs)
                .as("1000 routing operations should complete within 5000ms")
                .isLessThan(5000);
    }

    // =========================================================================
    // Test 11: Mapping Service State
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Mapping service correctly tracks all tenant mappings")
    void mappingServiceTracksAllMappings() {
        Map<String, String> allMappings = mappingService.getAllMappings();

        assertThat(allMappings).containsEntry(TENANT_A, "tenant-a-ds");
        assertThat(allMappings).containsEntry(TENANT_B, "tenant-b-ds");
        assertThat(mappingService.getDefaultDatasource()).isEqualTo("master");

        // Resolve known tenants
        assertThat(mappingService.resolveDatasource(TENANT_A)).isEqualTo("tenant-a-ds");
        assertThat(mappingService.resolveDatasource(TENANT_B)).isEqualTo("tenant-b-ds");

        // Resolve unknown tenant → default
        assertThat(mappingService.resolveDatasource("unknown")).isEqualTo("master");
        assertThat(mappingService.resolveDatasource(null)).isEqualTo("master");
    }

    // =========================================================================
    // Test 12: Remove Mapping Falls Back to Default
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Removing a tenant mapping causes fallback to master")
    void removeMappingFallsBackToDefault() throws SQLException {
        // Initially routes to tenant-a-ds
        setTenantContext(TENANT_A);
        try (Connection conn = routingDataSource.getConnection()) {
            assertThat(conn.getCatalog()).isEqualTo("beema_tenant_a");
        }

        // Remove mapping
        mappingService.removeMapping(TENANT_A);

        // Now falls back to master
        try (Connection conn = routingDataSource.getConnection()) {
            assertThat(conn.getCatalog()).isEqualTo("beema_master");
        }

        // Restore for other tests
        mappingService.addMapping(TENANT_A, "tenant-a-ds");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setTenantContext(String tenantId) {
        tenantContextService.setContext(new TenantContext(tenantId, "test-user", "US"));
    }

    private TenantDatasourceProperties buildProperties() {
        TenantDatasourceProperties props = new TenantDatasourceProperties();
        props.setEnabled(true);
        props.setDefaultDatasource("master");

        Map<String, TenantDatasourceProperties.DatasourceDefinition> datasources = new HashMap<>();
        datasources.put("master", buildDsDef(masterDb));
        datasources.put("tenant-a-ds", buildDsDef(tenantADb));
        datasources.put("tenant-b-ds", buildDsDef(tenantBDb));
        props.setDatasources(datasources);

        Map<String, String> tenantMappings = new HashMap<>();
        tenantMappings.put(TENANT_A, "tenant-a-ds");
        tenantMappings.put(TENANT_B, "tenant-b-ds");
        props.setTenantMappings(tenantMappings);

        return props;
    }

    private TenantDatasourceProperties.DatasourceDefinition buildDsDef(PostgreSQLContainer<?> container) {
        TenantDatasourceProperties.DatasourceDefinition def = new TenantDatasourceProperties.DatasourceDefinition();
        def.setUrl(container.getJdbcUrl());
        def.setUsername(container.getUsername());
        def.setPassword(container.getPassword());
        def.setDriverClassName("org.postgresql.Driver");
        def.setPoolSize(5);
        def.setMinimumIdle(1);
        return def;
    }

    private DataSource buildHikariDataSource(String name, PostgreSQLContainer<?> container) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setPoolName("Test-" + name);
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    private void runMigrations(PostgreSQLContainer<?> container) {
        Flyway flyway = Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .locations("classpath:db/migration")
                .schemas("public")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }

    private void cleanTestData(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM agreement_coverages WHERE tenant_id LIKE 'tenant-%'");
            stmt.execute("DELETE FROM agreement_parties WHERE tenant_id LIKE 'tenant-%'");
            stmt.execute("DELETE FROM agreements WHERE tenant_id LIKE 'tenant-%'");
        } catch (SQLException e) {
            log.warn("Could not clean test data: {}", e.getMessage());
        }
    }

    private void insertAgreementViaRoutingDs(String id, String tenantId, String agreementNumber)
            throws SQLException {
        try (Connection conn = routingDataSource.getConnection()) {
            insertAgreementDirect(conn, id, tenantId, agreementNumber);
        }
    }

    private void insertAgreementDirect(Connection conn, String id, String tenantId, String agreementNumber)
            throws SQLException {
        String sql = """
                INSERT INTO agreements (
                    id, valid_from, transaction_time, valid_to, is_current,
                    agreement_number, agreement_type_code, market_context,
                    status, tenant_id, data_residency_region,
                    attributes, created_by, updated_by, version
                ) VALUES (
                    ?::uuid, NOW(), NOW(), '9999-12-31T23:59:59Z'::timestamptz, true,
                    ?, 'AUTO_POLICY', 'RETAIL',
                    'DRAFT', ?, 'US',
                    '{"vehicle_make":"TestCar","vehicle_year":2024}'::jsonb,
                    'test-user', 'test-user', 0
                )
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, agreementNumber);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        }
    }

    private boolean recordExistsInDatabase(DataSource ds, String id) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return queryAgreementExists(conn, id);
        }
    }

    private boolean queryAgreementExists(Connection conn, String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM agreements WHERE id = ?::uuid";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
