package com.beema.kernel.service.validation;

import com.beema.kernel.domain.metadata.MarketContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for context-specific validation.
 *
 * Applies business rules beyond JSON Schema validation.
 */
@Service
public class ContextValidationService {

    private static final Logger log = LoggerFactory.getLogger(ContextValidationService.class);

    private final List<ContextValidationRule> validationRules;

    public ContextValidationService(List<ContextValidationRule> validationRules) {
        this.validationRules = validationRules;
        log.info("Loaded {} context validation rules", validationRules.size());
        validationRules.forEach(rule ->
            log.info("  - {}/{}: {}",
                rule.getMarketContext(),
                rule.getAgreementTypeCode(),
                rule.getDescription())
        );
    }

    /**
     * Validate attributes with context-specific rules.
     *
     * @param marketContext Market context
     * @param agreementTypeCode Agreement type code
     * @param attributes Attributes to validate
     * @return Validation result
     */
    public ValidationResult validate(
        MarketContext marketContext,
        String agreementTypeCode,
        Map<String, Object> attributes
    ) {
        log.debug("Validating with context-specific rules: {} / {}",
            marketContext, agreementTypeCode);

        List<String> allErrors = new ArrayList<>();

        // Find and apply matching rules
        List<ContextValidationRule> matchingRules = validationRules.stream()
            .filter(rule ->
                rule.getMarketContext() == marketContext &&
                rule.getAgreementTypeCode().equals(agreementTypeCode)
            )
            .collect(Collectors.toList());

        if (matchingRules.isEmpty()) {
            log.debug("No context-specific validation rules found for {} / {}",
                marketContext, agreementTypeCode);
            return new ValidationResult(true, List.of());
        }

        log.debug("Applying {} context-specific rules", matchingRules.size());

        for (ContextValidationRule rule : matchingRules) {
            List<String> errors = rule.validate(attributes);
            allErrors.addAll(errors);
        }

        boolean isValid = allErrors.isEmpty();

        if (!isValid) {
            log.warn("Context validation failed: {}", allErrors);
        } else {
            log.debug("Context validation passed");
        }

        return new ValidationResult(isValid, allErrors);
    }

    /**
     * Validation result.
     */
    public record ValidationResult(boolean isValid, List<String> errors) {
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
