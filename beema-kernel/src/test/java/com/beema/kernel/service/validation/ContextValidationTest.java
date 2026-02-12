package com.beema.kernel.service.validation;

import com.beema.kernel.domain.metadata.MarketContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for context-specific validation rules.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContextValidationTest {

    @Autowired
    private ContextValidationService validationService;

    // ========================================================================
    // RETAIL Auto Policy Tests
    // ========================================================================

    @Test
    void shouldPassValidRetailAutoPolicy() {
        // Given: Valid auto policy attributes
        Map<String, Object> attributes = Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35,
            "annual_mileage", 12000
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.RETAIL,
            "AUTO_POLICY",
            attributes
        );

        // Then: Validation passes
        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectTooOldVehicle() {
        // Given: Vehicle older than 30 years
        Map<String, Object> attributes = Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 1985,  // Too old
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.RETAIL,
            "AUTO_POLICY",
            attributes
        );

        // Then: Validation fails
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("too old"));
    }

    @Test
    void shouldRejectTooYoungDriver() {
        // Given: Driver under 16
        Map<String, Object> attributes = Map.of(
            "vehicle_vin", "1HGCM82633A123456",
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 15  // Too young
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.RETAIL,
            "AUTO_POLICY",
            attributes
        );

        // Then: Validation fails
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("too young"));
    }

    @Test
    void shouldRejectInvalidVinLength() {
        // Given: Invalid VIN length
        Map<String, Object> attributes = Map.of(
            "vehicle_vin", "INVALID",  // Not 17 characters
            "vehicle_year", 2024,
            "vehicle_make", "Honda",
            "vehicle_model", "Accord",
            "primary_driver_age", 35
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.RETAIL,
            "AUTO_POLICY",
            attributes
        );

        // Then: Validation fails
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("17 characters"));
    }

    // ========================================================================
    // COMMERCIAL General Liability Tests
    // ========================================================================

    @Test
    void shouldPassValidCommercialLiability() {
        // Given: Valid commercial liability attributes
        Map<String, Object> attributes = Map.of(
            "business_name", "Acme Corp",
            "industry_code", "5411",
            "annual_revenue", 1_000_000.0,
            "number_of_employees", 50,
            "years_in_business", 5,
            "prior_claims_count", 1
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.COMMERCIAL,
            "GENERAL_LIABILITY",
            attributes
        );

        // Then: Validation passes
        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldFlagHighRevenue() {
        // Given: Revenue exceeding auto-approval limit
        Map<String, Object> attributes = Map.of(
            "business_name", "Mega Corp",
            "industry_code", "5411",
            "annual_revenue", 15_000_000.0,  // > $10M
            "number_of_employees", 50,
            "years_in_business", 10
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.COMMERCIAL,
            "GENERAL_LIABILITY",
            attributes
        );

        // Then: Validation fails (requires review)
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("underwriter review"));
    }

    @Test
    void shouldFlagHighRiskIndustry() {
        // Given: High-risk industry code
        Map<String, Object> attributes = Map.of(
            "business_name", "Bar & Grill",
            "industry_code", "5812",  // High-risk: eating places
            "annual_revenue", 500_000.0,
            "number_of_employees", 20,
            "years_in_business", 3
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.COMMERCIAL,
            "GENERAL_LIABILITY",
            attributes
        );

        // Then: Validation fails (requires approval)
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("high-risk"));
    }

    // ========================================================================
    // LONDON_MARKET Marine Cargo Tests
    // ========================================================================

    @Test
    void shouldPassValidMarineCargo() {
        // Given: Valid marine cargo attributes
        Map<String, Object> attributes = Map.of(
            "cargo_type", "CONTAINERIZED",
            "cargo_value", 500_000.0,
            "vessel_name", "MV Shipping Star",
            "vessel_imo_number", "IMO1234567",
            "voyage_from", "SINGAPORE",
            "voyage_to", "ROTTERDAM",
            "incoterms", "CIF"
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.LONDON_MARKET,
            "MARINE_CARGO",
            attributes
        );

        // Then: Validation passes
        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldFlagHighValueCargo() {
        // Given: Cargo value exceeding single vessel limit
        Map<String, Object> attributes = Map.of(
            "cargo_type", "GENERAL_CARGO",
            "cargo_value", 60_000_000.0,  // > $50M
            "vessel_name", "MV Big Ship",
            "voyage_from", "SHANGHAI",
            "voyage_to", "LOS_ANGELES"
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.LONDON_MARKET,
            "MARINE_CARGO",
            attributes
        );

        // Then: Validation fails (requires syndicate approval)
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("syndicate approval"));
    }

    @Test
    void shouldFlagHighRiskRoute() {
        // Given: High-risk route
        Map<String, Object> attributes = Map.of(
            "cargo_type", "CONTAINERIZED",
            "cargo_value", 1_000_000.0,
            "vessel_name", "MV Brave Voyager",
            "voyage_from", "GULF_OF_ADEN",  // High-risk
            "voyage_to", "DUBAI"
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.LONDON_MARKET,
            "MARINE_CARGO",
            attributes
        );

        // Then: Validation fails (requires war risk coverage)
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("war risk"));
    }

    @Test
    void shouldRejectRestrictedCargo() {
        // Given: Restricted cargo type
        Map<String, Object> attributes = Map.of(
            "cargo_type", "WEAPONS",  // Restricted
            "cargo_value", 1_000_000.0,
            "vessel_name", "MV No Coverage",
            "voyage_from", "PORT_A",
            "voyage_to", "PORT_B"
        );

        // When: Validating
        var result = validationService.validate(
            MarketContext.LONDON_MARKET,
            "MARINE_CARGO",
            attributes
        );

        // Then: Validation fails (restricted)
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("restricted"));
    }
}
