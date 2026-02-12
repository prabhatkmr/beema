package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.domain.metadata.MetadataAttribute;
import com.beema.kernel.domain.metadata.MetadataTypeAttribute;
import com.beema.kernel.repository.metadata.MetadataAgreementTypeRepository;
import com.beema.kernel.repository.metadata.MetadataAttributeRepository;
import com.beema.kernel.repository.metadata.MetadataTypeAttributeRepository;
import com.beema.kernel.service.metadata.model.CompiledFieldDefinition;
import com.beema.kernel.service.metadata.model.CompiledObjectDefinition;
import com.beema.kernel.service.metadata.model.FieldDefinition;
import com.beema.kernel.service.metadata.model.LayoutDefinition;
import com.beema.kernel.service.metadata.model.LayoutSection;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MetadataRegistryImpl implements MetadataRegistry {

    private static final Logger log = LoggerFactory.getLogger(MetadataRegistryImpl.class);

    private final MetadataAgreementTypeRepository agreementTypeRepository;
    private final MetadataAttributeRepository attributeRepository;
    private final MetadataTypeAttributeRepository typeAttributeRepository;
    private final JexlExpressionCompiler jexlCompiler;

    // ===== NEW: Primary cache for CompiledObjectDefinition =====
    private final Cache<String, CompiledObjectDefinition> compiledDefinitionCache = Caffeine.newBuilder()
            .maximumSize(500)           // Each entry contains ALL metadata for a type
            .expireAfterWrite(4, TimeUnit.HOURS)  // Longer TTL since this is expensive to build
            .recordStats()
            .build();

    // ===== Legacy caches (kept for backwards compatibility) =====
    private final Cache<String, List<FieldDefinition>> fieldsByTypeCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, FieldDefinition> fieldByNameCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, LayoutDefinition> layoutByTypeCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, List<FieldDefinition>> calculatedFieldsCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .recordStats()
            .build();

    public MetadataRegistryImpl(MetadataAgreementTypeRepository agreementTypeRepository,
                                MetadataAttributeRepository attributeRepository,
                                MetadataTypeAttributeRepository typeAttributeRepository,
                                JexlExpressionCompiler jexlCompiler) {
        this.agreementTypeRepository = agreementTypeRepository;
        this.attributeRepository = attributeRepository;
        this.typeAttributeRepository = typeAttributeRepository;
        this.jexlCompiler = jexlCompiler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("MetadataRegistry: preloading metadata caches...");
        refreshAll();
        log.info("MetadataRegistry: preload complete");
    }

    // -----------------------------------------------------------------------
    // NEW: Compiled Object Definition (primary API)
    // -----------------------------------------------------------------------

    @Override
    public Optional<CompiledObjectDefinition> getCompiledDefinition(UUID tenantId, String typeCode, MarketContext marketContext) {
        String key = buildTypeKey(tenantId, typeCode, marketContext);

        // Check cache first
        CompiledObjectDefinition cached = compiledDefinitionCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache HIT for compiled definition: {}", key);
            return Optional.of(cached);
        }

        // Cache MISS - load and compile
        log.debug("Cache MISS for compiled definition: {}", key);
        Optional<CompiledObjectDefinition> compiled = loadAndCompileDefinition(tenantId, typeCode, marketContext);
        compiled.ifPresent(def -> compiledDefinitionCache.put(key, def));

        return compiled;
    }

    // -----------------------------------------------------------------------
    // Legacy Field lookups (for backwards compatibility)
    // -----------------------------------------------------------------------

    @Override
    public List<FieldDefinition> getFieldsForType(UUID tenantId, String typeCode, MarketContext marketContext) {
        String key = buildTypeKey(tenantId, typeCode, marketContext);
        List<FieldDefinition> cached = fieldsByTypeCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        List<FieldDefinition> fields = loadFieldsForType(tenantId, typeCode, marketContext);
        fieldsByTypeCache.put(key, fields);
        return fields;
    }

    @Override
    public Optional<FieldDefinition> getField(UUID tenantId, String attributeName, MarketContext marketContext) {
        String key = buildFieldKey(tenantId, attributeName, marketContext);
        FieldDefinition cached = fieldByNameCache.getIfPresent(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<FieldDefinition> field = loadField(tenantId, attributeName, marketContext);
        field.ifPresent(f -> fieldByNameCache.put(key, f));
        return field;
    }

    @Override
    public List<FieldDefinition> getCalculatedFields(UUID tenantId, String typeCode, MarketContext marketContext) {
        String key = buildTypeKey(tenantId, typeCode, marketContext);
        List<FieldDefinition> cached = calculatedFieldsCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        List<FieldDefinition> allFields = getFieldsForType(tenantId, typeCode, marketContext);
        List<FieldDefinition> calculated = topologicalSort(
                allFields.stream().filter(FieldDefinition::isCalculated).toList()
        );
        calculatedFieldsCache.put(key, calculated);
        return calculated;
    }

    // -----------------------------------------------------------------------
    // Layout lookups
    // -----------------------------------------------------------------------

    @Override
    public Optional<LayoutDefinition> getLayout(UUID tenantId, String typeCode, MarketContext marketContext) {
        String key = buildTypeKey(tenantId, typeCode, marketContext);
        LayoutDefinition cached = layoutByTypeCache.getIfPresent(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<LayoutDefinition> layout = loadLayout(tenantId, typeCode, marketContext);
        layout.ifPresent(l -> layoutByTypeCache.put(key, l));
        return layout;
    }

    // -----------------------------------------------------------------------
    // Cache management
    // -----------------------------------------------------------------------

    @Override
    public void refreshAll() {
        // Invalidate ALL caches
        compiledDefinitionCache.invalidateAll();
        fieldsByTypeCache.invalidateAll();
        fieldByNameCache.invalidateAll();
        layoutByTypeCache.invalidateAll();
        calculatedFieldsCache.invalidateAll();

        List<MetadataAgreementType> allTypes = agreementTypeRepository.findAll().stream()
                .filter(MetadataAgreementType::getIsActive)
                .toList();

        int compiled = 0;
        int failed = 0;

        for (MetadataAgreementType type : allTypes) {
            try {
                String key = buildTypeKey(type.getTenantId(), type.getTypeCode(), type.getMarketContext());

                // NEW: Load and compile the full definition (primary cache)
                Optional<CompiledObjectDefinition> compiledDef = loadAndCompileDefinition(
                        type.getTenantId(), type.getTypeCode(), type.getMarketContext());

                if (compiledDef.isPresent()) {
                    compiledDefinitionCache.put(key, compiledDef.get());
                    compiled++;

                    // Also populate legacy caches for backwards compatibility
                    List<FieldDefinition> fields = compiledDef.get().allFields().stream()
                            .map(CompiledFieldDefinition::toFieldDefinition)
                            .toList();
                    fieldsByTypeCache.put(key, fields);

                    for (FieldDefinition field : fields) {
                        fieldByNameCache.put(
                                buildFieldKey(type.getTenantId(), field.attributeName(), type.getMarketContext()),
                                field);
                    }

                    List<FieldDefinition> calculated = compiledDef.get().calculatedFieldsSorted().stream()
                            .map(CompiledFieldDefinition::toFieldDefinition)
                            .toList();
                    calculatedFieldsCache.put(key, calculated);

                    if (compiledDef.get().hasLayout()) {
                        layoutByTypeCache.put(key, compiledDef.get().layout());
                    }
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.warn("Failed to preload cache for type {} [{}]: {}",
                        type.getTypeCode(), type.getMarketContext(), e.getMessage());
                failed++;
            }
        }

        log.info("MetadataRegistry refreshed: {} compiled definitions, {} failed, {} total fields cached",
                compiled, failed, fieldByNameCache.estimatedSize());
    }

    @Override
    public void refreshForType(UUID tenantId, String typeCode, MarketContext marketContext) {
        evictForType(tenantId, typeCode, marketContext);

        String key = buildTypeKey(tenantId, typeCode, marketContext);

        // NEW: Load and compile the full definition
        Optional<CompiledObjectDefinition> compiled = loadAndCompileDefinition(tenantId, typeCode, marketContext);
        if (compiled.isPresent()) {
            compiledDefinitionCache.put(key, compiled.get());

            // Also refresh legacy caches
            List<FieldDefinition> fields = compiled.get().allFields().stream()
                    .map(CompiledFieldDefinition::toFieldDefinition)
                    .toList();
            fieldsByTypeCache.put(key, fields);

            for (FieldDefinition field : fields) {
                fieldByNameCache.put(
                        buildFieldKey(tenantId, field.attributeName(), marketContext), field);
            }

            List<FieldDefinition> calculated = compiled.get().calculatedFieldsSorted().stream()
                    .map(CompiledFieldDefinition::toFieldDefinition)
                    .toList();
            calculatedFieldsCache.put(key, calculated);

            if (compiled.get().hasLayout()) {
                layoutByTypeCache.put(key, compiled.get().layout());
            }

            log.info("MetadataRegistry refreshed for type {} [{}] - {} pre-compiled expressions",
                    typeCode, marketContext, compiled.get().compiledExpressionCount());
        } else {
            log.warn("Failed to refresh metadata for type {} [{}]", typeCode, marketContext);
        }
    }

    @Override
    public void evictForType(UUID tenantId, String typeCode, MarketContext marketContext) {
        String key = buildTypeKey(tenantId, typeCode, marketContext);

        // Get fields for eviction before invalidating
        CompiledObjectDefinition compiledDef = compiledDefinitionCache.getIfPresent(key);
        List<FieldDefinition> evictedFields = fieldsByTypeCache.getIfPresent(key);

        // Evict from ALL caches
        compiledDefinitionCache.invalidate(key);
        fieldsByTypeCache.invalidate(key);
        layoutByTypeCache.invalidate(key);
        calculatedFieldsCache.invalidate(key);

        // Evict individual field cache entries
        if (compiledDef != null) {
            for (CompiledFieldDefinition field : compiledDef.allFields()) {
                fieldByNameCache.invalidate(
                        buildFieldKey(tenantId, field.attributeName(), marketContext));
            }
        } else if (evictedFields != null) {
            for (FieldDefinition field : evictedFields) {
                fieldByNameCache.invalidate(
                        buildFieldKey(tenantId, field.attributeName(), marketContext));
            }
        }

        log.debug("Evicted metadata for type {} [{}]", typeCode, marketContext);
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // NEW: Primary cache stats
        stats.put("compiledDefinitions", formatStats(compiledDefinitionCache));

        // Count total pre-compiled expressions
        long totalCompiledExpressions = compiledDefinitionCache.asMap().values().stream()
                .mapToLong(CompiledObjectDefinition::compiledExpressionCount)
                .sum();
        stats.put("totalPreCompiledExpressions", totalCompiledExpressions);

        // Legacy cache stats
        stats.put("fieldsByType", formatStats(fieldsByTypeCache));
        stats.put("fieldByName", formatStats(fieldByNameCache));
        stats.put("layoutByType", formatStats(layoutByTypeCache));
        stats.put("calculatedFields", formatStats(calculatedFieldsCache));

        return stats;
    }

    // -----------------------------------------------------------------------
    // Loading logic
    // -----------------------------------------------------------------------

    /**
     * Loads and compiles a complete object definition with pre-compiled JEXL expressions.
     * This is the PRIMARY loader for the compiled definition cache.
     */
    private Optional<CompiledObjectDefinition> loadAndCompileDefinition(
            UUID tenantId, String typeCode, MarketContext marketContext) {

        // Load agreement type metadata
        Optional<MetadataAgreementType> typeOpt = agreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(tenantId, typeCode, marketContext);

        if (typeOpt.isEmpty()) {
            log.debug("No agreement type found for {}/{}:{}", tenantId, typeCode, marketContext);
            return Optional.empty();
        }

        MetadataAgreementType agreementType = typeOpt.get();

        // Load fields
        List<FieldDefinition> fields = loadFieldsForType(tenantId, typeCode, marketContext);
        if (fields.isEmpty()) {
            log.warn("No fields found for agreement type {}/{}:{}", tenantId, typeCode, marketContext);
            return Optional.empty();
        }

        // Compile fields (with JEXL pre-compilation)
        List<CompiledFieldDefinition> compiledFields = jexlCompiler.compileAll(fields);

        // Topologically sort calculated fields for evaluation order
        List<CompiledFieldDefinition> calculatedSorted = topologicalSortCompiled(
                compiledFields.stream().filter(CompiledFieldDefinition::isCalculated).toList()
        );

        // Load layout
        LayoutDefinition layout = loadLayout(tenantId, typeCode, marketContext).orElse(null);

        // Build type metadata
        Map<String, Object> typeMetadata = new HashMap<>();
        typeMetadata.put("schemaVersion", agreementType.getSchemaVersion());
        typeMetadata.put("isActive", agreementType.getIsActive());
        typeMetadata.put("attributeSchema", agreementType.getAttributeSchema());
        typeMetadata.put("validationRules", agreementType.getValidationRules());
        typeMetadata.put("calculationRules", agreementType.getCalculationRules());

        // Create compiled definition
        CompiledObjectDefinition compiled = new CompiledObjectDefinition(
                tenantId,
                typeCode,
                marketContext,
                agreementType.getTypeName(),  // Use getTypeName() not getDisplayName()
                agreementType.getDescription(),
                compiledFields,
                calculatedSorted,
                layout,
                typeMetadata,
                Instant.now()
        );

        log.info("Compiled object definition: {} ({} fields, {} pre-compiled expressions)",
                compiled.cacheKey(), compiled.fieldCount(), compiled.compiledExpressionCount());

        return Optional.of(compiled);
    }

    private List<FieldDefinition> loadFieldsForType(UUID tenantId, String typeCode, MarketContext marketContext) {
        Optional<MetadataAgreementType> typeOpt = agreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(tenantId, typeCode, marketContext);

        if (typeOpt.isEmpty()) {
            return Collections.emptyList();
        }

        MetadataAgreementType agreementType = typeOpt.get();
        List<MetadataTypeAttribute> typeAttributes = typeAttributeRepository
                .findByAgreementTypeId(agreementType.getId());

        if (typeAttributes.isEmpty()) {
            return Collections.emptyList();
        }

        // Load all referenced attributes in one query
        List<UUID> attributeIds = typeAttributes.stream()
                .map(MetadataTypeAttribute::getAttributeId)
                .toList();
        Map<UUID, MetadataAttribute> attributeMap = attributeRepository.findAllById(attributeIds).stream()
                .collect(Collectors.toMap(MetadataAttribute::getId, a -> a));

        List<FieldDefinition> fields = new ArrayList<>();
        for (MetadataTypeAttribute ta : typeAttributes) {
            MetadataAttribute attr = attributeMap.get(ta.getAttributeId());
            if (attr == null || !attr.getIsActive()) {
                continue;
            }
            fields.add(toFieldDefinition(attr, ta));
        }

        fields.sort(Comparator.comparingInt(FieldDefinition::uiOrder));
        return Collections.unmodifiableList(fields);
    }

    private Optional<FieldDefinition> loadField(UUID tenantId, String attributeName, MarketContext marketContext) {
        return attributeRepository.findByTenantIdAndAttributeNameAndMarketContext(
                tenantId, attributeName, marketContext
        ).filter(MetadataAttribute::getIsActive).map(attr -> toFieldDefinition(attr, null));
    }

    @SuppressWarnings("unchecked")
    private Optional<LayoutDefinition> loadLayout(UUID tenantId, String typeCode, MarketContext marketContext) {
        Optional<MetadataAgreementType> typeOpt = agreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(tenantId, typeCode, marketContext);

        if (typeOpt.isEmpty()) {
            return Optional.empty();
        }

        MetadataAgreementType agreementType = typeOpt.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> uiConfig = (Map<String, Object>) agreementType.getUiConfiguration();
        if (uiConfig == null || uiConfig.isEmpty()) {
            return Optional.empty();
        }

        String layoutType = (String) uiConfig.getOrDefault("layout", "tabbed");
        List<String> sectionNames = (List<String>) uiConfig.getOrDefault("sections", Collections.emptyList());

        // Load fields for this type to populate sections
        List<FieldDefinition> allFields = getFieldsForType(tenantId, typeCode, marketContext);

        // Group fields by section
        Map<String, List<FieldDefinition>> fieldsBySection = new LinkedHashMap<>();
        for (String section : sectionNames) {
            fieldsBySection.put(section, new ArrayList<>());
        }
        for (FieldDefinition field : allFields) {
            String section = field.sectionName();
            if (section != null && fieldsBySection.containsKey(section)) {
                fieldsBySection.get(section).add(field);
            }
        }

        List<LayoutSection> sections = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, List<FieldDefinition>> entry : fieldsBySection.entrySet()) {
            sections.add(new LayoutSection(
                    entry.getKey(),
                    order++,
                    Collections.unmodifiableList(entry.getValue())
            ));
        }

        return Optional.of(new LayoutDefinition(typeCode, marketContext, layoutType, Collections.unmodifiableList(sections)));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private FieldDefinition toFieldDefinition(MetadataAttribute attr, MetadataTypeAttribute override) {
        boolean isRequired = attr.getIsRequired();
        Map<String, Object> defaultValue = (Map<String, Object>) attr.getDefaultValue();
        int uiOrder = attr.getUiOrder();
        String sectionName = null;

        if (override != null) {
            if (override.getIsRequiredOverride() != null) {
                isRequired = override.getIsRequiredOverride();
            }
            if (override.getDefaultValueOverride() != null) {
                defaultValue = override.getDefaultValueOverride();
            }
            if (override.getUiOrderOverride() != null) {
                uiOrder = override.getUiOrderOverride();
            }
            sectionName = override.getSectionName();
        }

        return new FieldDefinition(
                attr.getId(),
                attr.getAttributeName(),
                attr.getDisplayName(),
                attr.getDescription(),
                attr.getDataType(),
                attr.getFieldType(),
                attr.getValidationPattern(),
                attr.getMinValue(),
                attr.getMaxValue(),
                (Map<String, Object>) attr.getAllowedValues(),
                defaultValue,
                isRequired,
                attr.getIsSearchable(),
                attr.getUiComponent(),
                uiOrder,
                attr.getCategory(),
                sectionName,
                attr.getCalculationScript(),
                attr.getDependsOn()
        );
    }

    /**
     * Topological sort for CompiledFieldDefinition (with pre-compiled expressions).
     */
    private List<CompiledFieldDefinition> topologicalSortCompiled(List<CompiledFieldDefinition> calculatedFields) {
        if (calculatedFields.isEmpty()) {
            return calculatedFields;
        }

        Map<String, CompiledFieldDefinition> byName = calculatedFields.stream()
                .collect(Collectors.toMap(CompiledFieldDefinition::attributeName, f -> f));

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<CompiledFieldDefinition> sorted = new ArrayList<>();

        for (CompiledFieldDefinition field : calculatedFields) {
            if (!visited.contains(field.attributeName())) {
                topoVisitCompiled(field.attributeName(), byName, visited, visiting, sorted);
            }
        }

        return Collections.unmodifiableList(sorted);
    }

    private void topoVisitCompiled(String name, Map<String, CompiledFieldDefinition> byName,
                                   Set<String> visited, Set<String> visiting, List<CompiledFieldDefinition> sorted) {
        if (visited.contains(name)) {
            return;
        }
        if (visiting.contains(name)) {
            log.warn("Circular dependency detected for calculated field: {}", name);
            return;
        }

        CompiledFieldDefinition field = byName.get(name);
        if (field == null) {
            return;
        }

        visiting.add(name);
        if (field.hasDependencies()) {
            for (String dep : field.dependsOn()) {
                topoVisitCompiled(dep, byName, visited, visiting, sorted);
            }
        }
        visiting.remove(name);
        visited.add(name);
        sorted.add(field);
    }

    private List<FieldDefinition> topologicalSort(List<FieldDefinition> calculatedFields) {
        if (calculatedFields.isEmpty()) {
            return calculatedFields;
        }

        Map<String, FieldDefinition> byName = calculatedFields.stream()
                .collect(Collectors.toMap(FieldDefinition::attributeName, f -> f));

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<FieldDefinition> sorted = new ArrayList<>();

        for (FieldDefinition field : calculatedFields) {
            if (!visited.contains(field.attributeName())) {
                topoVisit(field.attributeName(), byName, visited, visiting, sorted);
            }
        }

        return Collections.unmodifiableList(sorted);
    }

    private void topoVisit(String name, Map<String, FieldDefinition> byName,
                           Set<String> visited, Set<String> visiting, List<FieldDefinition> sorted) {
        if (visited.contains(name)) {
            return;
        }
        if (visiting.contains(name)) {
            log.warn("Circular dependency detected for calculated field: {}", name);
            return;
        }

        FieldDefinition field = byName.get(name);
        if (field == null) {
            return;
        }

        visiting.add(name);
        if (field.hasDependencies()) {
            for (String dep : field.dependsOn()) {
                topoVisit(dep, byName, visited, visiting, sorted);
            }
        }
        visiting.remove(name);
        visited.add(name);
        sorted.add(field);
    }

    private String buildTypeKey(UUID tenantId, String typeCode, MarketContext marketContext) {
        return tenantId + ":" + typeCode + ":" + marketContext;
    }

    private String buildFieldKey(UUID tenantId, String attributeName, MarketContext marketContext) {
        return tenantId + ":" + attributeName + ":" + marketContext;
    }

    private Map<String, Object> formatStats(Cache<?, ?> cache) {
        var stats = cache.stats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("size", cache.estimatedSize());
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
        result.put("evictionCount", stats.evictionCount());
        return result;
    }
}
