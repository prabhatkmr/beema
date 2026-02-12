package com.beema.kernel.service.validation;

import com.beema.kernel.domain.metadata.MarketContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validation rules for LONDON_MARKET MARINE_CARGO.
 *
 * Business rules:
 * - Cargo value limits per vessel
 * - High-risk route restrictions
 * - Hazardous cargo surcharge requirements
 * - War risk zones
 */
@Component
public class LondonMarketCargoValidationRule implements ContextValidationRule {

    private static final double MAX_SINGLE_VESSEL_VALUE = 50_000_000; // $50M
    private static final List<String> HIGH_RISK_ROUTES = List.of(
        "SOMALIA", "GULF_OF_ADEN", "STRAIT_OF_MALACCA", "WEST_AFRICA"
    );
    private static final List<String> RESTRICTED_CARGO = List.of(
        "WEAPONS", "NUCLEAR_MATERIAL", "NARCOTICS"
    );

    @Override
    public MarketContext getMarketContext() {
        return MarketContext.LONDON_MARKET;
    }

    @Override
    public String getAgreementTypeCode() {
        return "MARINE_CARGO";
    }

    @Override
    public List<String> validate(Map<String, Object> attributes) {
        List<String> errors = new ArrayList<>();

        // Validate cargo value
        Object cargoValue = attributes.get("cargo_value");
        if (cargoValue instanceof Number) {
            double value = ((Number) cargoValue).doubleValue();

            if (value <= 0) {
                errors.add("Cargo value must be positive");
            }

            if (value > MAX_SINGLE_VESSEL_VALUE) {
                errors.add(String.format(
                    "Cargo value $%.2f exceeds single vessel limit ($%.2f) - requires syndicate approval",
                    value, MAX_SINGLE_VESSEL_VALUE
                ));
            }
        }

        // Check high-risk routes
        String voyageFrom = getStringAttribute(attributes, "voyage_from");
        String voyageTo = getStringAttribute(attributes, "voyage_to");

        if (voyageFrom != null && HIGH_RISK_ROUTES.stream().anyMatch(voyageFrom::contains)) {
            errors.add(String.format(
                "Origin %s is in a high-risk zone - requires war risk coverage",
                voyageFrom
            ));
        }

        if (voyageTo != null && HIGH_RISK_ROUTES.stream().anyMatch(voyageTo::contains)) {
            errors.add(String.format(
                "Destination %s is in a high-risk zone - requires war risk coverage",
                voyageTo
            ));
        }

        // Validate cargo type
        String cargoType = getStringAttribute(attributes, "cargo_type");
        if (cargoType != null) {
            if (RESTRICTED_CARGO.stream().anyMatch(cargoType::contains)) {
                errors.add(String.format(
                    "Cargo type %s is restricted - coverage not available",
                    cargoType
                ));
            }

            if ("HAZARDOUS".equals(cargoType)) {
                errors.add("Hazardous cargo requires 50% premium surcharge and special handling");
            }

            if ("PERISHABLES".equals(cargoType)) {
                errors.add("Perishable cargo requires refrigeration certificate and 25% surcharge");
            }
        }

        // Validate vessel IMO number if provided
        String imoNumber = getStringAttribute(attributes, "vessel_imo_number");
        if (imoNumber != null && !imoNumber.matches("^IMO[0-9]{7}$")) {
            errors.add(String.format(
                "Invalid IMO number format: %s (must be IMO followed by 7 digits)",
                imoNumber
            ));
        }

        // Check incoterms
        String incoterms = getStringAttribute(attributes, "incoterms");
        if (incoterms != null) {
            if ("EXW".equals(incoterms)) {
                errors.add("EXW terms - buyer assumes all risk, verify coverage intent");
            }
        }

        return errors;
    }

    @Override
    public String getDescription() {
        return "LONDON_MARKET Marine Cargo validation: cargo value limits, high-risk routes, restricted cargo";
    }

    private String getStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String ? (String) value : null;
    }
}
