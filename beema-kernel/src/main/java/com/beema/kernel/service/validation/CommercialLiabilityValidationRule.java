package com.beema.kernel.service.validation;

import com.beema.kernel.domain.metadata.MarketContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validation rules for COMMERCIAL GENERAL_LIABILITY.
 *
 * Business rules:
 * - Revenue must be positive
 * - Employee count must be reasonable
 * - Years in business must be non-negative
 * - High-risk industries require additional review
 */
@Component
public class CommercialLiabilityValidationRule implements ContextValidationRule {

    private static final long MAX_REVENUE_WITHOUT_REVIEW = 10_000_000; // $10M
    private static final int MAX_EMPLOYEES_WITHOUT_REVIEW = 500;
    private static final List<String> HIGH_RISK_INDUSTRIES = List.of("5812", "7389", "8699");

    @Override
    public MarketContext getMarketContext() {
        return MarketContext.COMMERCIAL;
    }

    @Override
    public String getAgreementTypeCode() {
        return "GENERAL_LIABILITY";
    }

    @Override
    public List<String> validate(Map<String, Object> attributes) {
        List<String> errors = new ArrayList<>();

        // Validate annual revenue
        Object revenue = attributes.get("annual_revenue");
        if (revenue instanceof Number) {
            double revenueValue = ((Number) revenue).doubleValue();

            if (revenueValue < 0) {
                errors.add("Annual revenue cannot be negative");
            }

            if (revenueValue > MAX_REVENUE_WITHOUT_REVIEW) {
                errors.add(String.format(
                    "Revenue $%.2f exceeds auto-approval limit ($%.2f) - requires underwriter review",
                    revenueValue, (double) MAX_REVENUE_WITHOUT_REVIEW
                ));
            }
        }

        // Validate employee count
        Object employees = attributes.get("number_of_employees");
        if (employees instanceof Number) {
            int empCount = ((Number) employees).intValue();

            if (empCount < 0) {
                errors.add("Number of employees cannot be negative");
            }

            if (empCount > MAX_EMPLOYEES_WITHOUT_REVIEW) {
                errors.add(String.format(
                    "Employee count %d requires additional review (threshold: %d)",
                    empCount, MAX_EMPLOYEES_WITHOUT_REVIEW
                ));
            }
        }

        // Validate years in business
        Object yearsInBusiness = attributes.get("years_in_business");
        if (yearsInBusiness instanceof Number) {
            int years = ((Number) yearsInBusiness).intValue();

            if (years < 0) {
                errors.add("Years in business cannot be negative");
            }

            if (years == 0) {
                errors.add("Startup businesses (< 1 year) require special underwriting");
            }
        }

        // Check high-risk industries
        Object industryCode = attributes.get("industry_code");
        if (industryCode instanceof String) {
            String code = (String) industryCode;

            if (HIGH_RISK_INDUSTRIES.contains(code)) {
                errors.add(String.format(
                    "Industry code %s is classified as high-risk - requires senior underwriter approval",
                    code
                ));
            }
        }

        // Validate prior claims
        Object priorClaims = attributes.get("prior_claims_count");
        if (priorClaims instanceof Number) {
            int claims = ((Number) priorClaims).intValue();

            if (claims < 0) {
                errors.add("Prior claims count cannot be negative");
            }

            if (claims > 3) {
                errors.add(String.format(
                    "Prior claims count %d exceeds acceptable threshold (3)",
                    claims
                ));
            }
        }

        return errors;
    }

    @Override
    public String getDescription() {
        return "COMMERCIAL General Liability validation: revenue, employees, industry risk";
    }
}
