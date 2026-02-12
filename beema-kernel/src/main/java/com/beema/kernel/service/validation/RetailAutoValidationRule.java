package com.beema.kernel.service.validation;

import com.beema.kernel.domain.metadata.MarketContext;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validation rules for RETAIL AUTO_POLICY.
 *
 * Business rules:
 * - Vehicle must be less than 30 years old
 * - Driver must be 16-85 years old
 * - Annual mileage must be reasonable (0-100,000)
 * - VIN must be 17 characters
 */
@Component
public class RetailAutoValidationRule implements ContextValidationRule {

    private static final int MAX_VEHICLE_AGE = 30;
    private static final int MIN_DRIVER_AGE = 16;
    private static final int MAX_DRIVER_AGE = 85;
    private static final int MAX_ANNUAL_MILEAGE = 100_000;

    @Override
    public MarketContext getMarketContext() {
        return MarketContext.RETAIL;
    }

    @Override
    public String getAgreementTypeCode() {
        return "AUTO_POLICY";
    }

    @Override
    public List<String> validate(Map<String, Object> attributes) {
        List<String> errors = new ArrayList<>();

        // Validate vehicle year
        Object vehicleYear = attributes.get("vehicle_year");
        if (vehicleYear instanceof Number) {
            int year = ((Number) vehicleYear).intValue();
            int currentYear = Year.now().getValue();
            int vehicleAge = currentYear - year;

            if (vehicleAge > MAX_VEHICLE_AGE) {
                errors.add(String.format(
                    "Vehicle is too old: %d years (max: %d years)",
                    vehicleAge, MAX_VEHICLE_AGE
                ));
            }

            if (year > currentYear + 1) {
                errors.add(String.format(
                    "Vehicle year %d is too far in the future (current: %d)",
                    year, currentYear
                ));
            }
        }

        // Validate driver age
        Object driverAge = attributes.get("primary_driver_age");
        if (driverAge instanceof Number) {
            int age = ((Number) driverAge).intValue();

            if (age < MIN_DRIVER_AGE) {
                errors.add(String.format(
                    "Driver too young: %d (minimum: %d)",
                    age, MIN_DRIVER_AGE
                ));
            }

            if (age > MAX_DRIVER_AGE) {
                errors.add(String.format(
                    "Driver age %d exceeds underwriting limit (%d)",
                    age, MAX_DRIVER_AGE
                ));
            }
        }

        // Validate annual mileage if present
        Object annualMileage = attributes.get("annual_mileage");
        if (annualMileage instanceof Number) {
            int mileage = ((Number) annualMileage).intValue();

            if (mileage < 0) {
                errors.add("Annual mileage cannot be negative");
            }

            if (mileage > MAX_ANNUAL_MILEAGE) {
                errors.add(String.format(
                    "Annual mileage %d exceeds maximum (%d)",
                    mileage, MAX_ANNUAL_MILEAGE
                ));
            }
        }

        // Validate VIN format
        Object vin = attributes.get("vehicle_vin");
        if (vin instanceof String) {
            String vinStr = (String) vin;
            if (vinStr.length() != 17) {
                errors.add(String.format(
                    "VIN must be exactly 17 characters (got: %d)",
                    vinStr.length()
                ));
            }
        }

        return errors;
    }

    @Override
    public String getDescription() {
        return "RETAIL Auto Policy validation: vehicle age, driver age, mileage limits";
    }
}
