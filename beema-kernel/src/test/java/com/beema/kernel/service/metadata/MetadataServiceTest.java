package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.util.SchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for MetadataService.
 *
 * Tests:
 * - Schema registration
 * - Schema retrieval
 * - Attribute validation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MetadataServiceTest {

    @Autowired
    private MetadataService metadataService;

    @Test
    void shouldRegisterAndRetrieveAgreementType() {
        // Given: A new agreement type
        MetadataAgreementType type = new MetadataAgreementType();
        type.setTypeCode("TEST_POLICY");
        type.setMarketContext(MarketContext.RETAIL);
        type.setDisplayName("Test Policy");
        type.setDescription("Test policy for unit testing");
        type.setAttributeSchema(Map.of(
            "type", "object",
            "required", new String[]{"test_field"},
            "properties", Map.of(
                "test_field", Map.of(
                    "type", "string",
                    "minLength", 1
                )
            )
        ));
        type.setCreatedBy("test-user");
        type.setUpdatedBy("test-user");

        // When: Registering the type
        MetadataAgreementType saved = metadataService.registerAgreementType(type);

        // Then: Type is saved with ID and version
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSchemaVersion()).isEqualTo(1);
        assertThat(saved.getIsActive()).isTrue();

        // When: Retrieving the type
        Optional<MetadataAgreementType> retrieved = metadataService.getAgreementType(
            "TEST_POLICY",
            MarketContext.RETAIL
        );

        // Then: Type is found
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTypeCode()).isEqualTo("TEST_POLICY");
        assertThat(retrieved.get().getMarketContext()).isEqualTo(MarketContext.RETAIL);
    }

    @Test
    void shouldValidateAttributesAgainstSchema() {
        // Given: An agreement type with schema (from seed data)
        String typeCode = "AUTO_POLICY";
        MarketContext marketContext = MarketContext.RETAIL;

        // When: Validating valid attributes
        Map<String, Object> validAttributes = Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35
        );

        SchemaValidator.ValidationResult validResult = metadataService.validateAttributes(
            typeCode,
            marketContext,
            validAttributes
        );

        // Then: Validation passes
        assertThat(validResult.isValid()).isTrue();
        assertThat(validResult.errors()).isEmpty();

        // When: Validating invalid attributes (missing required field)
        Map<String, Object> invalidAttributes = Map.of(
            "vehicle_year", 2024
            // Missing vehicle_vin, vehicle_make, vehicle_model, primary_driver_age
        );

        SchemaValidator.ValidationResult invalidResult = metadataService.validateAttributes(
            typeCode,
            marketContext,
            invalidAttributes
        );

        // Then: Validation fails
        assertThat(invalidResult.isValid()).isFalse();
        assertThat(invalidResult.errors()).isNotEmpty();
    }

    @Test
    void shouldRetrieveAllAgreementTypesByMarket() {
        // When: Retrieving all RETAIL types
        var retailTypes = metadataService.getAgreementTypesByMarket(MarketContext.RETAIL);

        // Then: Should find at least AUTO_POLICY from seed data
        assertThat(retailTypes).isNotEmpty();
        assertThat(retailTypes.stream()
            .anyMatch(t -> t.getTypeCode().equals("AUTO_POLICY"))
        ).isTrue();

        // When: Retrieving all COMMERCIAL types
        var commercialTypes = metadataService.getAgreementTypesByMarket(MarketContext.COMMERCIAL);

        // Then: Should find GENERAL_LIABILITY from seed data
        assertThat(commercialTypes).isNotEmpty();
        assertThat(commercialTypes.stream()
            .anyMatch(t -> t.getTypeCode().equals("GENERAL_LIABILITY"))
        ).isTrue();

        // When: Retrieving all LONDON_MARKET types
        var londonTypes = metadataService.getAgreementTypesByMarket(MarketContext.LONDON_MARKET);

        // Then: Should find MARINE_CARGO from seed data
        assertThat(londonTypes).isNotEmpty();
        assertThat(londonTypes.stream()
            .anyMatch(t -> t.getTypeCode().equals("MARINE_CARGO"))
        ).isTrue();
    }

    @Test
    void shouldDeactivateAgreementType() {
        // Given: A registered agreement type
        MetadataAgreementType type = new MetadataAgreementType();
        type.setTypeCode("DEACTIVATE_TEST");
        type.setMarketContext(MarketContext.RETAIL);
        type.setDisplayName("Deactivate Test");
        type.setAttributeSchema(Map.of("type", "object"));
        type.setCreatedBy("test-user");
        type.setUpdatedBy("test-user");

        metadataService.registerAgreementType(type);

        // When: Deactivating the type
        metadataService.deactivateAgreementType("DEACTIVATE_TEST", MarketContext.RETAIL);

        // Then: Type is no longer in active list
        var activeTypes = metadataService.getAgreementTypesByMarket(MarketContext.RETAIL);
        assertThat(activeTypes.stream()
            .anyMatch(t -> t.getTypeCode().equals("DEACTIVATE_TEST"))
        ).isFalse();
    }
}
