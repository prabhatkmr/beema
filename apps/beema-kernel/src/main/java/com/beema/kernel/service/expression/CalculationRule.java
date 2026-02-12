package com.beema.kernel.service.expression;

import java.util.Map;

/**
 * Typed representation of a calculation rule stored in metadata.
 *
 * @param targetField attribute key or top-level field name where the result is written
 * @param expression  JEXL3 expression string evaluated against agreement context
 * @param resultType  coercion type: CURRENCY, NUMBER, PERCENTAGE, BOOLEAN
 * @param scale       decimal places for numeric results (default 4)
 * @param order       evaluation sequence (ascending); earlier results available to later expressions
 * @param description human-readable description of the calculation
 */
public record CalculationRule(
        String targetField,
        String expression,
        String resultType,
        int scale,
        int order,
        String description
) {

    public static CalculationRule fromMap(Map<String, Object> map) {
        return new CalculationRule(
                (String) map.get("targetField"),
                (String) map.get("expression"),
                (String) map.getOrDefault("resultType", "NUMBER"),
                ((Number) map.getOrDefault("scale", 4)).intValue(),
                ((Number) map.getOrDefault("order", 0)).intValue(),
                (String) map.get("description")
        );
    }
}
