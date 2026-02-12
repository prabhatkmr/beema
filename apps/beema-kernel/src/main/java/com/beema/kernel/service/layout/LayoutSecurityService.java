package com.beema.kernel.service.layout;

import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Applies security trimming to layouts using JEXL expressions
 */
@Service
public class LayoutSecurityService {

    private static final Logger log = LoggerFactory.getLogger(LayoutSecurityService.class);

    private final JexlExpressionEngine jexlEngine;

    public LayoutSecurityService(JexlExpressionEngine jexlEngine) {
        this.jexlEngine = jexlEngine;
    }

    /**
     * Apply security trimming to layout based on user context and data
     */
    public Map<String, Object> applySecurityTrimming(
            Map<String, Object> layoutSchema,
            SecurityContext securityContext,
            Map<String, Object> dataContext
    ) {
        log.debug("Applying security trimming for user role: {}", securityContext.getRole());

        // Clone the schema to avoid modifying the original
        Map<String, Object> trimmedSchema = new HashMap<>(layoutSchema);

        // Build JEXL evaluation context
        Map<String, Object> jexlContext = buildJexlContext(securityContext, dataContext);

        // Trim sections
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trimmedSchema.get("sections");
        if (sections != null) {
            List<Map<String, Object>> visibleSections = sections.stream()
                .filter(section -> isSectionVisible(section, jexlContext))
                .map(section -> trimSection(section, jexlContext))
                .collect(Collectors.toList());

            trimmedSchema.put("sections", visibleSections);
        }

        return trimmedSchema;
    }

    private Map<String, Object> buildJexlContext(
            SecurityContext securityContext,
            Map<String, Object> dataContext
    ) {
        Map<String, Object> context = new HashMap<>();

        // Add user context
        context.put("user", Map.of(
            "id", securityContext.getUserId(),
            "role", securityContext.getRole(),
            "email", securityContext.getEmail(),
            "tenantId", securityContext.getTenantId()
        ));

        // Add data context (current object being edited)
        if (dataContext != null) {
            context.putAll(dataContext);
        }

        return context;
    }

    private boolean isSectionVisible(
            Map<String, Object> section,
            Map<String, Object> jexlContext
    ) {
        String visibleIf = (String) section.get("visible_if");

        if (visibleIf == null || visibleIf.trim().equals("true")) {
            return true;
        }

        try {
            Object result = jexlEngine.evaluate(jexlContext, visibleIf);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Failed to evaluate section visible_if expression: {} - Error: {}",
                visibleIf, e.getMessage());
            return false; // Hide section if expression fails
        }
    }

    private Map<String, Object> trimSection(
            Map<String, Object> section,
            Map<String, Object> jexlContext
    ) {
        Map<String, Object> trimmedSection = new HashMap<>(section);

        // Trim fields
        List<Map<String, Object>> fields = (List<Map<String, Object>>) section.get("fields");
        if (fields != null) {
            List<Map<String, Object>> visibleFields = fields.stream()
                .filter(field -> isFieldVisible(field, jexlContext))
                .map(field -> trimField(field, jexlContext))
                .collect(Collectors.toList());

            trimmedSection.put("fields", visibleFields);
        }

        return trimmedSection;
    }

    private boolean isFieldVisible(
            Map<String, Object> field,
            Map<String, Object> jexlContext
    ) {
        String visibleIf = (String) field.get("visible_if");

        if (visibleIf == null || visibleIf.trim().equals("true")) {
            return true;
        }

        try {
            Object result = jexlEngine.evaluate(jexlContext, visibleIf);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Failed to evaluate field visible_if expression: {} - Error: {}",
                visibleIf, e.getMessage());
            return false;
        }
    }

    private Map<String, Object> trimField(
            Map<String, Object> field,
            Map<String, Object> jexlContext
    ) {
        Map<String, Object> trimmedField = new HashMap<>(field);

        // Evaluate editable_if
        String editableIf = (String) field.get("editable_if");
        if (editableIf != null && !editableIf.trim().equals("true")) {
            try {
                Object result = jexlEngine.evaluate(jexlContext, editableIf);
                boolean isEditable = Boolean.TRUE.equals(result);
                trimmedField.put("readonly", !isEditable);
            } catch (Exception e) {
                log.warn("Failed to evaluate field editable_if expression: {} - Error: {}",
                    editableIf, e.getMessage());
                trimmedField.put("readonly", true); // Make read-only if expression fails
            }
        }

        // Remove JEXL expressions from client response (security)
        trimmedField.remove("visible_if");
        trimmedField.remove("editable_if");
        trimmedField.remove("required_if");

        return trimmedField;
    }

    public static class SecurityContext {
        private final String userId;
        private final String role;
        private final String email;
        private final String tenantId;

        public SecurityContext(String userId, String role, String email, String tenantId) {
            this.userId = userId;
            this.role = role;
            this.email = email;
            this.tenantId = tenantId;
        }

        public String getUserId() { return userId; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
        public String getTenantId() { return tenantId; }
    }
}
