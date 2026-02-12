package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.domain.metadata.MetadataAttribute;
import com.beema.kernel.repository.metadata.MetadataAgreementTypeRepository;
import com.beema.kernel.repository.metadata.MetadataAttributeRepository;
import com.beema.kernel.util.SchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for metadata management and validation.
 *
 * Responsibilities:
 * - Register and retrieve agreement type schemas
 * - Validate agreement attributes against schemas
 * - Manage attribute catalog
 * - Cache metadata for performance
 */
@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final MetadataAgreementTypeRepository agreementTypeRepository;
    private final MetadataAttributeRepository attributeRepository;
    private final SchemaValidator schemaValidator;

    public MetadataService(
        MetadataAgreementTypeRepository agreementTypeRepository,
        MetadataAttributeRepository attributeRepository,
        SchemaValidator schemaValidator
    ) {
        this.agreementTypeRepository = agreementTypeRepository;
        this.attributeRepository = attributeRepository;
        this.schemaValidator = schemaValidator;
    }

    // ========================================================================
    // Agreement Type Operations
    // ========================================================================

    /**
     * Register a new agreement type with schema.
     *
     * @param agreementType Agreement type to register
     * @return Saved agreement type
     */
    @Transactional
    @CacheEvict(value = "metadata-agreement-types", allEntries = true)
    public MetadataAgreementType registerAgreementType(MetadataAgreementType agreementType) {
        log.info("Registering agreement type: {} for market context: {}",
            agreementType.getTypeCode(), agreementType.getMarketContext());

        // Check if type already exists
        boolean exists = agreementTypeRepository.existsByTypeCodeAndMarketContext(
            agreementType.getTypeCode(),
            agreementType.getMarketContext()
        );

        if (exists) {
            // Increment schema version for new version
            List<MetadataAgreementType> versions = agreementTypeRepository
                .findAllVersionsByTypeCodeAndMarketContext(
                    agreementType.getTypeCode(),
                    agreementType.getMarketContext()
                );

            int nextVersion = versions.stream()
                .mapToInt(MetadataAgreementType::getSchemaVersion)
                .max()
                .orElse(0) + 1;

            agreementType.setSchemaVersion(nextVersion);
            log.info("Creating new schema version: {}", nextVersion);
        }

        return agreementTypeRepository.save(agreementType);
    }

    /**
     * Get agreement type by code and market context (latest version).
     *
     * Result is cached for performance.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @return Agreement type if found
     */
    @Cacheable(value = "metadata-agreement-types", key = "#typeCode + ':' + #marketContext")
    public Optional<MetadataAgreementType> getAgreementType(String typeCode, MarketContext marketContext) {
        log.debug("Fetching agreement type: {} for market: {}", typeCode, marketContext);
        return agreementTypeRepository.findLatestByTypeCodeAndMarketContext(typeCode, marketContext);
    }

    /**
     * Get agreement type by code, market context, and specific version.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @param schemaVersion Schema version
     * @return Agreement type if found
     */
    @Cacheable(value = "metadata-agreement-types",
        key = "#typeCode + ':' + #marketContext + ':v' + #schemaVersion")
    public Optional<MetadataAgreementType> getAgreementType(
        String typeCode,
        MarketContext marketContext,
        Integer schemaVersion
    ) {
        log.debug("Fetching agreement type: {} for market: {} version: {}",
            typeCode, marketContext, schemaVersion);
        return agreementTypeRepository.findByTypeCodeAndMarketContextAndSchemaVersion(
            typeCode, marketContext, schemaVersion
        );
    }

    /**
     * Get all active agreement types for a market context.
     *
     * @param marketContext Market context
     * @return List of active types
     */
    public List<MetadataAgreementType> getAgreementTypesByMarket(MarketContext marketContext) {
        log.debug("Fetching all agreement types for market: {}", marketContext);
        return agreementTypeRepository.findAllActiveByMarketContext(marketContext);
    }

    /**
     * Get all active agreement types across all markets.
     *
     * @return List of all active types
     */
    public List<MetadataAgreementType> getAllAgreementTypes() {
        log.debug("Fetching all active agreement types");
        return agreementTypeRepository.findAllActive();
    }

    /**
     * Deactivate an agreement type.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     */
    @Transactional
    @CacheEvict(value = "metadata-agreement-types", allEntries = true)
    public void deactivateAgreementType(String typeCode, MarketContext marketContext) {
        log.info("Deactivating agreement type: {} for market: {}", typeCode, marketContext);

        Optional<MetadataAgreementType> type = agreementTypeRepository
            .findLatestByTypeCodeAndMarketContext(typeCode, marketContext);

        type.ifPresent(t -> {
            t.setIsActive(false);
            agreementTypeRepository.save(t);
        });
    }

    // ========================================================================
    // Validation
    // ========================================================================

    /**
     * Validate agreement attributes against schema.
     *
     * @param typeCode Agreement type code
     * @param marketContext Market context
     * @param attributes Attributes to validate
     * @return Validation result
     */
    public SchemaValidator.ValidationResult validateAttributes(
        String typeCode,
        MarketContext marketContext,
        Map<String, Object> attributes
    ) {
        log.debug("Validating attributes for type: {} market: {}", typeCode, marketContext);

        // Get schema
        Optional<MetadataAgreementType> typeOpt = getAgreementType(typeCode, marketContext);

        if (typeOpt.isEmpty()) {
            return new SchemaValidator.ValidationResult(
                false,
                List.of("Agreement type not found: " + typeCode + " for market: " + marketContext)
            );
        }

        MetadataAgreementType type = typeOpt.get();

        // Validate against schema
        SchemaValidator.ValidationResult result = schemaValidator.validate(
            attributes,
            type.getAttributeSchema()
        );

        if (result.isValid()) {
            log.debug("Validation passed for type: {}", typeCode);
        } else {
            log.warn("Validation failed for type: {}. Errors: {}", typeCode, result.errors());
        }

        return result;
    }

    // ========================================================================
    // Attribute Operations
    // ========================================================================

    /**
     * Register a new attribute in the catalog.
     *
     * @param attribute Attribute to register
     * @return Saved attribute
     */
    @Transactional
    public MetadataAttribute registerAttribute(MetadataAttribute attribute) {
        log.info("Registering attribute: {} for market: {}",
            attribute.getAttributeKey(), attribute.getMarketContext());
        return attributeRepository.save(attribute);
    }

    /**
     * Get attribute by key and market context.
     *
     * @param attributeKey Attribute key
     * @param marketContext Market context
     * @return Attribute if found
     */
    public Optional<MetadataAttribute> getAttribute(String attributeKey, MarketContext marketContext) {
        return attributeRepository.findByAttributeKeyAndMarketContext(attributeKey, marketContext);
    }

    /**
     * Get all active attributes for a market context.
     *
     * @param marketContext Market context
     * @return List of active attributes
     */
    public List<MetadataAttribute> getAttributesByMarket(MarketContext marketContext) {
        return attributeRepository.findAllActiveByMarketContext(marketContext);
    }

    /**
     * Get all active attributes across all markets.
     *
     * @return List of all active attributes
     */
    public List<MetadataAttribute> getAllAttributes() {
        return attributeRepository.findAllActive();
    }
}
