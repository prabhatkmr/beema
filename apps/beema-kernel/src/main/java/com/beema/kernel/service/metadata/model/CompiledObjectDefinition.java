package com.beema.kernel.service.metadata.model;

import com.beema.kernel.domain.agreement.MarketContext;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unified, pre-compiled metadata definition for an agreement type.
 *
 * This is the cache value for MetadataRegistry. Contains:
 * - All fields (standard, derived, calculated) with pre-compiled JEXL expressions
 * - Layout definition
 * - Topologically sorted calculated fields for efficient evaluation
 * - Agreement type metadata
 *
 * Cached by key: (tenantId, typeCode, marketContext)
 *
 * Benefits:
 * - Single cache lookup gets ALL metadata for a type
 * - Pre-compiled JEXL expressions eliminate parse overhead (10x faster)
 * - Immutable and thread-safe
 */
public record CompiledObjectDefinition(
        UUID tenantId,
        String typeCode,
        MarketContext marketContext,
        String displayName,
        String description,
        List<CompiledFieldDefinition> allFields,
        List<CompiledFieldDefinition> calculatedFieldsSorted,  // Topologically sorted for evaluation order
        LayoutDefinition layout,
        Map<String, Object> typeMetadata,
        Instant compiledAt
) {

    /**
     * Thread-safe constructor ensuring immutability.
     */
    public CompiledObjectDefinition {
        allFields = Collections.unmodifiableList(allFields);
        calculatedFieldsSorted = Collections.unmodifiableList(calculatedFieldsSorted);
        typeMetadata = typeMetadata != null ? Collections.unmodifiableMap(typeMetadata) : Map.of();
    }

    /**
     * Gets a field by attribute name.
     */
    public Optional<CompiledFieldDefinition> getField(String attributeName) {
        return allFields.stream()
                .filter(f -> f.attributeName().equals(attributeName))
                .findFirst();
    }

    /**
     * Gets all standard (non-calculated) fields.
     */
    public List<CompiledFieldDefinition> getStandardFields() {
        return allFields.stream()
                .filter(CompiledFieldDefinition::isStandard)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets all calculated fields (unsorted).
     */
    public List<CompiledFieldDefinition> getCalculatedFields() {
        return allFields.stream()
                .filter(CompiledFieldDefinition::isCalculated)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets required fields only.
     */
    public List<CompiledFieldDefinition> getRequiredFields() {
        return allFields.stream()
                .filter(CompiledFieldDefinition::isRequired)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets searchable fields only.
     */
    public List<CompiledFieldDefinition> getSearchableFields() {
        return allFields.stream()
                .filter(CompiledFieldDefinition::isSearchable)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets fields in a specific section (for UI layout).
     */
    public List<CompiledFieldDefinition> getFieldsInSection(String sectionName) {
        return allFields.stream()
                .filter(f -> sectionName.equals(f.sectionName()))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Cache key for this definition.
     */
    public String cacheKey() {
        return tenantId + ":" + typeCode + ":" + marketContext;
    }

    /**
     * Total number of fields.
     */
    public int fieldCount() {
        return allFields.size();
    }

    /**
     * Number of pre-compiled expressions.
     */
    public long compiledExpressionCount() {
        return allFields.stream()
                .filter(CompiledFieldDefinition::hasCompiledExpression)
                .count();
    }

    /**
     * Whether this definition has a layout.
     */
    public boolean hasLayout() {
        return layout != null;
    }

    @Override
    public String toString() {
        return String.format("CompiledObjectDefinition[%s:%s:%s, fields=%d, compiled=%d, compiledAt=%s]",
                tenantId, typeCode, marketContext, fieldCount(), compiledExpressionCount(), compiledAt);
    }
}
