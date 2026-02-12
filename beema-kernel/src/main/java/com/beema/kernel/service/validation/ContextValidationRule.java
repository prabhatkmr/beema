package com.beema.kernel.service.validation;

import com.beema.kernel.domain.metadata.MarketContext;

import java.util.List;
import java.util.Map;

/**
 * Interface for market-context-specific validation rules.
 *
 * Each market context can have custom business rules beyond JSON Schema validation.
 */
public interface ContextValidationRule {

    /**
     * Market context this rule applies to.
     */
    MarketContext getMarketContext();

    /**
     * Agreement type code this rule applies to.
     */
    String getAgreementTypeCode();

    /**
     * Validate agreement attributes.
     *
     * @param attributes Agreement attributes
     * @return List of validation errors (empty if valid)
     */
    List<String> validate(Map<String, Object> attributes);

    /**
     * Rule description for documentation.
     */
    String getDescription();
}
