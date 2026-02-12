package com.beema.kernel.service.expression;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.util.SchemaValidator.ValidationResult;

import java.util.List;
import java.util.Map;

/**
 * Evaluates calculation expressions against agreement data.
 * Computed results are written back into the agreement's attributes
 * or top-level fields before persistence.
 */
public interface ExpressionEvaluator {

    /**
     * Evaluates all calculation rules against the given agreement,
     * writing computed results back into the agreement.
     *
     * @param agreement        the agreement to evaluate and mutate
     * @param calculationRules raw calculation rule maps from metadata
     * @return validation result — valid if all expressions succeeded,
     *         invalid with error messages if any failed
     */
    ValidationResult evaluateCalculations(Agreement agreement,
                                          List<Map<String, Object>> calculationRules);
}
