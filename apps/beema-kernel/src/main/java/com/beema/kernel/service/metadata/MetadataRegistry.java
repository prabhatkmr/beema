package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.service.metadata.model.CompiledObjectDefinition;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import com.beema.kernel.service.metadata.model.LayoutDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MetadataRegistry {

    // ===== NEW: Compiled Object Definition (unified cache entry) =====

    /**
     * Gets the complete compiled object definition for an agreement type.
     * This is the PRIMARY cache entry containing all fields (with pre-compiled JEXL),
     * layout, and metadata.
     *
     * @param tenantId Tenant ID
     * @param typeCode Agreement type code (e.g., "MOTOR_PERSONAL")
     * @param marketContext Market context (RETAIL, COMMERCIAL, LONDON_MARKET)
     * @return CompiledObjectDefinition with pre-compiled JEXL expressions
     */
    Optional<CompiledObjectDefinition> getCompiledDefinition(UUID tenantId, String typeCode, MarketContext marketContext);

    // ===== Legacy Field lookups (for backwards compatibility) =====

    List<FieldDefinition> getFieldsForType(UUID tenantId, String typeCode, MarketContext marketContext);

    Optional<FieldDefinition> getField(UUID tenantId, String attributeName, MarketContext marketContext);

    List<FieldDefinition> getCalculatedFields(UUID tenantId, String typeCode, MarketContext marketContext);

    // Layout lookups
    Optional<LayoutDefinition> getLayout(UUID tenantId, String typeCode, MarketContext marketContext);

    // Cache management
    void refreshAll();

    void refreshForType(UUID tenantId, String typeCode, MarketContext marketContext);

    void evictForType(UUID tenantId, String typeCode, MarketContext marketContext);

    Map<String, Object> getCacheStats();
}
