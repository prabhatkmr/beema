package com.beema.kernel.service.expression;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.util.SchemaValidator.ValidationResult;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ExpressionEvaluatorImpl implements ExpressionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ExpressionEvaluatorImpl.class);

    private final JexlEngine jexlEngine;

    public ExpressionEvaluatorImpl() {
        this.jexlEngine = new JexlBuilder()
                .strict(false)
                .silent(false)
                .safe(true)
                .permissions(createPermissions())
                .create();
    }

    @Override
    public ValidationResult evaluateCalculations(Agreement agreement,
                                                  List<Map<String, Object>> calculationRules) {
        if (calculationRules == null || calculationRules.isEmpty()) {
            return ValidationResult.valid();
        }

        List<CalculationRule> rules = calculationRules.stream()
                .map(CalculationRule::fromMap)
                .sorted(Comparator.comparingInt(CalculationRule::order))
                .toList();

        List<String> errors = new ArrayList<>();
        MapContext context = buildContext(agreement);

        for (CalculationRule rule : rules) {
            try {
                JexlExpression expr = jexlEngine.createExpression(rule.expression());
                Object rawResult = expr.evaluate(context);

                if (rawResult == null) {
                    log.debug("Expression '{}' for field '{}' evaluated to null, skipping",
                            rule.expression(), rule.targetField());
                    continue;
                }

                Object typedResult = coerceResult(rawResult, rule);
                writeResult(agreement, rule.targetField(), typedResult);

                // Update context so subsequent expressions can reference this result
                context.set(rule.targetField(), typedResult);

            } catch (JexlException e) {
                String errorMsg = String.format(
                        "Calculation error for field '%s': %s",
                        rule.targetField(), e.getMessage());
                log.warn(errorMsg);
                errors.add(errorMsg);
            } catch (NumberFormatException e) {
                String errorMsg = String.format(
                        "Type coercion error for field '%s': cannot convert result to %s",
                        rule.targetField(), rule.resultType());
                log.warn(errorMsg);
                errors.add(errorMsg);
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private MapContext buildContext(Agreement agreement) {
        MapContext context = new MapContext();

        // Add all JSONB attributes first (lower precedence)
        Map<String, Object> attributes = agreement.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                context.set(entry.getKey(), toBigDecimalIfNumeric(entry.getValue()));
            }
        }

        // Add top-level agreement fields (higher precedence — overwrites)
        context.set("totalPremium", agreement.getTotalPremium());
        context.set("totalSumInsured", agreement.getTotalSumInsured());
        context.set("currencyCode", agreement.getCurrencyCode());
        context.set("inceptionDate", agreement.getInceptionDate());
        context.set("expiryDate", agreement.getExpiryDate());
        context.set("marketContext",
                agreement.getMarketContext() != null ? agreement.getMarketContext().name() : null);

        return context;
    }

    private Object toBigDecimalIfNumeric(Object value) {
        if (value instanceof Integer i) return new BigDecimal(i);
        if (value instanceof Long l) return new BigDecimal(l);
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Float f) return BigDecimal.valueOf(f);
        return value;
    }

    private Object coerceResult(Object rawResult, CalculationRule rule) {
        return switch (rule.resultType()) {
            case "CURRENCY", "NUMBER", "PERCENTAGE" -> {
                BigDecimal bd;
                if (rawResult instanceof BigDecimal b) {
                    bd = b;
                } else if (rawResult instanceof Number n) {
                    bd = new BigDecimal(n.toString());
                } else {
                    bd = new BigDecimal(rawResult.toString());
                }
                yield bd.setScale(rule.scale(), RoundingMode.HALF_UP);
            }
            case "BOOLEAN" -> {
                if (rawResult instanceof Boolean) yield rawResult;
                yield Boolean.parseBoolean(rawResult.toString());
            }
            default -> rawResult;
        };
    }

    private void writeResult(Agreement agreement, String targetField, Object value) {
        switch (targetField) {
            case "totalPremium" -> {
                if (value instanceof BigDecimal bd) agreement.setTotalPremium(bd);
            }
            case "totalSumInsured" -> {
                if (value instanceof BigDecimal bd) agreement.setTotalSumInsured(bd);
            }
            default -> agreement.setAttribute(targetField, value);
        }
    }

    private JexlPermissions createPermissions() {
        return JexlPermissions.parse(
                "java.lang { Math {} }",
                "java.math { BigDecimal {} }",
                "java.lang { String {} }"
        );
    }
}
