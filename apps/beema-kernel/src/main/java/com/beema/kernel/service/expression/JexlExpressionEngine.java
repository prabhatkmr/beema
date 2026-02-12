package com.beema.kernel.service.expression;

import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Sandboxed JEXL Expression Engine
 *
 * Safely evaluates JEXL expressions against record data with strict security:
 * - Blocks access to java.lang.System, java.io.File, java.lang.Runtime
 * - Blocks all reflection, classloading, and process execution
 * - Only allows: Math, BigDecimal, String, basic arithmetic
 *
 * Thread-safe singleton engine.
 */
@Service
public class JexlExpressionEngine {

    private static final Logger log = LoggerFactory.getLogger(JexlExpressionEngine.class);

    private final JexlEngine jexlEngine;

    public JexlExpressionEngine() {
        this.jexlEngine = new JexlBuilder()
                .strict(false)        // Allows null propagation (null + 5 = null, not error)
                .silent(true)         // Silent mode: undefined variables return null instead of throwing
                .safe(false)          // Safe=false allows proper null handling in arithmetic
                .permissions(createSandboxPermissions())
                .create();

        log.info("JexlExpressionEngine initialized with sandboxed permissions");
    }

    /**
     * Evaluates a JEXL expression against a record Map.
     *
     * @param record Data context (e.g., {"rate": 0.05, "limit": 100000})
     * @param script JEXL expression (e.g., "rate * limit")
     * @return Evaluated result, or null if expression is invalid/returns null
     * @throws ExpressionEvaluationException if expression syntax is invalid or evaluation fails
     */
    public Object evaluate(Map<String, Object> record, String script) {
        if (script == null || script.isBlank()) {
            throw new ExpressionEvaluationException("Expression script cannot be null or empty");
        }

        // PRE-VALIDATION: Block dangerous patterns before JEXL parsing
        validateSafeExpression(script);

        try {
            // Create JEXL context from record with security sandbox
            JexlContext context = createSecuredContext(record);

            // Compile and evaluate expression
            JexlExpression expression = jexlEngine.createExpression(script);
            Object result = expression.evaluate(context);

            log.debug("Evaluated expression '{}' => {}", script, result);
            return result;

        } catch (JexlException.Parsing e) {
            throw new ExpressionEvaluationException(
                    String.format("Syntax error in expression '%s': %s", script, e.getMessage()), e);
        } catch (IllegalAccessError e) {
            // Thrown by NamespaceBlockingContext when trying to access java.* packages
            throw new ExpressionEvaluationException(
                    String.format("Security violation in expression '%s': %s", script, e.getMessage()), e);
        } catch (JexlException e) {
            throw new ExpressionEvaluationException(
                    String.format("Evaluation error for expression '%s': %s", script, e.getMessage()), e);
        }
    }

    /**
     * Evaluates expression and converts result to BigDecimal with specified scale.
     * Useful for monetary/percentage calculations.
     *
     * @param record Data context
     * @param script JEXL expression
     * @param scale Decimal places (e.g., 4 for currency)
     * @return Result as BigDecimal with proper scale
     */
    public BigDecimal evaluateAsDecimal(Map<String, Object> record, String script, int scale) {
        Object result = evaluate(record, script);

        if (result == null) {
            return null;
        }

        try {
            BigDecimal bd;
            if (result instanceof BigDecimal b) {
                bd = b;
            } else if (result instanceof Number n) {
                bd = new BigDecimal(n.toString());
            } else {
                bd = new BigDecimal(result.toString());
            }
            return bd.setScale(scale, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new ExpressionEvaluationException(
                    String.format("Cannot convert result '%s' to BigDecimal", result), e);
        }
    }

    /**
     * Evaluates expression and converts result to Boolean.
     *
     * @param record Data context
     * @param script JEXL expression (e.g., "age >= 18")
     * @return Result as Boolean
     */
    public Boolean evaluateAsBoolean(Map<String, Object> record, String script) {
        Object result = evaluate(record, script);

        if (result == null) {
            return null;
        }

        if (result instanceof Boolean b) {
            return b;
        }

        return Boolean.parseBoolean(result.toString());
    }

    /**
     * Evaluates expression and converts result to String.
     *
     * @param record Data context
     * @param script JEXL expression
     * @return Result as String, or null if result is null
     */
    public String evaluateAsString(Map<String, Object> record, String script) {
        Object result = evaluate(record, script);
        return result != null ? result.toString() : null;
    }

    /**
     * Validates expression syntax without evaluating it.
     *
     * @param script JEXL expression
     * @return true if syntax is valid, false otherwise
     */
    public boolean isValidSyntax(String script) {
        if (script == null || script.isBlank()) {
            return false;
        }

        try {
            jexlEngine.createExpression(script);
            return true;
        } catch (JexlException.Parsing e) {
            log.debug("Invalid expression syntax: '{}' - {}", script, e.getMessage());
            return false;
        }
    }

    /**
     * Converts Java types to JEXL-friendly types.
     * Integers are converted to Double to ensure decimal division (e.g., 500/12 = 41.666... not 41).
     * This is important for insurance calculations which require decimal precision.
     */
    private Object convertToJexlType(Object value) {
        if (value == null) {
            return null;
        }

        // Convert integers to Double to ensure decimal division
        // (e.g., 500 / 12 should be 41.6667, not 41)
        if (value instanceof Integer i) {
            return i.doubleValue();
        }
        if (value instanceof Long l) {
            return l.doubleValue();
        }

        // Keep other types as-is (Double, Float, String, Boolean, BigDecimal)
        return value;
    }

    /**
     * Validates that the expression doesn't contain dangerous patterns.
     * This is a defense-in-depth measure in addition to JexlPermissions.
     *
     * BLOCKED patterns:
     * - java.io.* (File, FileWriter, etc.)
     * - java.net.* (Socket, URL, etc.)
     * - java.nio.file.* (Files, Path, etc.)
     * - java.lang.Runtime, ProcessBuilder, Process
     * - java.lang.System (except Math which is allowed)
     * - java.lang.Class, ClassLoader (reflection)
     * - java.lang.reflect.*
     */
    private void validateSafeExpression(String script) {
        String normalized = script.toLowerCase().replaceAll("\\s+", "");

        // Check for dangerous class access patterns
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
                throw new ExpressionEvaluationException(
                        String.format("Expression contains blocked pattern '%s': %s", pattern, script));
            }
        }

        // Block "new" with fully qualified class names (except BigDecimal which is safe)
        if (normalized.contains("newjava.") && !normalized.contains("newjava.math.bigdecimal")) {
            throw new ExpressionEvaluationException(
                    "Expression cannot instantiate java.* classes (except BigDecimal): " + script);
        }
    }

    /**
     * Creates a secured JEXL context that blocks java.* package access.
     * This prevents expressions like "java.lang.System.exit(0)" from working.
     */
    private JexlContext createSecuredContext(Map<String, Object> record) {
        MapContext baseContext = new MapContext();
        if (record != null) {
            record.forEach((key, value) -> baseContext.set(key, convertToJexlType(value)));
        }

        // Wrap in a namespace-blocking context
        return new NamespaceBlockingContext(baseContext);
    }

    /**
     * JexlContext wrapper that blocks access to java.* namespaces.
     * This prevents dangerous expressions like "java.lang.System.exit(0)".
     */
    private static class NamespaceBlockingContext implements JexlContext, JexlContext.NamespaceResolver {
        private final MapContext delegate;

        NamespaceBlockingContext(MapContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object get(String name) {
            // Allow Math (safe class)
            if ("Math".equals(name)) {
                return Math.class;
            }

            // Block any variable name that looks like a Java package
            if (name != null && (name.startsWith("java.") || name.equals("java"))) {
                throw new IllegalAccessError("Access to java.* packages is blocked for security");
            }
            return delegate.get(name);
        }

        @Override
        public void set(String name, Object value) {
            delegate.set(name, value);
        }

        @Override
        public boolean has(String name) {
            if (name != null && (name.startsWith("java.") || name.equals("java"))) {
                return false;
            }
            return delegate.has(name);
        }

        @Override
        public Object resolveNamespace(String name) {
            // Block namespace resolution for java.* packages
            if (name != null && (name.startsWith("java") || name.equals("System") ||
                    name.equals("Runtime") || name.equals("Class") || name.equals("File"))) {
                throw new IllegalAccessError("Access to java.* packages is blocked for security");
            }
            return null; // No custom namespaces
        }
    }

    /**
     * Creates strict sandbox permissions blocking high-risk Java classes.
     *
     * Uses JEXL's RESTRICTED permissions which blocks all dangerous java.* classes:
     * - System, Runtime, Class, ClassLoader, Process, ProcessBuilder
     * - All java.io.* (File, FileWriter, etc.)
     * - All java.nio.file.*
     * - All java.net.* (Socket, URL, etc.)
     * - All java.lang.reflect.*
     *
     * RESTRICTED allows: Math, String, Number types, Collections
     * This provides safe expression evaluation for insurance calculations.
     */
    private JexlPermissions createSandboxPermissions() {
        // RESTRICTED is predefined to block all dangerous java.* packages
        // while allowing safe operations like Math, String, Number types
        return JexlPermissions.RESTRICTED;
    }

    /**
     * Exception thrown when expression evaluation fails.
     */
    public static class ExpressionEvaluationException extends RuntimeException {
        public ExpressionEvaluationException(String message) {
            super(message);
        }

        public ExpressionEvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
