package com.beema.kernel.integration;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.integration.config.TestSecurityConfig;
import com.beema.kernel.repository.agreement.AgreementRepository;
import com.beema.kernel.repository.metadata.MetadataAgreementTypeRepository;
import com.beema.kernel.service.agreement.AgreementService;
import com.beema.kernel.util.SchemaValidator;
import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = OAuth2ResourceServerAutoConfiguration.class)
@Import(TestSecurityConfig.class)
@Transactional
class AgreementServiceIntegrationTest {

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
    private AgreementService agreementService;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private MetadataAgreementTypeRepository metadataAgreementTypeRepository;

    @Autowired
    private EntityManager entityManager;

    // Fixed tenant IDs for testing
    private static final UUID TENANT_A = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    private static final String TENANT_A_STR = TENANT_A.toString();
    private static final String TENANT_B_STR = TENANT_B.toString();

    private UUID retailTypeId;
    private UUID commercialTypeId;
    private UUID londonMarketTypeId;

    @BeforeEach
    void setUp() {
        // Set tenant session variable for RLS (required for insert operations)
        setTenantSession(TENANT_A);

        // Resolve agreement type IDs from seed data
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
    }

    private void setTenantSession(UUID tenantId) {
        entityManager.createNativeQuery("SET LOCAL app.current_tenant = '" + tenantId + "'")
                .executeUpdate();
    }

    // =========================================================================
    // Helper methods to build agreements
    // =========================================================================

    private Agreement buildRetailAgreement(String agreementNumber) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setAgreementTypeId(retailTypeId);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId(TENANT_A);
        agreement.setInceptionDate(LocalDate.now());
        agreement.setExpiryDate(LocalDate.now().plusYears(1));
        agreement.setCurrencyCode("GBP");
        agreement.setTotalPremium(new BigDecimal("450.00"));
        agreement.setTotalSumInsured(new BigDecimal("25000.00"));
        agreement.setCreatedBy("test-user");
        agreement.setAttributes(Map.of(
                "vehicle_registration", "AB12 CDE",
                "vehicle_make", "Toyota",
                "vehicle_year", 2022,
                "cover_type", "COMPREHENSIVE"
        ));
        return agreement;
    }

    private Agreement buildCommercialAgreement(String agreementNumber) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setMarketContext(MarketContext.COMMERCIAL);
        agreement.setAgreementTypeId(commercialTypeId);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId(TENANT_A);
        agreement.setInceptionDate(LocalDate.now());
        agreement.setExpiryDate(LocalDate.now().plusYears(1));
        agreement.setCurrencyCode("GBP");
        agreement.setTotalPremium(new BigDecimal("15000.00"));
        agreement.setTotalSumInsured(new BigDecimal("2000000.00"));
        agreement.setCreatedBy("test-user");
        agreement.setAttributes(Map.of(
                "business_name", "Acme Corp",
                "sic_code", "62020",
                "building_value", 500000
        ));
        return agreement;
    }

    private Agreement buildLondonMarketAgreement(String agreementNumber) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setExternalReference("B1234X789");
        agreement.setMarketContext(MarketContext.LONDON_MARKET);
        agreement.setAgreementTypeId(londonMarketTypeId);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId(TENANT_A);
        agreement.setInceptionDate(LocalDate.now());
        agreement.setExpiryDate(LocalDate.now().plusYears(1));
        agreement.setCurrencyCode("USD");
        agreement.setTotalPremium(new BigDecimal("250000.00"));
        agreement.setTotalSumInsured(new BigDecimal("5000000.00"));
        agreement.setCreatedBy("test-user");
        agreement.setAttributes(Map.of(
                "umr", "B1234X789",
                "placing_basis", "OPEN_MARKET",
                "cargo_description", "Electronics",
                "cargo_value", 2000000,
                "institute_clauses", "ICC_A"
        ));
        return agreement;
    }

    // =========================================================================
    // Cross-Context Operations
    // =========================================================================

    @Nested
    @DisplayName("Cross-Context Operations")
    class CrossContextOperations {

        @Test
        @DisplayName("Should create and retrieve RETAIL agreement")
        void shouldCreateRetailAgreement() {
            Agreement agreement = buildRetailAgreement("RET-001");
            Agreement saved = agreementService.createAgreement(agreement);

            assertThat(saved.getTemporalKey()).isNotNull();
            assertThat(saved.getTemporalKey().getId()).isNotNull();
            assertThat(saved.getAgreementNumber()).isEqualTo("RET-001");
            assertThat(saved.getMarketContext()).isEqualTo(MarketContext.RETAIL);
            assertThat(saved.getStatus()).isEqualTo(AgreementStatus.DRAFT);
        }

        @Test
        @DisplayName("Should create and retrieve COMMERCIAL agreement")
        void shouldCreateCommercialAgreement() {
            Agreement agreement = buildCommercialAgreement("COM-001");
            Agreement saved = agreementService.createAgreement(agreement);

            assertThat(saved.getTemporalKey()).isNotNull();
            assertThat(saved.getMarketContext()).isEqualTo(MarketContext.COMMERCIAL);
            assertThat(saved.getAgreementNumber()).isEqualTo("COM-001");
        }

        @Test
        @DisplayName("Should create and retrieve LONDON_MARKET agreement")
        void shouldCreateLondonMarketAgreement() {
            Agreement agreement = buildLondonMarketAgreement("LM-001");
            Agreement saved = agreementService.createAgreement(agreement);

            assertThat(saved.getTemporalKey()).isNotNull();
            assertThat(saved.getMarketContext()).isEqualTo(MarketContext.LONDON_MARKET);
            assertThat(saved.getExternalReference()).isEqualTo("B1234X789");
        }

        @Test
        @DisplayName("Should list agreements filtered by market context")
        void shouldFilterByMarketContext() {
            agreementService.createAgreement(buildRetailAgreement("RET-010"));
            agreementService.createAgreement(buildRetailAgreement("RET-011"));
            agreementService.createAgreement(buildCommercialAgreement("COM-010"));
            agreementService.createAgreement(buildLondonMarketAgreement("LM-010"));

            entityManager.flush();
            entityManager.clear();

            Page<Agreement> retailPage = agreementService.getAgreementsByTenantAndContext(
                    TENANT_A_STR, MarketContext.RETAIL, PageRequest.of(0, 10));

            assertThat(retailPage.getContent()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(retailPage.getContent()).allMatch(a -> a.getMarketContext() == MarketContext.RETAIL);

            Page<Agreement> commercialPage = agreementService.getAgreementsByTenantAndContext(
                    TENANT_A_STR, MarketContext.COMMERCIAL, PageRequest.of(0, 10));

            assertThat(commercialPage.getContent()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(commercialPage.getContent()).allMatch(a -> a.getMarketContext() == MarketContext.COMMERCIAL);
        }

        @Test
        @DisplayName("Should list agreements filtered by status")
        void shouldFilterByStatus() {
            agreementService.createAgreement(buildRetailAgreement("RET-020"));

            Agreement active = buildCommercialAgreement("COM-020");
            active.setStatus(AgreementStatus.ACTIVE);
            agreementService.createAgreement(active);

            entityManager.flush();
            entityManager.clear();

            Page<Agreement> drafts = agreementService.getAgreementsByTenantAndStatus(
                    TENANT_A_STR, AgreementStatus.DRAFT, PageRequest.of(0, 10));
            assertThat(drafts.getContent()).allMatch(a -> a.getStatus() == AgreementStatus.DRAFT);

            Page<Agreement> actives = agreementService.getAgreementsByTenantAndStatus(
                    TENANT_A_STR, AgreementStatus.ACTIVE, PageRequest.of(0, 10));
            assertThat(actives.getContent()).allMatch(a -> a.getStatus() == AgreementStatus.ACTIVE);
        }
    }

    // =========================================================================
    // Bitemporal Versioning
    // =========================================================================

    @Nested
    @DisplayName("Bitemporal Versioning")
    class BitemporalVersioning {

        @Test
        @DisplayName("Should create new version when updating agreement")
        void shouldCreateNewVersionOnUpdate() {
            Agreement original = agreementService.createAgreement(buildRetailAgreement("RET-100"));
            UUID agreementId = original.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            // Build update with new values
            Agreement update = buildRetailAgreement("RET-100");
            update.setTenantId(TENANT_A);
            update.setStatus(AgreementStatus.QUOTED);
            update.setTotalPremium(new BigDecimal("500.00"));

            Agreement updated = agreementService.updateAgreement(agreementId, update);

            assertThat(updated.getTemporalKey().getId()).isEqualTo(agreementId);
            assertThat(updated.getStatus()).isEqualTo(AgreementStatus.QUOTED);
            assertThat(updated.getTotalPremium()).isEqualByComparingTo(new BigDecimal("500.00"));

            // Verify the history has multiple versions
            entityManager.flush();
            entityManager.clear();

            List<Agreement> history = agreementService.getAgreementHistory(agreementId, TENANT_A_STR);
            assertThat(history).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should retrieve agreement history in chronological order")
        void shouldRetrieveHistoryChronologically() {
            Agreement v1 = agreementService.createAgreement(buildRetailAgreement("RET-101"));
            UUID agreementId = v1.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Agreement v2Update = buildRetailAgreement("RET-101");
            v2Update.setTenantId(TENANT_A);
            v2Update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, v2Update);

            entityManager.flush();
            entityManager.clear();

            Agreement v3Update = buildRetailAgreement("RET-101");
            v3Update.setTenantId(TENANT_A);
            v3Update.setStatus(AgreementStatus.BOUND);
            agreementService.updateAgreement(agreementId, v3Update);

            entityManager.flush();
            entityManager.clear();

            List<Agreement> history = agreementService.getAgreementHistory(agreementId, TENANT_A_STR);
            assertThat(history).hasSizeGreaterThanOrEqualTo(3);

            // Verify chronological ordering by validFrom
            for (int i = 1; i < history.size(); i++) {
                OffsetDateTime prev = history.get(i - 1).getTemporalKey().getValidFrom();
                OffsetDateTime curr = history.get(i).getTemporalKey().getValidFrom();
                assertThat(prev).isBeforeOrEqualTo(curr);
            }
        }

        @Test
        @DisplayName("Should find current version after multiple updates")
        void shouldFindCurrentVersionAfterMultipleUpdates() {
            Agreement v1 = agreementService.createAgreement(buildRetailAgreement("RET-102"));
            UUID agreementId = v1.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Agreement v2Update = buildRetailAgreement("RET-102");
            v2Update.setTenantId(TENANT_A);
            v2Update.setStatus(AgreementStatus.ACTIVE);
            v2Update.setTotalPremium(new BigDecimal("600.00"));
            agreementService.updateAgreement(agreementId, v2Update);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> current = agreementRepository.findCurrentById(agreementId, TENANT_A_STR);
            assertThat(current).isPresent();
            assertThat(current.get().getStatus()).isEqualTo(AgreementStatus.ACTIVE);
            assertThat(current.get().getTotalPremium()).isEqualByComparingTo(new BigDecimal("600.00"));
        }
    }

    // =========================================================================
    // Temporal Queries
    // =========================================================================

    @Nested
    @DisplayName("Temporal Queries")
    class TemporalQueries {

        @Test
        @DisplayName("Should find agreement as-of a specific valid time")
        void shouldFindAsOfValidTime() {
            Agreement agreement = agreementService.createAgreement(buildRetailAgreement("RET-200"));
            UUID agreementId = agreement.getTemporalKey().getId();
            OffsetDateTime afterCreate = OffsetDateTime.now(ZoneOffset.UTC);

            entityManager.flush();
            entityManager.clear();

            // Query as-of now should return the agreement
            Optional<Agreement> result = agreementService.getAgreementAsOf(
                    agreementId, TENANT_A_STR, afterCreate);

            assertThat(result).isPresent();
            assertThat(result.get().getAgreementNumber()).isEqualTo("RET-200");
        }

        @Test
        @DisplayName("Should not find agreement as-of time before creation")
        void shouldNotFindAsOfTimeBeforeCreation() {
            OffsetDateTime beforeCreate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

            Agreement agreement = agreementService.createAgreement(buildRetailAgreement("RET-201"));
            UUID agreementId = agreement.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> result = agreementService.getAgreementAsOf(
                    agreementId, TENANT_A_STR, beforeCreate);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find agreement by number")
        void shouldFindByAgreementNumber() {
            agreementService.createAgreement(buildRetailAgreement("RET-202"));

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> result = agreementService.getCurrentAgreementByNumber("RET-202", TENANT_A_STR);
            assertThat(result).isPresent();
            assertThat(result.get().getAgreementNumber()).isEqualTo("RET-202");
        }

        @Test
        @DisplayName("Should return empty for non-existent agreement number")
        void shouldReturnEmptyForNonExistentNumber() {
            Optional<Agreement> result = agreementService.getCurrentAgreementByNumber("NONEXISTENT", TENANT_A_STR);
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // JSONB Attribute Queries
    // =========================================================================

    @Nested
    @DisplayName("JSONB Attribute Queries")
    class JsonbAttributeQueries {

        @Test
        @DisplayName("Should store and retrieve JSONB attributes")
        void shouldStoreAndRetrieveAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("vehicle_registration", "XY99 ZZZ");
            attributes.put("vehicle_make", "BMW");
            attributes.put("vehicle_year", 2023);
            attributes.put("cover_type", "COMPREHENSIVE");
            attributes.put("no_claims_bonus_years", 5);

            Agreement agreement = buildRetailAgreement("RET-300");
            agreement.setAttributes(attributes);
            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAttributes()).containsEntry("vehicle_registration", "XY99 ZZZ");
            assertThat(retrieved.get().getAttributes()).containsEntry("vehicle_make", "BMW");
            assertThat(retrieved.get().getAttributes()).containsEntry("no_claims_bonus_years", 5);
        }

        @Test
        @DisplayName("Should query by JSONB attribute containment")
        void shouldQueryByJsonbContainment() {
            Agreement agreement = buildRetailAgreement("RET-301");
            agreement.setAttribute("vehicle_make", "Mercedes");
            agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            List<Agreement> results = agreementRepository.findByAttribute(
                    TENANT_A_STR, "{\"vehicle_make\": \"Mercedes\"}");

            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(a -> "Mercedes".equals(a.getAttribute("vehicle_make")));
        }

        @Test
        @DisplayName("Should handle complex nested JSONB attributes")
        void shouldHandleNestedJsonbAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("business_name", "Test Corp");
            attributes.put("sic_code", "62020");
            attributes.put("building_value", 750000);
            attributes.put("property_address", Map.of(
                    "line1", "123 Test Street",
                    "city", "London",
                    "postcode", "EC1A 1BB",
                    "country", "GB"
            ));

            Agreement agreement = buildCommercialAgreement("COM-300");
            agreement.setAttributes(attributes);
            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);

            assertThat(retrieved).isPresent();
            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>) retrieved.get().getAttributes().get("property_address");
            assertThat(address).containsEntry("city", "London");
            assertThat(address).containsEntry("postcode", "EC1A 1BB");
        }
    }

    // =========================================================================
    // Tenant Isolation (RLS Verification)
    // =========================================================================

    @Nested
    @DisplayName("Tenant Isolation")
    class TenantIsolation {

        @Test
        @DisplayName("Should only return agreements for the queried tenant")
        void shouldIsolateByTenant() {
            // Create agreement for tenant A
            setTenantSession(TENANT_A);
            Agreement tenantAAgreement = buildRetailAgreement("RET-400");
            agreementService.createAgreement(tenantAAgreement);

            entityManager.flush();
            entityManager.clear();

            // Query for tenant B should not see tenant A's agreements
            Optional<Agreement> resultForTenantB = agreementService.getCurrentAgreementByNumber(
                    "RET-400", TENANT_B_STR);

            assertThat(resultForTenantB).isEmpty();

            // Query for tenant A should find the agreement
            Optional<Agreement> resultForTenantA = agreementService.getCurrentAgreementByNumber(
                    "RET-400", TENANT_A_STR);

            assertThat(resultForTenantA).isPresent();
        }

        @Test
        @DisplayName("Should prevent cross-tenant data leakage in list queries")
        void shouldPreventCrossTenantLeakageInList() {
            setTenantSession(TENANT_A);
            agreementService.createAgreement(buildRetailAgreement("RET-401"));
            agreementService.createAgreement(buildCommercialAgreement("COM-401"));

            entityManager.flush();
            entityManager.clear();

            // Tenant B should see no agreements from Tenant A
            Page<Agreement> tenantBRetail = agreementService.getAgreementsByTenantAndContext(
                    TENANT_B_STR, MarketContext.RETAIL, PageRequest.of(0, 100));

            assertThat(tenantBRetail.getContent()).isEmpty();

            // Tenant A should see their own agreements
            Page<Agreement> tenantARetail = agreementService.getAgreementsByTenantAndContext(
                    TENANT_A_STR, MarketContext.RETAIL, PageRequest.of(0, 100));

            assertThat(tenantARetail.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Should isolate agreement history by tenant")
        void shouldIsolateHistoryByTenant() {
            setTenantSession(TENANT_A);
            Agreement agreement = agreementService.createAgreement(buildRetailAgreement("RET-402"));
            UUID agreementId = agreement.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            // Tenant A should see history
            List<Agreement> tenantAHistory = agreementService.getAgreementHistory(agreementId, TENANT_A_STR);
            assertThat(tenantAHistory).isNotEmpty();

            // Tenant B should see no history for this agreement
            List<Agreement> tenantBHistory = agreementService.getAgreementHistory(agreementId, TENANT_B_STR);
            assertThat(tenantBHistory).isEmpty();
        }
    }

    // =========================================================================
    // Metadata Schema Validation
    // =========================================================================

    @Nested
    @DisplayName("Metadata Schema Validation")
    class MetadataSchemaValidation {

        @Test
        @DisplayName("Should validate agreement against metadata schema - valid attributes")
        void shouldPassValidationWithValidAttributes() {
            Agreement agreement = buildRetailAgreement("RET-500");
            SchemaValidator.ValidationResult result = agreementService.validateAgreement(agreement);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when required attributes are missing")
        void shouldFailValidationWhenRequiredAttributesMissing() {
            Agreement agreement = buildRetailAgreement("RET-501");
            // Clear all attributes - required ones are now missing
            agreement.setAttributes(Map.of());

            SchemaValidator.ValidationResult result = agreementService.validateAgreement(agreement);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should validate commercial agreement attributes")
        void shouldValidateCommercialAttributes() {
            Agreement agreement = buildCommercialAgreement("COM-500");
            SchemaValidator.ValidationResult result = agreementService.validateAgreement(agreement);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should validate London Market agreement attributes")
        void shouldValidateLondonMarketAttributes() {
            Agreement agreement = buildLondonMarketAgreement("LM-500");
            SchemaValidator.ValidationResult result = agreementService.validateAgreement(agreement);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should reject creation of agreement with invalid schema attributes")
        void shouldRejectInvalidSchemaOnCreate() {
            Agreement agreement = buildRetailAgreement("RET-502");
            agreement.setAttributes(Map.of()); // Missing required attributes

            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("schema validation");
        }

        @Test
        @DisplayName("Should skip schema validation when agreement type is null")
        void shouldSkipSchemaValidationWhenTypeIdNull() {
            Agreement agreement = buildRetailAgreement("RET-503");
            agreement.setAgreementTypeId(null);
            agreement.setAttributes(Map.of());

            SchemaValidator.ValidationResult result = agreementService.validateAgreement(agreement);
            // Market context validation may fail, but schema validation is skipped
            // Just verify no NPE occurs
            assertThat(result).isNotNull();
        }
    }

    // =========================================================================
    // Market-Context-Aware Validation
    // =========================================================================

    @Nested
    @DisplayName("Market-Context-Aware Validation")
    class MarketContextValidation {

        @Test
        @DisplayName("Should reject London Market agreement without external reference")
        void shouldRejectLondonMarketWithoutExternalReference() {
            Agreement agreement = buildLondonMarketAgreement("LM-600");
            agreement.setExternalReference(null);

            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("external reference");
        }

        @Test
        @DisplayName("Should reject London Market agreement without sum insured")
        void shouldRejectLondonMarketWithoutSumInsured() {
            Agreement agreement = buildLondonMarketAgreement("LM-601");
            agreement.setTotalSumInsured(null);

            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("sum insured");
        }

        @Test
        @DisplayName("Should reject agreement when inception date is after expiry date")
        void shouldRejectInceptionAfterExpiry() {
            Agreement agreement = buildRetailAgreement("RET-600");
            agreement.setInceptionDate(LocalDate.now().plusYears(2));
            agreement.setExpiryDate(LocalDate.now());

            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Inception date");
        }

        @Test
        @DisplayName("Should reject commercial agreement with premium exceeding sum insured")
        void shouldRejectCommercialPremiumExceedingSumInsured() {
            Agreement agreement = buildCommercialAgreement("COM-600");
            agreement.setTotalPremium(new BigDecimal("3000000.00"));
            agreement.setTotalSumInsured(new BigDecimal("2000000.00"));

            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("premium cannot exceed sum insured");
        }

        @Test
        @DisplayName("Should reject agreement without market context")
        void shouldRejectWithoutMarketContext() {
            Agreement agreement = buildRetailAgreement("RET-601");
            agreement.setMarketContext(null);

            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Market context is required");
        }

        @Test
        @DisplayName("Should accept valid retail agreement without extra rules")
        void shouldAcceptValidRetailAgreement() {
            Agreement agreement = buildRetailAgreement("RET-602");
            Agreement saved = agreementService.createAgreement(agreement);

            assertThat(saved.getTemporalKey()).isNotNull();
            assertThat(saved.getMarketContext()).isEqualTo(MarketContext.RETAIL);
        }
    }

    // =========================================================================
    // Repository Direct Queries
    // =========================================================================

    @Nested
    @DisplayName("Repository Queries")
    class RepositoryQueries {

        @Test
        @DisplayName("Should find current agreement by ID")
        void shouldFindCurrentById() {
            Agreement agreement = agreementService.createAgreement(buildRetailAgreement("RET-700"));
            UUID id = agreement.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> found = agreementRepository.findCurrentById(id, TENANT_A_STR);
            assertThat(found).isPresent();
            assertThat(found.get().getAgreementNumber()).isEqualTo("RET-700");
        }

        @Test
        @DisplayName("Should find agreement history including all versions")
        void shouldFindCompleteHistory() {
            Agreement v1 = agreementService.createAgreement(buildRetailAgreement("RET-701"));
            UUID id = v1.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Agreement v2 = buildRetailAgreement("RET-701");
            v2.setTenantId(TENANT_A);
            v2.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(id, v2);

            entityManager.flush();
            entityManager.clear();

            List<Agreement> history = agreementRepository.findHistory(id, TENANT_A_STR);
            assertThat(history).hasSizeGreaterThanOrEqualTo(2);

            // Verify one is the old version and one is current
            long currentCount = history.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCurrent()) && a.getValidTo() == null)
                    .count();
            assertThat(currentCount).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should find agreements by as-of query")
        void shouldFindByAsOfQuery() {
            Agreement agreement = agreementService.createAgreement(buildRetailAgreement("RET-702"));
            UUID id = agreement.getTemporalKey().getId();
            OffsetDateTime queryTime = OffsetDateTime.now(ZoneOffset.UTC);

            entityManager.flush();
            entityManager.clear();

            List<Agreement> asOfResults = agreementRepository.findAsOf(id, TENANT_A_STR, queryTime);
            assertThat(asOfResults).isNotEmpty();
            assertThat(asOfResults.getFirst().getAgreementNumber()).isEqualTo("RET-702");
        }

        @Test
        @DisplayName("Should page through agreements by context")
        void shouldPageByContext() {
            for (int i = 0; i < 5; i++) {
                agreementService.createAgreement(buildRetailAgreement("RET-PAGE-" + i));
            }

            entityManager.flush();
            entityManager.clear();

            Page<Agreement> page1 = agreementRepository.findAllCurrentByTenantAndContext(
                    TENANT_A_STR, MarketContext.RETAIL, PageRequest.of(0, 2));
            assertThat(page1.getContent()).hasSize(2);
            assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(5);

            Page<Agreement> page2 = agreementRepository.findAllCurrentByTenantAndContext(
                    TENANT_A_STR, MarketContext.RETAIL, PageRequest.of(1, 2));
            assertThat(page2.getContent()).hasSize(2);

            // Verify no overlap between pages
            assertThat(page1.getContent())
                    .extracting(a -> a.getTemporalKey().getId())
                    .doesNotContainAnyElementsOf(
                            page2.getContent().stream()
                                    .map(a -> a.getTemporalKey().getId())
                                    .toList()
                    );
        }
    }
}
