package com.beema.kernel.service.integration;

import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookTransformationService {

    private static final Logger log = LoggerFactory.getLogger(WebhookTransformationService.class);

    private final JexlExpressionEngine jexlEngine;

    public WebhookTransformationService(JexlExpressionEngine jexlEngine) {
        this.jexlEngine = jexlEngine;
    }

    /**
     * Transforms an external webhook payload into an internal structure using a JEXL mapping script.
     *
     * The mapping script is a semicolon-delimited set of assignments in the form:
     *   targetField = sourceExpression; ...
     *
     * Example:
     *   agreementNumber = external_id;
     *   marketContext = 'RETAIL';
     *   totalPremium = premium_amount * 100;
     *   inceptionDate = start_date;
     *   expiryDate = end_date
     *
     * Each expression is evaluated against the incoming payload as context.
     *
     * @param payload       the external webhook JSON payload
     * @param mappingScript the JEXL transformation script
     * @return transformed Map matching internal field names
     * @throws WebhookTransformationException if transformation fails
     */
    public Map<String, Object> transform(Map<String, Object> payload, String mappingScript) {
        if (mappingScript == null || mappingScript.isBlank()) {
            throw new WebhookTransformationException("Mapping script is empty");
        }

        Map<String, Object> result = new HashMap<>();

        // Flatten nested maps so JEXL can access nested fields via dot notation
        Map<String, Object> context = flattenForContext(payload);

        String[] mappings = mappingScript.split(";");
        for (String mapping : mappings) {
            String trimmed = mapping.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int eqIndex = trimmed.indexOf('=');
            if (eqIndex <= 0) {
                throw new WebhookTransformationException(
                        "Invalid mapping entry (missing '='): " + trimmed);
            }

            String targetField = trimmed.substring(0, eqIndex).trim();
            String expression = trimmed.substring(eqIndex + 1).trim();

            try {
                Object value = jexlEngine.evaluate(context, expression);
                result.put(targetField, value);
                log.debug("Mapped {} = {} => {}", targetField, expression, value);
            } catch (JexlExpressionEngine.ExpressionEvaluationException e) {
                throw new WebhookTransformationException(
                        "Failed to evaluate mapping '" + targetField + " = " + expression + "': " + e.getMessage(), e);
            }
        }

        log.info("Transformed webhook payload: {} fields mapped", result.size());
        return result;
    }

    /**
     * Flattens nested Maps into dot-notation keys so JEXL expressions can reference
     * nested fields directly (e.g., "address.city" from {"address": {"city": "London"}}).
     * Also preserves top-level keys so both "address" (full map) and "address_city" work.
     */
    private Map<String, Object> flattenForContext(Map<String, Object> payload) {
        Map<String, Object> flat = new HashMap<>(payload);
        flattenRecursive("", payload, flat);
        return flat;
    }

    @SuppressWarnings("unchecked")
    private void flattenRecursive(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "_" + entry.getKey();
            if (entry.getValue() instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                target.put(key, nested);
                flattenRecursive(key, nested, target);
            } else {
                target.put(key, entry.getValue());
            }
        }
    }

    public static class WebhookTransformationException extends RuntimeException {
        public WebhookTransformationException(String message) {
            super(message);
        }

        public WebhookTransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
