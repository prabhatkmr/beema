package com.beema.kernel.batch;

import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.springframework.batch.item.ItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Item processor that applies JEXL transformations.
 *
 * The processor script can modify the input map and return it,
 * or return null to filter out the item.
 *
 * Example scripts:
 * - "item.status = 'CLOSED'; item" - Update status field
 * - "item.premium = item.premium * 1.1; item" - Calculate new premium
 * - "item.claim_amount > 10000 ? item : null" - Filter high-value claims
 */
public class JexlItemProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(JexlItemProcessor.class);

    private final String processorScript;
    private final JexlExpressionEngine jexlEngine;

    public JexlItemProcessor(String processorScript, JexlExpressionEngine jexlEngine) {
        this.processorScript = processorScript;
        this.jexlEngine = jexlEngine;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> item) throws Exception {
        if (processorScript == null || processorScript.trim().isEmpty()) {
            return item; // Pass through if no script
        }

        try {
            // Evaluate JEXL script with item as context
            Object result = jexlEngine.evaluate(item, processorScript);

            // If script returns null, filter out this item
            if (result == null) {
                log.debug("Item filtered out by processor script");
                return null;
            }

            // If script returns a Map, use it as the processed item
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }

            // Otherwise, return original item (script may have mutated it in-place)
            return item;

        } catch (Exception e) {
            log.error("Error processing item with JEXL script: {}", processorScript, e);
            throw new RuntimeException("JEXL processing failed: " + e.getMessage(), e);
        }
    }
}
