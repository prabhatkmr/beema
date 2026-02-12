package com.beema.kernel.service.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AgreementService demonstrating bitemporal functionality.
 *
 * Tests:
 * - Agreement creation with validation
 * - Temporal updates (versioning)
 * - Point-in-time queries
 * - Audit trail
 * - Status changes
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgreementServiceTest {

    @Autowired
    private AgreementService agreementService;

    @Test
    void shouldCreateAgreementWithValidation() {
        // Given: A new AUTO_POLICY agreement
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber("TEST-AUTO-001");
        agreement.setAgreementTypeCode("AUTO_POLICY");
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId("tenant-test");
        agreement.setDataResidencyRegion("US");
        agreement.setCreatedBy("test-user");
        agreement.setUpdatedBy("test-user");

        // Valid attributes matching AUTO_POLICY schema from seed data
        agreement.setAttributes(Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35,
            "annual_mileage", 12000
        ));

        // When: Creating the agreement
        Agreement created = agreementService.createAgreement(agreement);

        // Then: Agreement is created successfully
        assertThat(created.getId()).isNotNull();
        assertThat(created.getAgreementNumber()).isEqualTo("TEST-AUTO-001");
        assertThat(created.getStatus()).isEqualTo(AgreementStatus.DRAFT);
        assertThat(created.getIsCurrent()).isTrue();
        assertThat(created.getAttribute("vehicle_vin")).isEqualTo("1HGCM82633A123456");
    }

    @Test
    void shouldCreateTemporalVersionOnUpdate() throws InterruptedException {
        // Given: An existing agreement
        Agreement original = new Agreement();
        original.setAgreementNumber("TEST-TEMPORAL-001");
        original.setAgreementTypeCode("AUTO_POLICY");
        original.setMarketContext(MarketContext.RETAIL);
        original.setTenantId("tenant-test");
        original.setCreatedBy("test-user");
        original.setUpdatedBy("test-user");
        original.setAttributes(Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35
        ));

        Agreement created = agreementService.createAgreement(original);
        OffsetDateTime creationTime = OffsetDateTime.now();

        // Small delay to ensure different transaction times
        Thread.sleep(100);

        // When: Updating the agreement (change driver age)
        OffsetDateTime effectiveFrom = OffsetDateTime.now().plusDays(1);
        Map<String, Object> updatedAttributes = Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 36  // Changed
        );

        Agreement updated = agreementService.updateAgreement(
            created.getId(),
            Map.of("attributes", updatedAttributes),
            effectiveFrom,
            "test-user"
        );

        // Then: New version is created
        assertThat(updated.getId()).isEqualTo(created.getId());  // Same business ID
        assertThat(updated.getIsCurrent()).isTrue();
        assertThat(updated.getAttribute("primary_driver_age")).isEqualTo(36);

        // And: History contains both versions
        List<Agreement> history = agreementService.getAgreementHistory(created.getId());
        assertThat(history).hasSize(2);

        // And: Original version is closed
        Optional<Agreement> originalVersion = agreementService.getAgreementAsOf(
            created.getId(),
            creationTime,
            creationTime
        );
        assertThat(originalVersion).isPresent();
        assertThat(originalVersion.get().getAttribute("primary_driver_age")).isEqualTo(35);
    }

    @Test
    void shouldQueryPointInTime() throws InterruptedException {
        // Given: An agreement created at T1
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber("TEST-PIT-001");
        agreement.setAgreementTypeCode("AUTO_POLICY");
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setTenantId("tenant-test");
        agreement.setCreatedBy("test-user");
        agreement.setUpdatedBy("test-user");
        agreement.setAttributes(Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 30
        ));

        Agreement created = agreementService.createAgreement(agreement);
        OffsetDateTime t1 = OffsetDateTime.now();

        Thread.sleep(100);

        // When: Updated at T2 with future effective date
        OffsetDateTime effectiveT2 = OffsetDateTime.now().plusMonths(1);
        agreementService.updateAgreement(
            created.getId(),
            Map.of("attributes", Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", 31
            )),
            effectiveT2,
            "test-user"
        );

        OffsetDateTime t2 = OffsetDateTime.now();

        // Then: Query at T1 returns age 30
        Optional<Agreement> atT1 = agreementService.getAgreementAsOf(
            created.getId(),
            t1,
            t1
        );
        assertThat(atT1).isPresent();
        assertThat(atT1.get().getAttribute("primary_driver_age")).isEqualTo(30);

        // And: Query at future effective date returns age 31
        Optional<Agreement> atT2Effective = agreementService.getAgreementAsOf(
            created.getId(),
            effectiveT2,
            t2
        );
        assertThat(atT2Effective).isPresent();
        assertThat(atT2Effective.get().getAttribute("primary_driver_age")).isEqualTo(31);
    }

    @Test
    void shouldRetrieveCompleteAuditTrail() throws InterruptedException {
        // Given: An agreement with multiple updates
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber("TEST-AUDIT-001");
        agreement.setAgreementTypeCode("AUTO_POLICY");
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setTenantId("tenant-test");
        agreement.setCreatedBy("test-user");
        agreement.setUpdatedBy("test-user");
        agreement.setAttributes(Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 25
        ));

        Agreement created = agreementService.createAgreement(agreement);

        // Update 1: Change age to 26
        Thread.sleep(50);
        agreementService.updateAgreement(
            created.getId(),
            Map.of("attributes", Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", 26
            )),
            OffsetDateTime.now(),
            "test-user"
        );

        // Update 2: Change age to 27
        Thread.sleep(50);
        agreementService.updateAgreement(
            created.getId(),
            Map.of("attributes", Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", 27
            )),
            OffsetDateTime.now(),
            "test-user"
        );

        // When: Retrieving audit trail
        List<Agreement> history = agreementService.getAgreementHistory(created.getId());

        // Then: All 3 versions are present
        assertThat(history).hasSize(3);

        // And: Versions show progression
        assertThat(history.get(0).getAttribute("primary_driver_age")).isEqualTo(25);
        assertThat(history.get(1).getAttribute("primary_driver_age")).isEqualTo(26);
        assertThat(history.get(2).getAttribute("primary_driver_age")).isEqualTo(27);

        // And: Only latest is current
        assertThat(history.stream().filter(a -> a.getIsCurrent()).count()).isEqualTo(1);
        assertThat(history.get(2).getIsCurrent()).isTrue();
    }

    @Test
    void shouldChangeStatus() {
        // Given: A DRAFT agreement
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber("TEST-STATUS-001");
        agreement.setAgreementTypeCode("AUTO_POLICY");
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setTenantId("tenant-test");
        agreement.setCreatedBy("test-user");
        agreement.setUpdatedBy("test-user");
        agreement.setAttributes(Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35
        ));

        Agreement created = agreementService.createAgreement(agreement);
        assertThat(created.getStatus()).isEqualTo(AgreementStatus.DRAFT);

        // When: Changing status to QUOTED
        Agreement quoted = agreementService.changeStatus(
            created.getId(),
            AgreementStatus.QUOTED,
            OffsetDateTime.now(),
            "underwriter"
        );

        // Then: Status is updated
        assertThat(quoted.getStatus()).isEqualTo(AgreementStatus.QUOTED);
        assertThat(quoted.getUpdatedBy()).isEqualTo("underwriter");

        // And: History shows both statuses
        List<Agreement> history = agreementService.getAgreementHistory(created.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getStatus()).isEqualTo(AgreementStatus.DRAFT);
        assertThat(history.get(1).getStatus()).isEqualTo(AgreementStatus.QUOTED);
    }

    @Test
    void shouldFindByJsonbAttribute() {
        // Given: Multiple agreements with different makes
        createTestAgreement("TEST-HONDA-001", "Honda");
        createTestAgreement("TEST-HONDA-002", "Honda");
        createTestAgreement("TEST-TOYOTA-001", "Toyota");

        // When: Finding all Honda vehicles
        List<Agreement> hondas = agreementService.findByAttribute(
            "tenant-test",
            Map.of("vehicle_make", "Honda")
        );

        // Then: Only Honda agreements are returned
        assertThat(hondas).hasSize(2);
        assertThat(hondas).allMatch(a ->
            "Honda".equals(a.getAttribute("vehicle_make"))
        );
    }

    // Helper method
    private Agreement createTestAgreement(String number, String make) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(number);
        agreement.setAgreementTypeCode("AUTO_POLICY");
        agreement.setMarketContext(MarketContext.RETAIL);
        agreement.setTenantId("tenant-test");
        agreement.setCreatedBy("test-user");
        agreement.setUpdatedBy("test-user");
        agreement.setAttributes(Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", make,
            "vehicle_model", "TestModel",
            "primary_driver_age", 35
        ));
        return agreementService.createAgreement(agreement);
    }
}
