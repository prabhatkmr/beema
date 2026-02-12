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
class VirtualFieldCalculationIntegrationTest {

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

    private static final UUID TENANT_A = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final String TENANT_A_STR = TENANT_A.toString();

    private UUID retailTypeId;
    private UUID commercialTypeId;
    private UUID londonMarketTypeId;

    @BeforeEach
    void setUp() {
        setTenantSession(TENANT_A);

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

        // Set up calculation rules on MOTOR_PERSONAL: TotalPremium = rate * limit
        setupCalculationRules(retailTypeId,
                "[{\"targetField\":\"TotalPremium\",\"expression\":\"rate * limit\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1,\"description\":\"Calculate total premium from rate and limit\"}]");

        // Set up calculation rules on COMMERCIAL_PROPERTY: TotalPremium = rate * limit
        setupCalculationRules(commercialTypeId,
                "[{\"targetField\":\"TotalPremium\",\"expression\":\"rate * limit\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1,\"description\":\"Calculate total premium from rate and limit\"}]");

        // Set up calculation rules on MARINE_CARGO: TotalPremium = rate * limit
        setupCalculationRules(londonMarketTypeId,
                "[{\"targetField\":\"TotalPremium\",\"expression\":\"rate * limit\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1,\"description\":\"Calculate total premium from rate and limit\"}]");
    }

    private void setTenantSession(UUID tenantId) {
        entityManager.createNativeQuery("SET LOCAL app.current_tenant = '" + tenantId + "'")
                .executeUpdate();
    }

    private void setupCalculationRules(UUID typeId, String rulesJson) {
        if (typeId != null) {
            entityManager.createNativeQuery(
                    "UPDATE metadata_agreement_types SET calculation_rules = :rules::JSONB WHERE id = :id")
                    .setParameter("rules", rulesJson)
                    .setParameter("id", typeId)
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }
    }

    // =========================================================================
    // Helper methods to build agreements with rate/limit for virtual fields
    // =========================================================================

    private Agreement buildRetailAgreementWithRateAndLimit(
            String agreementNumber, BigDecimal rate, BigDecimal limit) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setAgreementTypeId(retailTypeId);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId(TENANT_A);
        agreement.setInceptionDate(LocalDate.now());
        agreement.setExpiryDate(LocalDate.now().plusYears(1));
        agreement.setCurrencyCode("GBP");
        agreement.setTotalPremium(new BigDecimal("0.00"));
        agreement.setTotalSumInsured(new BigDecimal("25000.00"));
        agreement.setCreatedBy("test-user");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("vehicle_registration", "AB12 CDE");
        attributes.put("vehicle_make", "Toyota");
        attributes.put("vehicle_year", 2022);
        attributes.put("cover_type", "COMPREHENSIVE");
        if (rate != null) attributes.put("rate", rate);
        if (limit != null) attributes.put("limit", limit);
        agreement.setAttributes(attributes);
        return agreement;
    }

    private Agreement buildCommercialAgreementWithRateAndLimit(
            String agreementNumber, BigDecimal rate, BigDecimal limit) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setMarketContext(MarketContext.COMMERCIAL);
        agreement.setAgreementTypeId(commercialTypeId);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId(TENANT_A);
        agreement.setInceptionDate(LocalDate.now());
        agreement.setExpiryDate(LocalDate.now().plusYears(1));
        agreement.setCurrencyCode("GBP");
        agreement.setTotalPremium(new BigDecimal("0.00"));
        agreement.setTotalSumInsured(new BigDecimal("2000000.00"));
        agreement.setCreatedBy("test-user");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("business_name", "Acme Corp");
        attributes.put("sic_code", "62020");
        attributes.put("building_value", 500000);
        if (rate != null) attributes.put("rate", rate);
        if (limit != null) attributes.put("limit", limit);
        agreement.setAttributes(attributes);
        return agreement;
    }

    private Agreement buildLondonMarketAgreementWithRateAndLimit(
            String agreementNumber, BigDecimal rate, BigDecimal limit) {
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
        agreement.setTotalPremium(new BigDecimal("0.00"));
        agreement.setTotalSumInsured(new BigDecimal("5000000.00"));
        agreement.setCreatedBy("test-user");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("umr", "B1234X789");
        attributes.put("placing_basis", "OPEN_MARKET");
        attributes.put("cargo_description", "Electronics");
        attributes.put("cargo_value", 2000000);
        attributes.put("institute_clauses", "ICC_A");
        if (rate != null) attributes.put("rate", rate);
        if (limit != null) attributes.put("limit", limit);
        agreement.setAttributes(attributes);
        return agreement;
    }

    // =========================================================================
    // Basic Virtual Field Calculation
    // =========================================================================

    @Nested
    @DisplayName("Basic Virtual Field Calculation")
    class BasicVirtualFieldCalculation {

        @Test
        @DisplayName("Should auto-calculate TotalPremium as rate * limit on create")
        void shouldAutoCalculateTotalPremiumOnCreate() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-001", new BigDecimal("0.02"), new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);

            assertThat(retrieved).isPresent();
            // TotalPremium = 0.02 * 500000 = 10000
            assertThat(retrieved.get().getAttributes()).containsKey("TotalPremium");
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should preserve original operand attributes after calculation")
        void shouldPreserveOriginalOperands() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-002", new BigDecimal("0.05"), new BigDecimal("100000"));

            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);

            assertThat(retrieved).isPresent();
            Map<String, Object> attrs = retrieved.get().getAttributes();
            // Calculated field present
            assertThat(attrs).containsKey("TotalPremium");
            BigDecimal computed = new BigDecimal(attrs.get("TotalPremium").toString());
            assertThat(computed).isEqualByComparingTo(new BigDecimal("5000"));
            // Original operands still present
            assertThat(attrs).containsKey("rate");
            assertThat(attrs).containsKey("limit");
        }
    }

    // =========================================================================
    // Cross-Market Context Virtual Fields
    // =========================================================================

    @Nested
    @DisplayName("Cross-Market Context Virtual Fields")
    class CrossMarketContextVirtualFields {

        @Test
        @DisplayName("Should calculate TotalPremium for RETAIL agreement")
        void shouldCalculateForRetail() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-CTX-R01", new BigDecimal("0.02"), new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMarketContext()).isEqualTo(MarketContext.RETAIL);
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should calculate TotalPremium for COMMERCIAL agreement")
        void shouldCalculateForCommercial() {
            Agreement agreement = buildCommercialAgreementWithRateAndLimit(
                    "VFC-CTX-C01", new BigDecimal("0.015"), new BigDecimal("2000000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMarketContext()).isEqualTo(MarketContext.COMMERCIAL);
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            // 0.015 * 2000000 = 30000
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("30000"));
        }

        @Test
        @DisplayName("Should calculate TotalPremium for LONDON_MARKET agreement")
        void shouldCalculateForLondonMarket() {
            Agreement agreement = buildLondonMarketAgreementWithRateAndLimit(
                    "VFC-CTX-LM01", new BigDecimal("0.03"), new BigDecimal("5000000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getMarketContext()).isEqualTo(MarketContext.LONDON_MARKET);
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            // 0.03 * 5000000 = 150000
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("150000"));
        }
    }

    // =========================================================================
    // Update Recalculation
    // =========================================================================

    @Nested
    @DisplayName("Update Recalculation")
    class UpdateRecalculation {

        @Test
        @DisplayName("Should recalculate TotalPremium when rate changes on update")
        void shouldRecalculateWhenRateChanges() {
            Agreement original = buildRetailAgreementWithRateAndLimit(
                    "VFC-UPD-001", new BigDecimal("0.02"), new BigDecimal("500000"));
            Agreement saved = agreementService.createAgreement(original);
            UUID agreementId = saved.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            // Update with new rate
            Agreement update = buildRetailAgreementWithRateAndLimit(
                    "VFC-UPD-001", new BigDecimal("0.05"), new BigDecimal("500000"));
            update.setTenantId(TENANT_A);
            update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, update);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> current = agreementRepository.findCurrentById(agreementId, TENANT_A_STR);
            assertThat(current).isPresent();
            BigDecimal newTotal = new BigDecimal(
                    current.get().getAttributes().get("TotalPremium").toString());
            // 0.05 * 500000 = 25000
            assertThat(newTotal).isEqualByComparingTo(new BigDecimal("25000"));
        }

        @Test
        @DisplayName("Should recalculate TotalPremium when limit changes on update")
        void shouldRecalculateWhenLimitChanges() {
            Agreement original = buildRetailAgreementWithRateAndLimit(
                    "VFC-UPD-002", new BigDecimal("0.02"), new BigDecimal("500000"));
            Agreement saved = agreementService.createAgreement(original);
            UUID agreementId = saved.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Agreement update = buildRetailAgreementWithRateAndLimit(
                    "VFC-UPD-002", new BigDecimal("0.02"), new BigDecimal("1000000"));
            update.setTenantId(TENANT_A);
            update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, update);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> current = agreementRepository.findCurrentById(agreementId, TENANT_A_STR);
            assertThat(current).isPresent();
            BigDecimal newTotal = new BigDecimal(
                    current.get().getAttributes().get("TotalPremium").toString());
            // 0.02 * 1000000 = 20000
            assertThat(newTotal).isEqualByComparingTo(new BigDecimal("20000"));
        }

        @Test
        @DisplayName("Should recalculate TotalPremium when both rate and limit change")
        void shouldRecalculateWhenBothChange() {
            Agreement original = buildRetailAgreementWithRateAndLimit(
                    "VFC-UPD-003", new BigDecimal("0.02"), new BigDecimal("500000"));
            Agreement saved = agreementService.createAgreement(original);
            UUID agreementId = saved.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            Agreement update = buildRetailAgreementWithRateAndLimit(
                    "VFC-UPD-003", new BigDecimal("0.035"), new BigDecimal("750000"));
            update.setTenantId(TENANT_A);
            update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, update);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> current = agreementRepository.findCurrentById(agreementId, TENANT_A_STR);
            assertThat(current).isPresent();
            BigDecimal newTotal = new BigDecimal(
                    current.get().getAttributes().get("TotalPremium").toString());
            // 0.035 * 750000 = 26250
            assertThat(newTotal).isEqualByComparingTo(new BigDecimal("26250"));
        }
    }

    // =========================================================================
    // Multiple Virtual Fields
    // =========================================================================

    @Nested
    @DisplayName("Multiple Virtual Fields")
    class MultipleVirtualFields {

        @BeforeEach
        void setUpMultipleRules() {
            // Override retail type with chained calculation rules:
            // 1. TotalPremium = rate * limit
            // 2. TaxAmount = TotalPremium * taxRate
            String rules = "[" +
                    "{\"targetField\":\"TotalPremium\",\"expression\":\"rate * limit\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1}," +
                    "{\"targetField\":\"TaxAmount\",\"expression\":\"TotalPremium * taxRate\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":2}" +
                    "]";
            setupCalculationRules(retailTypeId, rules);
        }

        @Test
        @DisplayName("Should calculate multiple virtual fields simultaneously")
        void shouldCalculateMultipleVirtualFieldsSimultaneously() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-MVF-001", new BigDecimal("0.02"), new BigDecimal("500000"));
            agreement.setAttribute("taxRate", new BigDecimal("0.12"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);

            assertThat(retrieved).isPresent();
            Map<String, Object> attrs = retrieved.get().getAttributes();

            // TotalPremium = 0.02 * 500000 = 10000
            assertThat(attrs).containsKey("TotalPremium");
            BigDecimal totalPremium = new BigDecimal(attrs.get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("10000"));

            // TaxAmount = 10000 * 0.12 = 1200
            assertThat(attrs).containsKey("TaxAmount");
            BigDecimal taxAmount = new BigDecimal(attrs.get("TaxAmount").toString());
            assertThat(taxAmount).isEqualByComparingTo(new BigDecimal("1200"));
        }

        @Test
        @DisplayName("Should evaluate dependent virtual fields in correct order")
        void shouldEvaluateDependentFieldsInOrder() {
            // Override with 3 chained rules: TotalPremium -> TaxAmount -> GrossTotal
            String rules = "[" +
                    "{\"targetField\":\"TotalPremium\",\"expression\":\"rate * limit\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1}," +
                    "{\"targetField\":\"TaxAmount\",\"expression\":\"TotalPremium * taxRate\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":2}," +
                    "{\"targetField\":\"GrossTotal\",\"expression\":\"TotalPremium + TaxAmount\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":3}" +
                    "]";
            setupCalculationRules(retailTypeId, rules);

            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-MVF-002", new BigDecimal("0.04"), new BigDecimal("250000"));
            agreement.setAttribute("taxRate", new BigDecimal("0.12"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            Map<String, Object> attrs = retrieved.get().getAttributes();

            // TotalPremium = 0.04 * 250000 = 10000
            BigDecimal totalPremium = new BigDecimal(attrs.get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("10000"));

            // TaxAmount = 10000 * 0.12 = 1200
            BigDecimal taxAmount = new BigDecimal(attrs.get("TaxAmount").toString());
            assertThat(taxAmount).isEqualByComparingTo(new BigDecimal("1200"));

            // GrossTotal = 10000 + 1200 = 11200
            assertThat(attrs).containsKey("GrossTotal");
            BigDecimal grossTotal = new BigDecimal(attrs.get("GrossTotal").toString());
            assertThat(grossTotal).isEqualByComparingTo(new BigDecimal("11200"));
        }
    }

    // =========================================================================
    // Expression Error Handling
    // =========================================================================

    @Nested
    @DisplayName("Expression Error Handling")
    class ExpressionErrorHandling {

        @Test
        @DisplayName("Should skip calculation when rate operand is missing (JEXL safe mode)")
        void shouldSkipCalculationWhenRateMissing() {
            // JEXL3 with strict(false) and safe(true) returns null for undefined variables.
            // The evaluator skips null results (ExpressionEvaluatorImpl line 58-62).
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-ERR-001", null, new BigDecimal("500000"));

            // Should not throw — null result is skipped
            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            // TotalPremium should not be set (expression evaluated to null, was skipped)
            assertThat(retrieved.get().getAttributes()).doesNotContainKey("TotalPremium");
        }

        @Test
        @DisplayName("Should skip calculation when limit operand is missing")
        void shouldSkipCalculationWhenLimitMissing() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-ERR-002", new BigDecimal("0.02"), null);

            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAttributes()).doesNotContainKey("TotalPremium");
        }

        @Test
        @DisplayName("Should skip calculation when both operands are missing")
        void shouldSkipCalculationWhenBothMissing() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-ERR-003", null, null);

            Agreement saved = agreementService.createAgreement(agreement);

            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getAttributes()).doesNotContainKey("TotalPremium");
        }

        @Test
        @DisplayName("Should not create agreement when expression evaluation returns errors")
        void shouldRejectOnExpressionError() {
            // Set up a rule with an invalid expression (division by zero in strict context)
            setupCalculationRules(retailTypeId,
                    "[{\"targetField\":\"BadField\",\"expression\":\"1 / 0\",\"resultType\":\"NUMBER\",\"scale\":4,\"order\":1}]");

            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-ERR-004", new BigDecimal("0.02"), new BigDecimal("500000"));

            // Expression evaluation error should propagate as validation failure
            assertThatThrownBy(() -> agreementService.createAgreement(agreement))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should still create agreement when no calculation rules are defined")
        void shouldCreateAgreementWithoutCalculationRules() {
            // Clear calculation rules
            setupCalculationRules(retailTypeId, "[]");

            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-ERR-005", new BigDecimal("0.02"), new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);
            assertThat(saved.getTemporalKey()).isNotNull();
            assertThat(saved.getAgreementNumber()).isEqualTo("VFC-ERR-005");
        }
    }

    // =========================================================================
    // Null and Zero Handling
    // =========================================================================

    @Nested
    @DisplayName("Null and Zero Handling")
    class NullAndZeroHandling {

        @Test
        @DisplayName("Should calculate TotalPremium as zero when rate is zero")
        void shouldCalculateZeroWhenRateIsZero() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-NZ-001", BigDecimal.ZERO, new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate TotalPremium as zero when limit is zero")
        void shouldCalculateZeroWhenLimitIsZero() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-NZ-002", new BigDecimal("0.02"), BigDecimal.ZERO);

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate TotalPremium as zero when both are zero")
        void shouldCalculateZeroWhenBothAreZero() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-NZ-003", BigDecimal.ZERO, BigDecimal.ZERO);

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            assertThat(totalPremium).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle negative rate correctly")
        void shouldHandleNegativeRate() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-NZ-004", new BigDecimal("-0.01"), new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            // -0.01 * 500000 = -5000
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("-5000"));
        }
    }

    // =========================================================================
    // Decimal Precision
    // =========================================================================

    @Nested
    @DisplayName("Decimal Precision")
    class DecimalPrecision {

        @Test
        @DisplayName("Should maintain BigDecimal precision for small rates")
        void shouldMaintainPrecisionForSmallRates() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-PREC-001", new BigDecimal("0.00015"), new BigDecimal("1000000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            // 0.00015 * 1000000 = 150
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("150"));
        }

        @Test
        @DisplayName("Should not introduce floating-point errors")
        void shouldNotIntroduceFloatingPointErrors() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-PREC-002", new BigDecimal("0.1"), new BigDecimal("0.2"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            // 0.1 * 0.2 = 0.02 exactly (not 0.020000000000000004)
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("0.02"));
        }

        @Test
        @DisplayName("Should handle large monetary values without overflow")
        void shouldHandleLargeMonetaryValues() {
            Agreement agreement = buildLondonMarketAgreementWithRateAndLimit(
                    "VFC-PREC-003", new BigDecimal("0.025"), new BigDecimal("999999999999"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            BigDecimal expected = new BigDecimal("0.025").multiply(new BigDecimal("999999999999"));
            assertThat(totalPremium).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("Should apply scale of 4 as configured in calculation rule")
        void shouldApplyConfiguredScale() {
            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-PREC-004", new BigDecimal("0.033"), new BigDecimal("100000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            BigDecimal totalPremium = new BigDecimal(
                    retrieved.get().getAttributes().get("TotalPremium").toString());
            // 0.033 * 100000 = 3300.0000 (scale 4 from rule config)
            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("3300"));
            assertThat(totalPremium.scale()).isLessThanOrEqualTo(4);
        }
    }

    // =========================================================================
    // Bitemporal + Virtual Fields
    // =========================================================================

    @Nested
    @DisplayName("Bitemporal + Virtual Fields")
    class BitemporalVirtualFields {

        @Test
        @DisplayName("Should preserve original TotalPremium in history after update")
        void shouldPreserveOriginalInHistory() {
            Agreement original = buildRetailAgreementWithRateAndLimit(
                    "VFC-BT-001", new BigDecimal("0.02"), new BigDecimal("500000"));
            Agreement v1 = agreementService.createAgreement(original);
            UUID agreementId = v1.getTemporalKey().getId();

            entityManager.flush();
            entityManager.clear();

            // Update with different rate
            Agreement update = buildRetailAgreementWithRateAndLimit(
                    "VFC-BT-001", new BigDecimal("0.05"), new BigDecimal("500000"));
            update.setTenantId(TENANT_A);
            update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, update);

            entityManager.flush();
            entityManager.clear();

            List<Agreement> history = agreementService.getAgreementHistory(agreementId, TENANT_A_STR);
            assertThat(history).hasSizeGreaterThanOrEqualTo(2);

            // First version: TotalPremium = 0.02 * 500000 = 10000
            Agreement historyV1 = history.get(0);
            BigDecimal v1Total = new BigDecimal(
                    historyV1.getAttributes().get("TotalPremium").toString());
            assertThat(v1Total).isEqualByComparingTo(new BigDecimal("10000"));

            // Second version: TotalPremium = 0.05 * 500000 = 25000
            Agreement historyV2 = history.get(1);
            BigDecimal v2Total = new BigDecimal(
                    historyV2.getAttributes().get("TotalPremium").toString());
            assertThat(v2Total).isEqualByComparingTo(new BigDecimal("25000"));
        }

        @Test
        @DisplayName("Should return correct TotalPremium for as-of temporal query")
        void shouldReturnCorrectTotalPremiumForAsOfQuery() {
            Agreement original = buildRetailAgreementWithRateAndLimit(
                    "VFC-BT-002", new BigDecimal("0.02"), new BigDecimal("500000"));
            Agreement v1 = agreementService.createAgreement(original);
            UUID agreementId = v1.getTemporalKey().getId();
            OffsetDateTime afterV1 = OffsetDateTime.now(ZoneOffset.UTC);

            entityManager.flush();
            entityManager.clear();

            // Update
            Agreement update = buildRetailAgreementWithRateAndLimit(
                    "VFC-BT-002", new BigDecimal("0.05"), new BigDecimal("500000"));
            update.setTenantId(TENANT_A);
            update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, update);

            entityManager.flush();
            entityManager.clear();

            // As-of query at afterV1 should return v1's TotalPremium
            Optional<Agreement> asOfV1 = agreementService.getAgreementAsOf(
                    agreementId, TENANT_A_STR, afterV1);

            assertThat(asOfV1).isPresent();
            BigDecimal totalAtV1 = new BigDecimal(
                    asOfV1.get().getAttributes().get("TotalPremium").toString());
            assertThat(totalAtV1).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should have correct TotalPremium in current version after multiple updates")
        void shouldHaveCorrectTotalPremiumAfterMultipleUpdates() {
            Agreement v1 = agreementService.createAgreement(
                    buildRetailAgreementWithRateAndLimit("VFC-BT-003",
                            new BigDecimal("0.01"), new BigDecimal("100000")));
            UUID agreementId = v1.getTemporalKey().getId();
            entityManager.flush();
            entityManager.clear();

            // Update 1
            Agreement v2Update = buildRetailAgreementWithRateAndLimit("VFC-BT-003",
                    new BigDecimal("0.02"), new BigDecimal("200000"));
            v2Update.setTenantId(TENANT_A);
            v2Update.setStatus(AgreementStatus.QUOTED);
            agreementService.updateAgreement(agreementId, v2Update);
            entityManager.flush();
            entityManager.clear();

            // Update 2
            Agreement v3Update = buildRetailAgreementWithRateAndLimit("VFC-BT-003",
                    new BigDecimal("0.03"), new BigDecimal("300000"));
            v3Update.setTenantId(TENANT_A);
            v3Update.setStatus(AgreementStatus.BOUND);
            agreementService.updateAgreement(agreementId, v3Update);
            entityManager.flush();
            entityManager.clear();

            // Current version should have latest calculation
            Optional<Agreement> current = agreementRepository.findCurrentById(agreementId, TENANT_A_STR);
            assertThat(current).isPresent();
            BigDecimal currentTotal = new BigDecimal(
                    current.get().getAttributes().get("TotalPremium").toString());
            // 0.03 * 300000 = 9000
            assertThat(currentTotal).isEqualByComparingTo(new BigDecimal("9000"));

            // History has 3 versions
            List<Agreement> history = agreementService.getAgreementHistory(agreementId, TENANT_A_STR);
            assertThat(history).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    // =========================================================================
    // Top-Level Field Write-Back
    // =========================================================================

    @Nested
    @DisplayName("Top-Level Field Write-Back")
    class TopLevelFieldWriteBack {

        @Test
        @DisplayName("Should write to totalPremium top-level field when targetField is totalPremium")
        void shouldWriteToTopLevelTotalPremium() {
            // Set up rule that targets the top-level totalPremium field
            setupCalculationRules(retailTypeId,
                    "[{\"targetField\":\"totalPremium\",\"expression\":\"rate * limit\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1}]");

            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-TL-001", new BigDecimal("0.02"), new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            // Should be written to the top-level totalPremium field (not attributes)
            assertThat(retrieved.get().getTotalPremium())
                    .isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should write to totalSumInsured top-level field when targeted")
        void shouldWriteToTopLevelTotalSumInsured() {
            setupCalculationRules(retailTypeId,
                    "[{\"targetField\":\"totalSumInsured\",\"expression\":\"limit * 2\",\"resultType\":\"CURRENCY\",\"scale\":4,\"order\":1}]");

            Agreement agreement = buildRetailAgreementWithRateAndLimit(
                    "VFC-TL-002", new BigDecimal("0.02"), new BigDecimal("500000"));

            Agreement saved = agreementService.createAgreement(agreement);
            entityManager.flush();
            entityManager.clear();

            Optional<Agreement> retrieved = agreementRepository.findCurrentById(
                    saved.getTemporalKey().getId(), TENANT_A_STR);
            assertThat(retrieved).isPresent();
            // limit * 2 = 500000 * 2 = 1000000
            assertThat(retrieved.get().getTotalSumInsured())
                    .isEqualByComparingTo(new BigDecimal("1000000"));
        }
    }
}
