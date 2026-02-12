package com.beema.kernel.service.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JexlExpressionEngine - Sandboxed Expression Evaluation")
class JexlExpressionEngineTest {

    private JexlExpressionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new JexlExpressionEngine();
    }

    @Nested
    @DisplayName("Basic Arithmetic Operations")
    class BasicArithmeticTests {

        @Test
        @DisplayName("Should evaluate simple multiplication: rate * limit")
        void shouldEvaluateSimpleMultiplication() {
            Map<String, Object> record = Map.of(
                    "rate", 0.05,
                    "limit", 100000
            );

            // evaluate() returns native JEXL type (Double for double * int)
            BigDecimal result = engine.evaluateAsDecimal(record, "rate * limit", 2);

            assertThat(result).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("Should evaluate complex formula: (base + addon) * (1 + tax)")
        void shouldEvaluateComplexFormula() {
            Map<String, Object> record = Map.of(
                    "base", 1000,
                    "addon", 200,
                    "tax", 0.20
            );

            BigDecimal result = engine.evaluateAsDecimal(record, "(base + addon) * (1 + tax)", 2);

            assertThat(result).isEqualByComparingTo(new BigDecimal("1440.00"));
        }

        @Test
        @DisplayName("Should handle division with decimal precision")
        void shouldHandleDivision() {
            Map<String, Object> record = Map.of(
                    "premium", 500,
                    "installments", 12
            );

            BigDecimal result = engine.evaluateAsDecimal(record, "premium / installments", 4);

            assertThat(result).isEqualByComparingTo(new BigDecimal("41.6667"));
        }

        @Test
        @DisplayName("Should use Math operations: sqrt, pow")
        void shouldUseMathOperations() {
            Map<String, Object> record = Map.of("value", 16);

            Object sqrtResult = engine.evaluate(record, "Math.sqrt(value)");
            assertThat(((Number) sqrtResult).doubleValue()).isEqualTo(4.0);

            Object powResult = engine.evaluate(record, "Math.pow(2, 3)");
            assertThat(((Number) powResult).doubleValue()).isEqualTo(8.0);
        }
    }

    @Nested
    @DisplayName("Type Conversion")
    class TypeConversionTests {

        @Test
        @DisplayName("Should convert result to BigDecimal with scale")
        void shouldConvertToBigDecimal() {
            Map<String, Object> record = Map.of("rate", 0.00123456789);

            BigDecimal result = engine.evaluateAsDecimal(record, "rate * 1000", 4);

            assertThat(result).isEqualByComparingTo(new BigDecimal("1.2346"));
            assertThat(result.scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should convert result to Boolean")
        void shouldConvertToBoolean() {
            Map<String, Object> record = Map.of("age", 25, "premium", 500);

            Boolean ageCheck = engine.evaluateAsBoolean(record, "age >= 18");
            assertThat(ageCheck).isTrue();

            Boolean premiumCheck = engine.evaluateAsBoolean(record, "premium > 1000");
            assertThat(premiumCheck).isFalse();
        }

        @Test
        @DisplayName("Should convert result to String")
        void shouldConvertToString() {
            Map<String, Object> record = Map.of("first", "John", "last", "Doe");

            String result = engine.evaluateAsString(record, "first + ' ' + last");

            assertThat(result).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("Null and Edge Case Handling")
    class NullHandlingTests {

        @Test
        @DisplayName("Should handle null values gracefully (null coerced to 0)")
        void shouldHandleNullValues() {
            Map<String, Object> record = new HashMap<>();
            record.put("rate", null);
            record.put("limit", 100000);

            Object result = engine.evaluate(record, "rate * limit");

            // JEXL with strict=false: null is coerced to 0 in arithmetic
            assertThat(result).isNotNull();
            assertThat(((Number) result).doubleValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle missing variables (coerced to 0)")
        void shouldHandleMissingVariables() {
            Map<String, Object> record = Map.of("rate", 0.05);

            Object result = engine.evaluate(record, "rate * missing_variable");

            // JEXL silent mode: undefined variable treated as 0 in arithmetic
            assertThat(result).isNotNull();
            assertThat(((Number) result).doubleValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle division by zero (coerced to 0)")
        void shouldHandleDivisionByZero() {
            Map<String, Object> record = Map.of("value", 100, "divisor", 0);

            Object result = engine.evaluate(record, "value / divisor");

            // JEXL with strict=false: division by zero returns 0 (not Infinity)
            assertThat(result).isInstanceOf(Number.class);
            assertThat(((Number) result).doubleValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should evaluate constants even with null record")
        void shouldHandleNullRecord() {
            Object result = engine.evaluate(null, "5 + 3");

            // Constants return natural JEXL types (Integer for integer arithmetic)
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(Integer.class);
            assertThat(((Integer) result)).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Expression Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should validate correct expression syntax")
        void shouldValidateCorrectSyntax() {
            assertThat(engine.isValidSyntax("rate * limit")).isTrue();
            assertThat(engine.isValidSyntax("(base + addon) * (1 + tax)")).isTrue();
            assertThat(engine.isValidSyntax("Math.sqrt(value)")).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid expression syntax")
        void shouldRejectInvalidSyntax() {
            assertThat(engine.isValidSyntax("rate * * limit")).isFalse(); // Double operator
            assertThat(engine.isValidSyntax("(base + addon")).isFalse(); // Unclosed parenthesis
            assertThat(engine.isValidSyntax(null)).isFalse();
            assertThat(engine.isValidSyntax("")).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for null/empty script")
        void shouldThrowForNullScript() {
            Map<String, Object> record = Map.of("rate", 0.05);

            assertThatThrownBy(() -> engine.evaluate(record, null))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("cannot be null or empty");

            assertThatThrownBy(() -> engine.evaluate(record, "  "))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Security Sandbox - Blocked Operations")
    class SecuritySandboxTests {

        @Test
        @DisplayName("Should BLOCK access to System.exit()")
        void shouldBlockSystemExit() {
            Map<String, Object> record = Map.of();

            assertThatThrownBy(() -> engine.evaluate(record, "java.lang.System.exit(0)"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("System"); // Blocked by permissions
        }

        @Test
        @DisplayName("Should BLOCK access to System.getProperty()")
        void shouldBlockSystemGetProperty() {
            Map<String, Object> record = Map.of();

            assertThatThrownBy(() -> engine.evaluate(record, "java.lang.System.getProperty('user.home')"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class);
        }

        @Test
        @DisplayName("Should BLOCK access to Runtime.exec()")
        void shouldBlockRuntimeExec() {
            Map<String, Object> record = Map.of();

            assertThatThrownBy(() -> engine.evaluate(record, "java.lang.Runtime.getRuntime().exec('ls')"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("Runtime");
        }

        @Test
        @DisplayName("Should BLOCK file I/O operations")
        void shouldBlockFileIO() {
            Map<String, Object> record = Map.of();

            // Attempt to read file
            assertThatThrownBy(() -> engine.evaluate(record, "new java.io.File('/etc/passwd').exists()"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("File");

            // Attempt to write file
            assertThatThrownBy(() ->
                    engine.evaluate(record, "new java.io.FileWriter('/tmp/hack.txt')"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class);
        }

        @Test
        @DisplayName("Should BLOCK reflection and class loading")
        void shouldBlockReflection() {
            Map<String, Object> record = Map.of();

            // Attempt to load class
            assertThatThrownBy(() ->
                    engine.evaluate(record, "java.lang.Class.forName('java.lang.Runtime')"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class);

            // Attempt to get ClassLoader
            assertThatThrownBy(() ->
                    engine.evaluate(record, "this.getClass().getClassLoader()"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class);
        }

        @Test
        @DisplayName("Should BLOCK process execution")
        void shouldBlockProcessExecution() {
            Map<String, Object> record = Map.of();

            assertThatThrownBy(() ->
                    engine.evaluate(record, "new java.lang.ProcessBuilder('whoami').start()"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("ProcessBuilder");
        }

        @Test
        @DisplayName("Should BLOCK network access")
        void shouldBlockNetworkAccess() {
            Map<String, Object> record = Map.of();

            assertThatThrownBy(() ->
                    engine.evaluate(record, "new java.net.Socket('evil.com', 80)"))
                    .isInstanceOf(JexlExpressionEngine.ExpressionEvaluationException.class)
                    .hasMessageContaining("Socket");
        }

        @Test
        @DisplayName("Should ALLOW safe operations: Math, BigDecimal, String")
        void shouldAllowSafeOperations() {
            Map<String, Object> record = Map.of("value", 100);

            // Math operations - ALLOWED
            Object mathResult = engine.evaluate(record, "Math.sqrt(value)");
            assertThat(mathResult).isNotNull();

            // BigDecimal operations - ALLOWED
            Object bdResult = engine.evaluate(record, "new java.math.BigDecimal('123.45')");
            assertThat(bdResult).isInstanceOf(BigDecimal.class);

            // String operations - ALLOWED
            Object strResult = engine.evaluate(record, "'hello'.toUpperCase()");
            assertThat(strResult).isEqualTo("HELLO");
        }
    }

    @Nested
    @DisplayName("Real-World Insurance Calculations")
    class InsuranceCalculationsTests {

        @Test
        @DisplayName("Should calculate total premium: base + loading + tax")
        void shouldCalculateTotalPremium() {
            Map<String, Object> record = Map.of(
                    "base_premium", 1000,
                    "loading_factor", 0.15,
                    "tax_rate", 0.12
            );

            String formula = "base_premium * (1 + loading_factor) * (1 + tax_rate)";
            BigDecimal totalPremium = engine.evaluateAsDecimal(record, formula, 2);

            assertThat(totalPremium).isEqualByComparingTo(new BigDecimal("1288.00"));
        }

        @Test
        @DisplayName("Should calculate broker commission: premium * rate")
        void shouldCalculateBrokerCommission() {
            Map<String, Object> record = Map.of(
                    "premium", 5000,
                    "commission_rate", 0.10
            );

            BigDecimal commission = engine.evaluateAsDecimal(record, "premium * commission_rate", 2);

            assertThat(commission).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should calculate age-based premium adjustment")
        void shouldCalculateAgePremium() {
            Map<String, Object> record = Map.of(
                    "base_premium", 500,
                    "age", 65
            );

            // Age 65+: 50% surcharge
            String formula = "age >= 65 ? base_premium * 1.5 : base_premium";
            BigDecimal adjustedPremium = engine.evaluateAsDecimal(record, formula, 2);

            assertThat(adjustedPremium).isEqualByComparingTo(new BigDecimal("750.00"));
        }

        @Test
        @DisplayName("Should calculate claim reserve: sum_insured * reserve_percentage")
        void shouldCalculateClaimReserve() {
            Map<String, Object> record = Map.of(
                    "sum_insured", 1000000,
                    "reserve_percentage", 0.05
            );

            BigDecimal reserve = engine.evaluateAsDecimal(record,
                    "sum_insured * reserve_percentage", 2);

            assertThat(reserve).isEqualByComparingTo(new BigDecimal("50000.00"));
        }
    }
}
