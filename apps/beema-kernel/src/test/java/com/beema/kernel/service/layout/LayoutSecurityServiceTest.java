package com.beema.kernel.service.layout;

import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutSecurityServiceTest {

    private LayoutSecurityService securityService;
    private JexlExpressionEngine jexlEngine;

    @BeforeEach
    void setUp() {
        jexlEngine = new JexlExpressionEngine();
        securityService = new LayoutSecurityService(jexlEngine);
    }

    @Test
    void applySecurityTrimming_shouldHideSectionForNonUnderwriter() {
        // Given
        Map<String, Object> layout = Map.of(
            "title", "Test Layout",
            "sections", List.of(
                Map.of(
                    "id", "premium-section",
                    "title", "Premium",
                    "visible_if", "user.role == \"underwriter\"",
                    "fields", List.of()
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, Map.of());

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        assertThat(sections).isEmpty();
    }

    @Test
    void applySecurityTrimming_shouldShowSectionForUnderwriter() {
        // Given
        Map<String, Object> layout = Map.of(
            "title", "Test Layout",
            "sections", List.of(
                Map.of(
                    "id", "premium-section",
                    "title", "Premium",
                    "visible_if", "user.role == \"underwriter\"",
                    "fields", List.of()
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "underwriter", "uw@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, Map.of());

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        assertThat(sections).hasSize(1);
    }

    @Test
    void applySecurityTrimming_shouldHideFieldBasedOnStatus() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "premium_field",
                            "visible_if", "status == \"DRAFT\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        Map<String, Object> dataContext = Map.of("status", "ISSUED");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, dataContext);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        assertThat(fields).isEmpty();
    }

    @Test
    void applySecurityTrimming_shouldMarkFieldAsReadonly() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "premium_field",
                            "visible_if", "true",
                            "editable_if", "user.role == \"underwriter\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, Map.of());

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        Map<String, Object> field = fields.get(0);

        assertThat(field.get("readonly")).isEqualTo(true);
        assertThat(field.get("editable_if")).isNull(); // Should be removed
    }

    @Test
    void applySecurityTrimming_shouldShowFieldForUnderwriter() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "premium_field",
                            "visible_if", "status == \"DRAFT\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "underwriter", "uw@test.com", "default");

        Map<String, Object> dataContext = Map.of("status", "DRAFT");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, dataContext);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).get("id")).isEqualTo("premium_field");
    }

    @Test
    void applySecurityTrimming_shouldMarkFieldAsEditableForUnderwriter() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "premium_field",
                            "visible_if", "true",
                            "editable_if", "user.role == \"underwriter\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "underwriter", "uw@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, Map.of());

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        Map<String, Object> field = fields.get(0);

        assertThat(field.get("readonly")).isEqualTo(false);
        assertThat(field.get("editable_if")).isNull(); // Should be removed
    }

    @Test
    void applySecurityTrimming_shouldHandleComplexExpression() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "(user.role == \"underwriter\" || user.role == \"admin\") && status != \"CANCELLED\"",
                    "fields", List.of()
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "admin", "admin@test.com", "default");

        Map<String, Object> dataContext = Map.of("status", "DRAFT");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, dataContext);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        assertThat(sections).hasSize(1);
    }

    @Test
    void applySecurityTrimming_shouldHideSectionForCancelledStatus() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "(user.role == \"underwriter\" || user.role == \"admin\") && status != \"CANCELLED\"",
                    "fields", List.of()
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "admin", "admin@test.com", "default");

        Map<String, Object> dataContext = Map.of("status", "CANCELLED");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, dataContext);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        assertThat(sections).isEmpty();
    }

    @Test
    void applySecurityTrimming_shouldRemoveJexlExpressionsFromFields() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "field1",
                            "visible_if", "true",
                            "editable_if", "true",
                            "required_if", "status == \"DRAFT\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, Map.of());

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        Map<String, Object> field = fields.get(0);

        // All JEXL expressions should be removed for security
        assertThat(field.get("visible_if")).isNull();
        assertThat(field.get("editable_if")).isNull();
        assertThat(field.get("required_if")).isNull();
    }

    @Test
    void applySecurityTrimming_shouldHandleNullDataContext() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "user.role == \"user\"",
                    "fields", List.of()
                )
            )
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, null);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        assertThat(sections).hasSize(1);
    }

    @Test
    void applySecurityTrimming_shouldHandleNoSections() {
        // Given
        Map<String, Object> layout = Map.of(
            "title", "Test Layout"
        );

        LayoutSecurityService.SecurityContext context =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, context, Map.of());

        // Then
        assertThat(trimmed.get("sections")).isNull();
    }

    @Test
    void applySecurityTrimming_shouldHandleEditableAfterApprovalByAdmin() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "vehicle_make",
                            "visible_if", "true",
                            "editable_if", "status == \"DRAFT\" || user.role == \"admin\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext adminContext =
            new LayoutSecurityService.SecurityContext("admin1", "admin", "admin@test.com", "default");

        Map<String, Object> dataContext = Map.of("status", "APPROVED");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, adminContext, dataContext);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        Map<String, Object> field = fields.get(0);

        // Admin should be able to edit even after approval
        assertThat(field.get("readonly")).isEqualTo(false);
    }

    @Test
    void applySecurityTrimming_shouldMakeFieldReadonlyAfterApprovalForUser() {
        // Given
        Map<String, Object> layout = Map.of(
            "sections", List.of(
                Map.of(
                    "id", "section1",
                    "visible_if", "true",
                    "fields", List.of(
                        Map.of(
                            "id", "vehicle_make",
                            "visible_if", "true",
                            "editable_if", "status == \"DRAFT\" || user.role == \"admin\""
                        )
                    )
                )
            )
        );

        LayoutSecurityService.SecurityContext userContext =
            new LayoutSecurityService.SecurityContext("user1", "user", "user@test.com", "default");

        Map<String, Object> dataContext = Map.of("status", "APPROVED");

        // When
        Map<String, Object> trimmed = securityService.applySecurityTrimming(layout, userContext, dataContext);

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmed.get("sections");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) sections.get(0).get("fields");
        Map<String, Object> field = fields.get(0);

        // User should not be able to edit after approval
        assertThat(field.get("readonly")).isEqualTo(true);
    }
}
