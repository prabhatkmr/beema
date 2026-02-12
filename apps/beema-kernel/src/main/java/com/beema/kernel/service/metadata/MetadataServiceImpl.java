package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.repository.metadata.MetadataAgreementTypeRepository;
import com.beema.kernel.util.SchemaValidator;
import com.beema.kernel.util.SchemaValidator.ValidationResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
public class MetadataServiceImpl implements MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

    private final MetadataAgreementTypeRepository agreementTypeRepository;
    private final SchemaValidator schemaValidator;
    private final MetadataRegistry metadataRegistry;

    private final Cache<UUID, MetadataAgreementType> agreementTypeCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, MetadataAgreementType> agreementTypeByCodeCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    public MetadataServiceImpl(MetadataAgreementTypeRepository agreementTypeRepository,
                               SchemaValidator schemaValidator,
                               MetadataRegistry metadataRegistry) {
        this.agreementTypeRepository = agreementTypeRepository;
        this.schemaValidator = schemaValidator;
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    @Transactional
    public MetadataAgreementType registerAgreementType(MetadataAgreementType agreementType) {
        if (agreementTypeRepository.existsByTenantIdAndTypeCodeAndMarketContext(
                agreementType.getTenantId(), agreementType.getTypeCode(), agreementType.getMarketContext())) {
            throw new IllegalArgumentException(
                    "Agreement type already exists: " + agreementType.getTypeCode() +
                            " for market context " + agreementType.getMarketContext());
        }

        MetadataAgreementType saved = agreementTypeRepository.save(agreementType);
        log.info("Registered agreement type: {} [{}] for tenant {}",
                saved.getTypeCode(), saved.getMarketContext(), saved.getTenantId());

        agreementTypeCache.put(saved.getId(), saved);
        return saved;
    }

    @Override
    @Transactional
    public MetadataAgreementType updateAgreementType(UUID id, MetadataAgreementType update) {
        MetadataAgreementType existing = agreementTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agreement type not found: " + id));

        existing.setTypeName(update.getTypeName());
        existing.setDescription(update.getDescription());
        existing.setAttributeSchema(update.getAttributeSchema());
        existing.setValidationRules(update.getValidationRules());
        existing.setUiConfiguration(update.getUiConfiguration());
        existing.setCalculationRules(update.getCalculationRules());
        existing.setSchemaVersion(existing.getSchemaVersion() + 1);
        existing.setUpdatedBy(update.getUpdatedBy());

        MetadataAgreementType saved = agreementTypeRepository.save(existing);
        log.info("Updated agreement type: {} to schema version {}",
                saved.getTypeCode(), saved.getSchemaVersion());

        evictCaches(saved);
        return saved;
    }

    @Override
    public Optional<MetadataAgreementType> getAgreementType(UUID id) {
        MetadataAgreementType cached = agreementTypeCache.getIfPresent(id);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<MetadataAgreementType> result = agreementTypeRepository.findById(id);
        result.ifPresent(at -> agreementTypeCache.put(id, at));
        return result;
    }

    @Override
    public Optional<MetadataAgreementType> getAgreementTypeByCode(UUID tenantId, String typeCode,
                                                                   MarketContext marketContext) {
        String cacheKey = buildCodeCacheKey(tenantId, typeCode, marketContext);
        MetadataAgreementType cached = agreementTypeByCodeCache.getIfPresent(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<MetadataAgreementType> result = agreementTypeRepository
                .findByTenantIdAndTypeCodeAndMarketContext(tenantId, typeCode, marketContext);
        result.ifPresent(at -> {
            agreementTypeByCodeCache.put(cacheKey, at);
            agreementTypeCache.put(at.getId(), at);
        });
        return result;
    }

    @Override
    public List<MetadataAgreementType> getAgreementTypesByTenant(UUID tenantId) {
        return agreementTypeRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    @Override
    public List<MetadataAgreementType> getActiveAgreementTypesByTenantAndContext(UUID tenantId,
                                                                                 MarketContext marketContext) {
        return agreementTypeRepository.findByTenantIdAndMarketContextAndIsActiveTrue(tenantId, marketContext);
    }

    @Override
    public ValidationResult validateAgainstSchema(UUID agreementTypeId, Map<String, Object> attributes) {
        MetadataAgreementType agreementType = getAgreementType(agreementTypeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Agreement type not found for validation: " + agreementTypeId));

        Map<String, Object> schema = agreementType.getAttributeSchema();
        if (schema == null || schema.isEmpty()) {
            log.debug("No schema defined for agreement type {}, skipping validation", agreementTypeId);
            return ValidationResult.valid();
        }

        return schemaValidator.validate(attributes, schema);
    }

    @Override
    @Transactional
    public void deactivateAgreementType(UUID id) {
        MetadataAgreementType existing = agreementTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agreement type not found: " + id));

        existing.setIsActive(false);
        agreementTypeRepository.save(existing);
        log.info("Deactivated agreement type: {} [{}]", existing.getTypeCode(), existing.getMarketContext());

        evictCaches(existing);
    }

    private void evictCaches(MetadataAgreementType agreementType) {
        agreementTypeCache.invalidate(agreementType.getId());
        String cacheKey = buildCodeCacheKey(
                agreementType.getTenantId(), agreementType.getTypeCode(), agreementType.getMarketContext());
        agreementTypeByCodeCache.invalidate(cacheKey);
        metadataRegistry.evictForType(
                agreementType.getTenantId(), agreementType.getTypeCode(), agreementType.getMarketContext());
    }

    private String buildCodeCacheKey(UUID tenantId, String typeCode, MarketContext marketContext) {
        return tenantId + ":" + typeCode + ":" + marketContext;
    }
}
