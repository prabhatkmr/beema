package com.beema.kernel.service.metadata;

import com.beema.kernel.service.metadata.model.CompiledFieldDefinition;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-compiles JEXL expressions for calculated fields at cache load time.
 *
 * This eliminates the parse overhead on every evaluation, improving performance by ~10x:
 * - Without pre-compilation: parse (4-5ms) + evaluate (0.5ms) = ~5ms per field
 * - With pre-compilation: evaluate (0.5ms) only
 *
 * Uses the same secure sandbox as JexlExpressionEngine.
 */
@Component
public class JexlExpressionCompiler {

    private static final Logger log = LoggerFactory.getLogger(JexlExpressionCompiler.class);

    private final JexlEngine jexlEngine;

    public JexlExpressionCompiler() {
        this.jexlEngine = new JexlBuilder()
                .strict(false)        // Allows null coercion
                .silent(true)         // Silent mode for undefined variables
                .safe(false)          // Allows proper null handling
                .permissions(JexlPermissions.RESTRICTED)  // Security sandbox
                .create();

        log.info("JexlExpressionCompiler initialized with sandboxed permissions");
    }

    /**
     * Compiles a single field definition, returning CompiledFieldDefinition.
     *
     * @param field The field definition
     * @return CompiledFieldDefinition with pre-compiled expression (or null if not calculated)
     */
    public CompiledFieldDefinition compile(FieldDefinition field) {
        if (!field.isCalculated() || field.calculationScript() == null || field.calculationScript().isBlank()) {
            // Non-calculated field - no compilation needed
            return CompiledFieldDefinition.from(field);
        }

        String script = field.calculationScript().trim();

        // Validate safe expression (same as JexlExpressionEngine)
        try {
            validateSafeExpression(script);
        } catch (IllegalArgumentException e) {
            log.error("Field '{}' has unsafe calculation script: {}", field.attributeName(), e.getMessage());
            return CompiledFieldDefinition.from(field, null);  // Failed validation - no compilation
        }

        // Compile expression
        try {
            JexlExpression compiledExpression = jexlEngine.createExpression(script);
            log.debug("Compiled JEXL expression for field '{}': {}", field.attributeName(), script);
            return CompiledFieldDefinition.from(field, compiledExpression);
        } catch (JexlException.Parsing e) {
            log.error("Failed to compile JEXL expression for field '{}': {} - Script: {}",
                    field.attributeName(), e.getMessage(), script);
            return CompiledFieldDefinition.from(field, null);  // Compilation failed
        } catch (JexlException e) {
            log.error("JEXL error compiling field '{}': {}", field.attributeName(), e.getMessage());
            return CompiledFieldDefinition.from(field, null);
        }
    }

    /**
     * Compiles a list of field definitions.
     *
     * @param fields List of field definitions
     * @return List of compiled field definitions
     */
    public List<CompiledFieldDefinition> compileAll(List<FieldDefinition> fields) {
        List<CompiledFieldDefinition> compiled = new ArrayList<>(fields.size());
        int compiledCount = 0;
        int failedCount = 0;

        for (FieldDefinition field : fields) {
            CompiledFieldDefinition compiledField = compile(field);
            compiled.add(compiledField);

            if (compiledField.hasCompiledExpression()) {
                compiledCount++;
            } else if (field.isCalculated()) {
                failedCount++;
            }
        }

        if (compiledCount > 0) {
            log.info("Compiled {} JEXL expressions ({} failed)", compiledCount, failedCount);
        }

        return compiled;
    }

    /**
     * Validates that the expression doesn't contain dangerous patterns.
     * Same validation as JexlExpressionEngine for consistency.
     */
    private void validateSafeExpression(String script) {
        String normalized = script.toLowerCase().replaceAll("\\s+", "");

        String[] dangerousPatterns = {
                "java.io",
                "java.net",
                "java.nio",
                "runtime",
                "processbuilder",
                "system.exit",
                "system.getproperty",
                "system.getenv",
                "class.forname",
                "classloader",
                "reflect"
        };

        for (String pattern : dangerousPatterns) {
            if (normalized.contains(pattern)) {
                throw new IllegalArgumentException(
                        String.format("Expression contains blocked pattern '%s'", pattern));
            }
        }

        if (normalized.contains("newjava.") && !normalized.contains("newjava.math.bigdecimal")) {
            throw new IllegalArgumentException(
                    "Expression cannot instantiate java.* classes (except BigDecimal)");
        }
    }
}
