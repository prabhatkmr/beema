package com.beema.kernel.service.security;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.service.metadata.MetadataRegistry;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WriteShieldService - Mass Assignment Protection")
class WriteShieldServiceTest {

    @Mock
    private MetadataRegistry metadataRegistry;

    @Mock
    private MetadataService metadataService;

    private WriteShieldService writeShieldService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TYPE_ID = UUID.randomUUID();
    private static final String TYPE_CODE = "MOTOR_PERSONAL";

    @BeforeEach
    void setUp() {
        writeShieldService = new WriteShieldService(metadataRegistry, metadataService);
    }

    @Nested
    @DisplayName("Customer Role - Mass Assignment Protection")
    class CustomerRoleTests {

        @Test
        @DisplayName("Should strip INTERNAL_ONLY field when Customer tries to update it")
        void shouldStripInternalOnlyFieldForCustomer() {
            // Given: Agreement type with both PUBLIC and INTERNAL_ONLY fields
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("vehicle_registration", "PUBLIC"),
                    createField("underwriting_notes", "INTERNAL_ONLY"),
                    createField("claims_history", "PUBLIC")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            // When: Customer tries to set both public and internal fields
            Map<String, Object> rawAttributes = new HashMap<>();
            rawAttributes.put("vehicle_registration", "AB12 CDE");
            rawAttributes.put("underwriting_notes", "HACKED: Customer trying to set internal notes!");
            rawAttributes.put("claims_history", "No claims");

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes,
                    TYPE_ID,
                    MarketContext.RETAIL,
                    "CUSTOMER",
                    TENANT_ID
            );

            // Then: INTERNAL_ONLY field is stripped, PUBLIC fields remain
            assertThat(sanitized).hasSize(2);
            assertThat(sanitized).containsKeys("vehicle_registration", "claims_history");
            assertThat(sanitized).doesNotContainKey("underwriting_notes");
            assertThat(sanitized.get("vehicle_registration")).isEqualTo("AB12 CDE");
            assertThat(sanitized.get("claims_history")).isEqualTo("No claims");
        }

        @Test
        @DisplayName("Should strip ADMIN_ONLY field for Customer role")
        void shouldStripAdminOnlyFieldForCustomer() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("premium", "PUBLIC"),
                    createField("override_reason", "ADMIN_ONLY")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = new HashMap<>();
            rawAttributes.put("premium", "500.00");
            rawAttributes.put("override_reason", "HACKED: Manual override");

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            );

            assertThat(sanitized).containsOnlyKeys("premium");
            assertThat(sanitized).doesNotContainKey("override_reason");
        }

        @Test
        @DisplayName("Should allow all PUBLIC fields for Customer role")
        void shouldAllowPublicFieldsForCustomer() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("vehicle_make", "PUBLIC"),
                    createField("vehicle_model", "PUBLIC"),
                    createField("vehicle_year", "PUBLIC")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "vehicle_make", "Toyota",
                    "vehicle_model", "Camry",
                    "vehicle_year", 2024
            );

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            );

            assertThat(sanitized).isEqualTo(rawAttributes);
        }
    }

    @Nested
    @DisplayName("Role Hierarchy - Progressive Permissions")
    class RoleHierarchyTests {

        @Test
        @DisplayName("ADMIN role should access ADMIN_ONLY, INTERNAL_ONLY, and PUBLIC fields")
        void adminShouldAccessAllFields() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("vehicle_registration", "PUBLIC"),
                    createField("underwriting_notes", "INTERNAL_ONLY"),
                    createField("override_reason", "ADMIN_ONLY")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "vehicle_registration", "AB12 CDE",
                    "underwriting_notes", "High risk",
                    "override_reason", "CEO approval"
            );

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "ADMIN", TENANT_ID
            );

            assertThat(sanitized).hasSize(3);
            assertThat(sanitized).isEqualTo(rawAttributes);
        }

        @Test
        @DisplayName("UNDERWRITER role should access INTERNAL_ONLY but not ADMIN_ONLY")
        void underwriterShouldAccessInternalButNotAdmin() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("premium", "PUBLIC"),
                    createField("underwriting_notes", "INTERNAL_ONLY"),
                    createField("override_reason", "ADMIN_ONLY")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "premium", "500.00",
                    "underwriting_notes", "Risk assessment complete",
                    "override_reason", "Manual override"
            );

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "UNDERWRITER", TENANT_ID
            );

            assertThat(sanitized).hasSize(2);
            assertThat(sanitized).containsKeys("premium", "underwriting_notes");
            assertThat(sanitized).doesNotContainKey("override_reason");
        }

        @Test
        @DisplayName("BROKER role should access BROKER_ONLY but not INTERNAL_ONLY")
        void brokerShouldAccessBrokerOnlyButNotInternal() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("premium", "PUBLIC"),
                    createField("broker_commission", "BROKER_ONLY"),
                    createField("underwriting_notes", "INTERNAL_ONLY")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "premium", "500.00",
                    "broker_commission", "50.00",
                    "underwriting_notes", "High risk"
            );

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "BROKER", TENANT_ID
            );

            assertThat(sanitized).hasSize(2);
            assertThat(sanitized).containsKeys("premium", "broker_commission");
            assertThat(sanitized).doesNotContainKey("underwriting_notes");
        }
    }

    @Nested
    @DisplayName("Validation Mode - Strict Enforcement")
    class ValidationModeTests {

        @Test
        @DisplayName("validateNoProtectedFields should throw exception when CUSTOMER tries INTERNAL_ONLY field")
        void shouldThrowExceptionWhenCustomerTriesInternalField() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("vehicle_registration", "PUBLIC"),
                    createField("underwriting_notes", "INTERNAL_ONLY")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "vehicle_registration", "AB12 CDE",
                    "underwriting_notes", "HACKED: Attempted write"
            );

            assertThatThrownBy(() -> writeShieldService.validateNoProtectedFields(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            ))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("protected fields")
                    .hasMessageContaining("underwriting_notes")
                    .hasMessageContaining("CUSTOMER");
        }

        @Test
        @DisplayName("validateNoProtectedFields should pass when only allowed fields present")
        void shouldPassValidationWhenOnlyAllowedFields() {
            setupAgreementType();
            List<FieldDefinition> fields = Arrays.asList(
                    createField("vehicle_make", "PUBLIC"),
                    createField("vehicle_model", "PUBLIC")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "vehicle_make", "Toyota",
                    "vehicle_model", "Camry"
            );

            // Should not throw
            writeShieldService.validateNoProtectedFields(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null attributes gracefully")
        void shouldHandleNullAttributes() {
            Map<String, Object> sanitized = writeShieldService.sanitize(
                    null, TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            );
            assertThat(sanitized).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty attributes")
        void shouldHandleEmptyAttributes() {
            Map<String, Object> sanitized = writeShieldService.sanitize(
                    new HashMap<>(), TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            );
            assertThat(sanitized).isEmpty();
        }

        @Test
        @DisplayName("Should allow fields not in metadata (extensible schema)")
        void shouldAllowUnknownFields() {
            setupAgreementType();
            List<FieldDefinition> fields = List.of(
                    createField("vehicle_registration", "PUBLIC")
            );
            when(metadataRegistry.getFieldsForType(TENANT_ID, TYPE_CODE, MarketContext.RETAIL))
                    .thenReturn(fields);

            Map<String, Object> rawAttributes = Map.of(
                    "vehicle_registration", "AB12 CDE",
                    "custom_field_123", "Custom value"
            );

            Map<String, Object> sanitized = writeShieldService.sanitize(
                    rawAttributes, TYPE_ID, MarketContext.RETAIL, "CUSTOMER", TENANT_ID
            );

            assertThat(sanitized).hasSize(2);
            assertThat(sanitized).containsKeys("vehicle_registration", "custom_field_123");
        }
    }

    // Helper methods
    private void setupAgreementType() {
        MetadataAgreementType type = new MetadataAgreementType();
        type.setTypeCode(TYPE_CODE);
        when(metadataService.getAgreementType(TYPE_ID)).thenReturn(Optional.of(type));
    }

    private FieldDefinition createField(String name, String visibility) {
        String uiComponent = String.format("{\"visibility\": \"%s\"}", visibility);
        return new FieldDefinition(
                UUID.randomUUID(),
                name,
                name.replace("_", " "),
                null,
                "STRING",
                "STANDARD",
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                uiComponent,
                0,
                null,
                null,
                null,
                null
        );
    }
}
