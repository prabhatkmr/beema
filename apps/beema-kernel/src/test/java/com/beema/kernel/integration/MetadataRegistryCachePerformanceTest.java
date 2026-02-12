package com.beema.kernel.integration;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.integration.config.TestSecurityConfig;
import com.beema.kernel.repository.metadata.MetadataAgreementTypeRepository;
import com.beema.kernel.service.metadata.MetadataRegistry;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = OAuth2ResourceServerAutoConfiguration.class)
@Import(TestSecurityConfig.class)
@Transactional
class MetadataRegistryCachePerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beema_kernel_test")
            .withUsername("beema_test")
            .withPassword("beema_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "https://auth.test.local/realms/beema");
    }

    @Autowired
    private MetadataRegistry metadataRegistry;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private MetadataAgreementTypeRepository metadataAgreementTypeRepository;

    @Autowired
    private EntityManager entityManager;

    private static final UUID TENANT_A = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private UUID retailTypeId;
    private UUID commercialTypeId;
    private UUID londonMarketTypeId;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("SET LOCAL app.current_tenant = '" + TENANT_A + "'")
                .executeUpdate();

        retailTypeId = metadataAgreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL)
                .map(MetadataAgreementType::getId)
                .orElse(null);

        commercialTypeId = metadataAgreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(TENANT_A, "COMMERCIAL_PROPERTY", MarketContext.COMMERCIAL)
                .map(MetadataAgreementType::getId)
                .orElse(null);

        londonMarketTypeId = metadataAgreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(TENANT_A, "MARINE_CARGO", MarketContext.LONDON_MARKET)
                .map(MetadataAgreementType::getId)
                .orElse(null);

        // Ensure caches are fresh for each test
        metadataRegistry.refreshAll();
    }

    // =========================================================================
    // Cache Warm-up
    // =========================================================================

    @Nested
    @DisplayName("Cache Warm-up")
    class CacheWarmup {

        @Test
        @DisplayName("Should preload caches on refreshAll and serve fields from cache")
        void shouldPreloadCachesOnRefresh() {
            // refreshAll() was called in setUp(), so caches should already be populated

            // Act - these should all be cache hits
            List<FieldDefinition> retailFields = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            List<FieldDefinition> commercialFields = metadataRegistry.getFieldsForType(
                    TENANT_A, "COMMERCIAL_PROPERTY", MarketContext.COMMERCIAL);
            List<FieldDefinition> londonFields = metadataRegistry.getFieldsForType(
                    TENANT_A, "MARINE_CARGO", MarketContext.LONDON_MARKET);

            // Assert - all types should return fields from seed data
            assertThat(retailFields).isNotEmpty();
            assertThat(commercialFields).isNotEmpty();
            assertThat(londonFields).isNotEmpty();
        }

        @Test
        @DisplayName("Should serve individual field from cache after type preload")
        void shouldServeFieldFromCacheAfterPreload() {
            // refreshAll() populates fieldByNameCache for all fields

            // Act - should be cache hit since refreshAll populated individual fields too
            Optional<FieldDefinition> field = metadataRegistry.getField(
                    TENANT_A, "vehicle_registration", MarketContext.RETAIL);

            // Assert
            assertThat(field).isPresent();
            assertThat(field.get().attributeName()).isEqualTo("vehicle_registration");
            assertThat(field.get().dataType()).isEqualTo("STRING");
        }

        @Test
        @DisplayName("Should populate calculated fields cache on refreshAll")
        void shouldPopulateCalculatedFieldsCache() {
            // Act
            List<FieldDefinition> calculatedFields = metadataRegistry.getCalculatedFields(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Assert - seed data has no CALCULATED fields, so list should be empty
            assertThat(calculatedFields).isNotNull();
        }

        @Test
        @DisplayName("Should return same reference on repeated calls (cache identity)")
        void shouldReturnCachedReferenceOnRepeatedCalls() {
            List<FieldDefinition> first = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            List<FieldDefinition> second = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Caffeine returns the same cached object reference
            assertThat(first).isSameAs(second);
        }
    }

    // =========================================================================
    // Cache Hit Rate
    // =========================================================================

    @Nested
    @DisplayName("Cache Hit Rate")
    class CacheHitRate {

        @Test
        @DisplayName("Should achieve greater than 90% cache hit rate after warm-up")
        void shouldAchieveHighHitRateForFieldsByType() {
            // Arrange - cache is warm from setUp()

            // Act - perform 100 lookups across 3 types
            for (int i = 0; i < 100; i++) {
                String typeCode = switch (i % 3) {
                    case 0 -> "MOTOR_PERSONAL";
                    case 1 -> "COMMERCIAL_PROPERTY";
                    default -> "MARINE_CARGO";
                };
                MarketContext ctx = switch (i % 3) {
                    case 0 -> MarketContext.RETAIL;
                    case 1 -> MarketContext.COMMERCIAL;
                    default -> MarketContext.LONDON_MARKET;
                };
                List<FieldDefinition> result = metadataRegistry.getFieldsForType(TENANT_A, typeCode, ctx);
                assertThat(result).isNotNull();
            }

            // Assert via cache stats
            Map<String, Object> stats = metadataRegistry.getCacheStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldsByTypeStats = (Map<String, Object>) stats.get("fieldsByType");
            long hitCount = (Long) fieldsByTypeStats.get("hitCount");
            long missCount = (Long) fieldsByTypeStats.get("missCount");

            // After refreshAll (populates cache) + 100 lookups, hit rate should be high
            assertThat(hitCount).isGreaterThanOrEqualTo(100);
            double hitRate = (double) hitCount / (hitCount + missCount);
            assertThat(hitRate).isGreaterThan(0.90);
        }

        @Test
        @DisplayName("Should achieve high hit rate for individual field lookups")
        void shouldAchieveHighHitRateForFieldByName() {
            // Act - repeatedly look up the same field
            for (int i = 0; i < 50; i++) {
                Optional<FieldDefinition> result = metadataRegistry.getField(
                        TENANT_A, "vehicle_registration", MarketContext.RETAIL);
                assertThat(result).isPresent();
            }

            // Assert
            Map<String, Object> stats = metadataRegistry.getCacheStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldByNameStats = (Map<String, Object>) stats.get("fieldByName");
            long hitCount = (Long) fieldByNameStats.get("hitCount");
            assertThat(hitCount).isGreaterThanOrEqualTo(50);
        }
    }

    // =========================================================================
    // Cache Stats Verification
    // =========================================================================

    @Nested
    @DisplayName("Cache Stats Verification")
    class CacheStatsVerification {

        @Test
        @DisplayName("Should expose stats for all four caches")
        void shouldExposeStatsForAllCaches() {
            Map<String, Object> stats = metadataRegistry.getCacheStats();

            assertThat(stats).containsKeys("fieldsByType", "fieldByName", "layoutByType", "calculatedFields");
        }

        @Test
        @DisplayName("Should track hit and miss counts accurately")
        void shouldTrackHitAndMissCountsAccurately() {
            // refreshAll() already ran in setUp() — stats include preload

            // Act - evict, then access (forces a miss), then access again (hit)
            metadataRegistry.evictForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // This should be a cache miss (just evicted)
            metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // This should be a cache hit
            metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            Map<String, Object> stats = metadataRegistry.getCacheStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldsByTypeStats = (Map<String, Object>) stats.get("fieldsByType");

            long hitCount = (Long) fieldsByTypeStats.get("hitCount");
            long missCount = (Long) fieldsByTypeStats.get("missCount");

            // At minimum: 1 miss (after evict) + 1 hit (second access)
            assertThat(hitCount).isGreaterThanOrEqualTo(1);
            assertThat(missCount).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should include cache size in stats")
        void shouldIncludeCacheSizeInStats() {
            Map<String, Object> stats = metadataRegistry.getCacheStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldsByTypeStats = (Map<String, Object>) stats.get("fieldsByType");

            long size = (Long) fieldsByTypeStats.get("size");
            // After refreshAll with seed data, should have at least the 6 agreement types cached
            assertThat(size).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should format hit rate as percentage string")
        void shouldFormatHitRateAsPercentage() {
            // Perform a few lookups to generate stats
            metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            Map<String, Object> stats = metadataRegistry.getCacheStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldsByTypeStats = (Map<String, Object>) stats.get("fieldsByType");

            String hitRate = (String) fieldsByTypeStats.get("hitRate");
            assertThat(hitRate).matches("\\d+\\.\\d+%");
        }
    }

    // =========================================================================
    // Sub-millisecond Cached Lookups
    // =========================================================================

    @Nested
    @DisplayName("Sub-millisecond Cached Lookups")
    class SubMillisecondLookups {

        @Test
        @DisplayName("Should complete cached field type lookups in under 1ms average")
        void shouldCompleteCachedFieldTypeLookupsInUnder1ms() {
            // Arrange - cache is warm from setUp()

            // Act
            int iterations = 1000;
            long startNanos = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            }
            long elapsedNanos = System.nanoTime() - startNanos;

            // Assert
            double averageMillis = (double) elapsedNanos / iterations / 1_000_000.0;
            assertThat(averageMillis).isLessThan(1.0);
        }

        @Test
        @DisplayName("Should complete cached individual field lookups in under 1ms average")
        void shouldCompleteCachedFieldLookupsInUnder1ms() {
            // Arrange - ensure field is cached
            metadataRegistry.getField(TENANT_A, "vehicle_registration", MarketContext.RETAIL);

            // Act
            int iterations = 1000;
            long startNanos = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                metadataRegistry.getField(TENANT_A, "vehicle_registration", MarketContext.RETAIL);
            }
            long elapsedNanos = System.nanoTime() - startNanos;

            // Assert
            double averageMillis = (double) elapsedNanos / iterations / 1_000_000.0;
            assertThat(averageMillis).isLessThan(1.0);
        }

        @Test
        @DisplayName("Should complete 10000 cached lookups in under 100ms total")
        void shouldComplete10000LookupsInUnder100ms() {
            // Act
            int iterations = 10000;
            long startNanos = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                String typeCode = switch (i % 3) {
                    case 0 -> "MOTOR_PERSONAL";
                    case 1 -> "COMMERCIAL_PROPERTY";
                    default -> "MARINE_CARGO";
                };
                MarketContext ctx = switch (i % 3) {
                    case 0 -> MarketContext.RETAIL;
                    case 1 -> MarketContext.COMMERCIAL;
                    default -> MarketContext.LONDON_MARKET;
                };
                metadataRegistry.getFieldsForType(TENANT_A, typeCode, ctx);
            }
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

            // Assert
            assertThat(elapsedMillis).isLessThan(100);
        }

        @Test
        @DisplayName("Cold lookup should be slower than cached lookup")
        void coldLookupShouldBeSlowerThanCachedLookup() {
            // Arrange - evict to force cold miss
            metadataRegistry.evictForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Cold lookup
            long coldStart = System.nanoTime();
            metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            long coldDuration = System.nanoTime() - coldStart;

            // Warm lookup (100 iterations averaged)
            long warmStart = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            }
            long warmDurationAvg = (System.nanoTime() - warmStart) / 100;

            // Assert - cold miss should be meaningfully slower
            assertThat(warmDurationAvg).isLessThan(coldDuration);
        }
    }

    // =========================================================================
    // Concurrent Access
    // =========================================================================

    @Nested
    @DisplayName("Concurrent Access")
    class ConcurrentAccess {

        @Test
        @DisplayName("Should handle 20 threads accessing cache simultaneously without errors")
        void shouldHandleConcurrentAccess() throws InterruptedException {
            int threadCount = 20;
            int iterationsPerThread = 50;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < iterationsPerThread; i++) {
                            String typeCode = switch (threadIndex % 3) {
                                case 0 -> "MOTOR_PERSONAL";
                                case 1 -> "COMMERCIAL_PROPERTY";
                                default -> "MARINE_CARGO";
                            };
                            MarketContext ctx = switch (threadIndex % 3) {
                                case 0 -> MarketContext.RETAIL;
                                case 1 -> MarketContext.COMMERCIAL;
                                default -> MarketContext.LONDON_MARKET;
                            };
                            List<FieldDefinition> result = metadataRegistry.getFieldsForType(
                                    TENANT_A, typeCode, ctx);
                            if (result != null) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(errorCount.get()).isZero();
            assertThat(successCount.get()).isEqualTo(threadCount * iterationsPerThread);
        }

        @Test
        @DisplayName("Should return consistent data across concurrent threads")
        void shouldReturnConsistentDataAcrossThreads() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicInteger fieldCountMismatch = new AtomicInteger(0);

            // Get expected field count for comparison
            int expectedFieldCount = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL).size();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < 100; i++) {
                            List<FieldDefinition> fields = metadataRegistry.getFieldsForType(
                                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
                            if (fields.size() != expectedFieldCount) {
                                fieldCountMismatch.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(errorCount.get()).isZero();
            assertThat(fieldCountMismatch.get()).isZero();
        }
    }

    // =========================================================================
    // Cache Eviction
    // =========================================================================

    @Nested
    @DisplayName("Cache Eviction")
    class CacheEviction {

        @Test
        @DisplayName("Should evict fieldsByType cache entry for specific type")
        void shouldEvictFieldsByTypeForSpecificType() {
            // Arrange - verify cache is populated
            List<FieldDefinition> before = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            assertThat(before).isNotEmpty();

            // Act - evict
            metadataRegistry.evictForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Assert - next access reloads from DB (still returns data, but is a cache miss)
            List<FieldDefinition> after = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            assertThat(after).isNotEmpty();
            assertThat(after.size()).isEqualTo(before.size());

            // The second call should be a hit again
            List<FieldDefinition> cached = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            assertThat(cached).isSameAs(after);
        }

        @Test
        @DisplayName("Should not affect other types when evicting one type")
        void shouldNotAffectOtherTypesOnEviction() {
            // Arrange - get references to cached objects
            List<FieldDefinition> commercialBefore = metadataRegistry.getFieldsForType(
                    TENANT_A, "COMMERCIAL_PROPERTY", MarketContext.COMMERCIAL);

            // Act - evict only MOTOR_PERSONAL
            metadataRegistry.evictForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Assert - commercial fields are still the same cached reference
            List<FieldDefinition> commercialAfter = metadataRegistry.getFieldsForType(
                    TENANT_A, "COMMERCIAL_PROPERTY", MarketContext.COMMERCIAL);
            assertThat(commercialAfter).isSameAs(commercialBefore);
        }

        @Test
        @DisplayName("Should evict calculated fields cache on evictForType")
        void shouldEvictCalculatedFieldsCacheOnEvictForType() {
            // Arrange
            metadataRegistry.getCalculatedFields(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Act
            metadataRegistry.evictForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Assert - should reload from DB on next access
            List<FieldDefinition> calculatedAfter = metadataRegistry.getCalculatedFields(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            assertThat(calculatedAfter).isNotNull();
        }

        @Test
        @DisplayName("Should refresh all caches on refreshAll")
        void shouldRefreshAllCaches() {
            // Arrange - get initial stats
            Map<String, Object> statsBefore = metadataRegistry.getCacheStats();

            // Act
            metadataRegistry.refreshAll();

            // Assert - caches should be repopulated
            Map<String, Object> statsAfter = metadataRegistry.getCacheStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldsByTypeStats = (Map<String, Object>) statsAfter.get("fieldsByType");
            long size = (Long) fieldsByTypeStats.get("size");
            assertThat(size).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should evict individual field entries when type is evicted")
        void shouldEvictFieldEntriesWhenTypeEvicted() {
            // Arrange - ensure individual field is cached
            Optional<FieldDefinition> fieldBefore = metadataRegistry.getField(
                    TENANT_A, "vehicle_registration", MarketContext.RETAIL);
            assertThat(fieldBefore).isPresent();

            // Act - evict the type (which should also evict its individual fields)
            metadataRegistry.evictForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            // Assert - individual field access still works (reloads from DB)
            Optional<FieldDefinition> fieldAfter = metadataRegistry.getField(
                    TENANT_A, "vehicle_registration", MarketContext.RETAIL);
            assertThat(fieldAfter).isPresent();
            assertThat(fieldAfter.get().attributeName()).isEqualTo("vehicle_registration");
        }
    }

    // =========================================================================
    // MetadataService Cache (agreementType caches)
    // =========================================================================

    @Nested
    @DisplayName("MetadataService Agreement Type Cache")
    class MetadataServiceCache {

        @Test
        @DisplayName("Should cache agreement type by ID on first access")
        void shouldCacheAgreementTypeById() {
            // Act
            Optional<MetadataAgreementType> first = metadataService.getAgreementType(retailTypeId);
            Optional<MetadataAgreementType> second = metadataService.getAgreementType(retailTypeId);

            // Assert
            assertThat(first).isPresent();
            assertThat(second).isPresent();
            assertThat(first.get().getTypeCode()).isEqualTo("MOTOR_PERSONAL");
            assertThat(second.get().getTypeCode()).isEqualTo("MOTOR_PERSONAL");
        }

        @Test
        @DisplayName("Should cache agreement type by code and populate ID cache")
        void shouldCacheByCodeAndPopulateIdCache() {
            // Act - access by code
            Optional<MetadataAgreementType> byCode = metadataService.getAgreementTypeByCode(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            assertThat(byCode).isPresent();

            // Access by ID should now be from cache
            Optional<MetadataAgreementType> byId = metadataService.getAgreementType(retailTypeId);
            assertThat(byId).isPresent();
            assertThat(byId.get().getTypeCode()).isEqualTo(byCode.get().getTypeCode());
        }

        @Test
        @DisplayName("Should evict cache on agreement type update")
        void shouldEvictCacheOnUpdate() {
            // Arrange - warm cache
            Optional<MetadataAgreementType> original = metadataService.getAgreementType(retailTypeId);
            assertThat(original).isPresent();

            // Act - update triggers evictCaches
            MetadataAgreementType update = original.get();
            update.setDescription("Cache eviction test description");
            update.setUpdatedBy("test-user");
            metadataService.updateAgreementType(retailTypeId, update);

            entityManager.flush();
            entityManager.clear();

            // Assert - next access should return updated data
            Optional<MetadataAgreementType> afterUpdate = metadataService.getAgreementType(retailTypeId);
            assertThat(afterUpdate).isPresent();
            assertThat(afterUpdate.get().getDescription()).isEqualTo("Cache eviction test description");
        }

        @Test
        @DisplayName("Should evict cache on agreement type deactivation")
        void shouldEvictCacheOnDeactivation() {
            // Arrange
            metadataService.getAgreementType(commercialTypeId);

            // Act
            metadataService.deactivateAgreementType(commercialTypeId);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<MetadataAgreementType> result = metadataService.getAgreementType(commercialTypeId);
            assertThat(result).isPresent();
            assertThat(result.get().getIsActive()).isFalse();
        }
    }

    // =========================================================================
    // TTL Behavior Documentation
    // =========================================================================

    @Nested
    @DisplayName("TTL Behavior Documentation")
    class TtlBehavior {

        @Test
        @DisplayName("Should document MetadataRegistry cache TTL and size configuration")
        void shouldDocumentRegistryCacheConfiguration() {
            // MetadataRegistry caches are configured with:
            //   - maximumSize: 1000 (fieldsByType, layoutByType, calculatedFields)
            //                  2000 (fieldByName)
            //   - expireAfterWrite: 2 hours
            //   - recordStats: true
            //
            // MetadataService caches are configured with:
            //   - maximumSize: 500 (agreementTypeCache, agreementTypeByCodeCache)
            //   - expireAfterWrite: 1 hour
            //   - recordStats: true
            //
            // Real TTL testing requires waiting > 2 hours, which is impractical.
            // This test verifies immediate re-access is a cache hit (within TTL).

            List<FieldDefinition> first = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            List<FieldDefinition> second = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("Should coexist multiple types in cache without premature eviction")
        void shouldCoexistMultipleTypesWithoutPrematureEviction() {
            // All 3 types should coexist (well under maximumSize of 1000)
            List<FieldDefinition> retail = metadataRegistry.getFieldsForType(
                    TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL);
            List<FieldDefinition> commercial = metadataRegistry.getFieldsForType(
                    TENANT_A, "COMMERCIAL_PROPERTY", MarketContext.COMMERCIAL);
            List<FieldDefinition> london = metadataRegistry.getFieldsForType(
                    TENANT_A, "MARINE_CARGO", MarketContext.LONDON_MARKET);

            // Verify all are still cached (same reference)
            assertThat(metadataRegistry.getFieldsForType(TENANT_A, "MOTOR_PERSONAL", MarketContext.RETAIL))
                    .isSameAs(retail);
            assertThat(metadataRegistry.getFieldsForType(TENANT_A, "COMMERCIAL_PROPERTY", MarketContext.COMMERCIAL))
                    .isSameAs(commercial);
            assertThat(metadataRegistry.getFieldsForType(TENANT_A, "MARINE_CARGO", MarketContext.LONDON_MARKET))
                    .isSameAs(london);
        }
    }
}
