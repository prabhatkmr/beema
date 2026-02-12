package com.beema.kernel.service.security;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.service.metadata.MetadataRegistry;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Write Shield Service - Protects against Mass Assignment attacks
 * by filtering incoming JSONB attributes against field-level permissions
 * defined in metadata.
 */
@Service
public class WriteShieldService {

    private static final Logger log = LoggerFactory.getLogger(WriteShieldService.class);

    private final MetadataRegistry metadataRegistry;
    private final MetadataService metadataService;

    public WriteShieldService(MetadataRegistry metadataRegistry, MetadataService metadataService) {
        this.metadataRegistry = metadataRegistry;
        this.metadataService = metadataService;
    }

    /**
     * Sanitizes incoming attributes by removing fields the user lacks permission to write.
     *
     * @param attributes Raw attributes from client request
     * @param agreementTypeId Agreement type ID
     * @param marketContext Market context
     * @param userRole User's role (CUSTOMER, BROKER, UNDERWRITER, ADMIN)
     * @param tenantId Tenant ID
     * @return Sanitized attributes with unauthorized fields removed
     */
    public Map<String, Object> sanitize(
            Map<String, Object> attributes,
            UUID agreementTypeId,
            MarketContext marketContext,
            String userRole,
            UUID tenantId
    ) {
        if (attributes == null || attributes.isEmpty()) {
            return new HashMap<>();
        }

        // Get agreement type to lookup typeCode
        MetadataAgreementType agreementType = metadataService.getAgreementType(agreementTypeId)
                .orElse(null);
        if (agreementType == null) {
            log.warn("Agreement type not found: {}. Allowing all fields (permissive fallback).", agreementTypeId);
            return new HashMap<>(attributes);
        }

        // Get field definitions for this agreement type
        List<FieldDefinition> fieldDefs = metadataRegistry.getFieldsForType(tenantId, agreementType.getTypeCode(), marketContext);
        if (fieldDefs == null || fieldDefs.isEmpty()) {
            log.warn("No field definitions found for type={}, context={}, tenant={}. Allowing all fields.",
                    agreementType.getTypeCode(), marketContext, tenantId);
            return new HashMap<>(attributes);
        }

        // Build permission map: fieldName -> allowedRoles
        Map<String, Set<String>> permissionMap = fieldDefs.stream()
                .collect(Collectors.toMap(
                        FieldDefinition::attributeName,
                        fd -> parseAllowedRoles(fd.uiComponent()) // visibility stored in uiComponent metadata
                ));

        // Filter attributes
        Map<String, Object> sanitized = new HashMap<>();
        List<String> blocked = new ArrayList<>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String fieldName = entry.getKey();
            Set<String> allowedRoles = permissionMap.get(fieldName);

            if (allowedRoles == null) {
                // Field not in metadata - allow (extensible schema)
                sanitized.put(fieldName, entry.getValue());
            } else if (allowedRoles.contains(userRole) || allowedRoles.contains("*")) {
                // User has permission
                sanitized.put(fieldName, entry.getValue());
            } else {
                // User lacks permission - block
                blocked.add(fieldName);
                log.info("WriteShield blocked field '{}' for role '{}' (allowed: {})",
                        fieldName, userRole, allowedRoles);
            }
        }

        if (!blocked.isEmpty()) {
            log.warn("WriteShield removed {} unauthorized fields for role '{}': {}",
                    blocked.size(), userRole, blocked);
        }

        return sanitized;
    }

    /**
     * Parses allowed roles from field metadata.
     * Format stored in uiComponent as JSON: {"visibility": "INTERNAL_ONLY"} or {"visibility": "PUBLIC"}
     *
     * Visibility levels:
     * - PUBLIC: All roles (*)
     * - CUSTOMER_EDITABLE: CUSTOMER, BROKER, UNDERWRITER, ADMIN
     * - BROKER_ONLY: BROKER, UNDERWRITER, ADMIN
     * - INTERNAL_ONLY: UNDERWRITER, ADMIN
     * - ADMIN_ONLY: ADMIN
     */
    private Set<String> parseAllowedRoles(String uiComponent) {
        // Simple parsing - in production, use Jackson
        if (uiComponent == null || !uiComponent.contains("visibility")) {
            return Set.of("*"); // Default: all roles
        }

        if (uiComponent.contains("PUBLIC")) {
            return Set.of("*");
        }
        if (uiComponent.contains("CUSTOMER_EDITABLE")) {
            return Set.of("CUSTOMER", "BROKER", "UNDERWRITER", "ADMIN");
        }
        if (uiComponent.contains("BROKER_ONLY")) {
            return Set.of("BROKER", "UNDERWRITER", "ADMIN");
        }
        if (uiComponent.contains("INTERNAL_ONLY")) {
            return Set.of("UNDERWRITER", "ADMIN");
        }
        if (uiComponent.contains("ADMIN_ONLY")) {
            return Set.of("ADMIN");
        }

        return Set.of("*"); // Default: permissive
    }

    /**
     * Validates that no protected fields are present in the attributes.
     * Throws exception if violations found.
     */
    public void validateNoProtectedFields(
            Map<String, Object> attributes,
            UUID agreementTypeId,
            MarketContext marketContext,
            String userRole,
            UUID tenantId
    ) {
        Map<String, Object> sanitized = sanitize(attributes, agreementTypeId, marketContext, userRole, tenantId);

        if (sanitized.size() < attributes.size()) {
            Set<String> blocked = new HashSet<>(attributes.keySet());
            blocked.removeAll(sanitized.keySet());
            throw new SecurityException(
                    String.format("Attempt to modify protected fields: %s (role: %s)", blocked, userRole)
            );
        }
    }
}
