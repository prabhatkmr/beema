package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.service.metadata.model.CompiledFieldDefinition;
import com.beema.kernel.service.metadata.model.CompiledObjectDefinition;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import org.apache.commons.jexl3.JexlExpression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Documentation and API tests for CompiledObjectDefinition.
 * Tests the caching model without requiring database setup.
 */
@DisplayName("Compiled Definition API Tests")
class CompiledDefinitionApiTest {

    @Test
    @DisplayName("CompiledFieldDefinition should wrap FieldDefinition with pre-compiled JEXL")
    void compiledFieldDefinitionShouldWrapWithJexl() {
        // Given: A basic field definition
        FieldDefinition basicField = new FieldDefinition(
                UUID.randomUUID(),
                "total_premium",
                "Total Premium",
                "Sum of base premium and taxes",
                "DECIMAL",
                "CALCULATED",
                null, null, null, null, null,
                false, false, null, 0,
                null, null,
                "base_premium * (1 + tax_rate)",
                List.of("base_premium", "tax_rate")
        );

        // When: Compiled with JEXL expression
        JexlExpression mockExpression = mock(JexlExpression.class);
        CompiledFieldDefinition compiled = CompiledFieldDefinition.from(basicField, mockExpression);

        // Then: Compiled field has pre-compiled expression
        assertThat(compiled.hasCompiledExpression()).isTrue();
        assertThat(compiled.compiledExpression()).isEqualTo(mockExpression);
        assertThat(compiled.calculationScript()).isEqualTo("base_premium * (1 + tax_rate)");
        assertThat(compiled.isCalculated()).isTrue();
    }

    @Test
    @DisplayName("CompiledObjectDefinition should bundle all metadata for a type")
    void compiledObjectDefinitionShouldBundleMetadata() {
        // Given: Fields with mixed types
        CompiledFieldDefinition standardField = createCompiledField(
                "premium", "STANDARD", null);
        CompiledFieldDefinition calculatedField = createCompiledField(
                "total_premium", "CALCULATED", mock(JexlExpression.class));

        List<CompiledFieldDefinition> allFields = List.of(standardField, calculatedField);
        List<CompiledFieldDefinition> calculatedOnly = List.of(calculatedField);

        // When: Create compiled definition
        UUID tenantId = UUID.randomUUID();
        CompiledObjectDefinition compiled = new CompiledObjectDefinition(
                tenantId,
                "MOTOR_PERSONAL",
                MarketContext.RETAIL,
                "Motor Personal",
                "Personal motor insurance",
                allFields,
                calculatedOnly,
                null,  // layout optional
                Collections.emptyMap(),
                Instant.now()
        );

        // Then: Definition provides all metadata in one object
        assertThat(compiled.fieldCount()).isEqualTo(2);
        assertThat(compiled.compiledExpressionCount()).isEqualTo(1);
        assertThat(compiled.getStandardFields()).hasSize(1);
        assertThat(compiled.getCalculatedFields()).hasSize(1);
        assertThat(compiled.cacheKey()).isEqualTo(tenantId + ":MOTOR_PERSONAL:RETAIL");
    }

    @Test
    @DisplayName("CompiledObjectDefinition should provide field lookup by name")
    void shouldLookupFieldByName() {
        // Given: Compiled definition with multiple fields
        CompiledFieldDefinition field1 = createCompiledField("premium", "STANDARD", null);
        CompiledFieldDefinition field2 = createCompiledField("tax", "STANDARD", null);

        CompiledObjectDefinition compiled = new CompiledObjectDefinition(
                UUID.randomUUID(),
                "MOTOR_PERSONAL",
                MarketContext.RETAIL,
                "Motor Personal",
                null,
                List.of(field1, field2),
                Collections.emptyList(),
                null,
                Collections.emptyMap(),
                Instant.now()
        );

        // When: Lookup by name
        Optional<CompiledFieldDefinition> found = compiled.getField("premium");

        // Then: Field found
        assertThat(found).isPresent();
        assertThat(found.get().attributeName()).isEqualTo("premium");
    }

    @Test
    @DisplayName("CompiledObjectDefinition should be immutable")
    void compiledObjectDefinitionShouldBeImmutable() {
        // Given: Mutable field list
        List<CompiledFieldDefinition> fields = new java.util.ArrayList<>();
        fields.add(createCompiledField("premium", "STANDARD", null));

        // When: Create compiled definition
        CompiledObjectDefinition compiled = new CompiledObjectDefinition(
                UUID.randomUUID(),
                "MOTOR_PERSONAL",
                MarketContext.RETAIL,
                "Motor Personal",
                null,
                fields,
                Collections.emptyList(),
                null,
                Collections.emptyMap(),
                Instant.now()
        );

        // Then: Fields list is immutable
        assertThatThrownBy(() -> compiled.allFields().add(createCompiledField("tax", "STANDARD", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("JexlExpressionCompiler should compile safe expressions only")
    void jexlCompilerShouldOnlyCompileSafeExpressions() {
        // This test documents the expected behavior - actual integration test
        // would be in MetadataRegistryCachePerformanceTest

        JexlExpressionCompiler compiler = new JexlExpressionCompiler();

        // Given: Field with safe calculation
        FieldDefinition safeField = new FieldDefinition(
                UUID.randomUUID(), "total", "Total", null,
                "DECIMAL", "CALCULATED", null, null, null, null, null,
                false, false, null, 0, null, null,
                "base * 1.2",  // Safe expression
                List.of("base")
        );

        // When: Compile
        CompiledFieldDefinition compiled = compiler.compile(safeField);

        // Then: Expression is compiled
        assertThat(compiled.hasCompiledExpression()).isTrue();
    }

    private CompiledFieldDefinition createCompiledField(String name, String fieldType, JexlExpression expression) {
        return new CompiledFieldDefinition(
                UUID.randomUUID(), name, name, null,
                "DECIMAL", fieldType, null,
                null, null, null, null,
                false, false, null, 0,
                null, null, null, null,
                expression
        );
    }
}
