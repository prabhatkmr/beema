package com.beema.kernel.service.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.repository.agreement.AgreementRepository;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.service.validation.ContextValidationService;
import com.beema.kernel.util.SchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AgreementService.
 *
 * Handles:
 * - Agreement CRUD with temporal versioning
 * - Schema validation via MetadataService
 * - Context-specific business rules validation
 * - Bitemporal queries
 */
@Service
@Transactional
public class AgreementServiceImpl implements AgreementService {

    private static final Logger log = LoggerFactory.getLogger(AgreementServiceImpl.class);

    private final AgreementRepository agreementRepository;
    private final MetadataService metadataService;
    private final ContextValidationService contextValidationService;
    private final ObjectMapper objectMapper;

    public AgreementServiceImpl(
        AgreementRepository agreementRepository,
        MetadataService metadataService,
        ContextValidationService contextValidationService,
        ObjectMapper objectMapper
    ) {
        this.agreementRepository = agreementRepository;
        this.metadataService = metadataService;
        this.contextValidationService = contextValidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Agreement createAgreement(Agreement agreement) {
        log.info("Creating agreement: {} for tenant: {}",
            agreement.getAgreementNumber(), agreement.getTenantId());

        // Validate attributes against schema
        validateAgreement(agreement);

        // Set temporal key if not set
        if (agreement.getTemporalKey() == null) {
            UUID id = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();
            agreement.setTemporalKey(new TemporalKey(id, now, now));
        }

        // Set defaults
        if (agreement.getStatus() == null) {
            agreement.setStatus(AgreementStatus.DRAFT);
        }

        if (agreement.getValidTo() == null) {
            agreement.setValidTo(OffsetDateTime.parse("9999-12-31T23:59:59Z"));
        }

        // Save
        Agreement saved = agreementRepository.save(agreement);
        log.info("Created agreement with ID: {}", saved.getId());

        return saved;
    }

    @Override
    public Agreement updateAgreement(
        UUID id,
        Map<String, Object> updates,
        OffsetDateTime effectiveFrom,
        String updatedBy
    ) {
        log.info("Updating agreement: {} effective from: {}", id, effectiveFrom);

        // Get current version
        Agreement current = agreementRepository.findCurrentById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + id));

        // Create new version
        Agreement newVersion = new Agreement();

        // Copy from current
        newVersion.setAgreementNumber(current.getAgreementNumber());
        newVersion.setAgreementTypeCode(current.getAgreementTypeCode());
        newVersion.setMarketContext(current.getMarketContext());
        newVersion.setStatus(current.getStatus());
        newVersion.setAttributes(new java.util.HashMap<>(current.getAttributes()));
        newVersion.setDataResidencyRegion(current.getDataResidencyRegion());
        newVersion.setTenantId(current.getTenantId());
        newVersion.setCreatedBy(current.getCreatedBy());

        // Apply updates
        if (updates.containsKey("status")) {
            newVersion.setStatus(AgreementStatus.valueOf(updates.get("status").toString()));
        }
        if (updates.containsKey("attributes")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> newAttributes = (Map<String, Object>) updates.get("attributes");
            newVersion.setAttributes(newAttributes);
        }

        // Set temporal key for new version
        OffsetDateTime transactionTime = OffsetDateTime.now();
        newVersion.setTemporalKey(new TemporalKey(id, effectiveFrom, transactionTime));
        newVersion.setValidTo(OffsetDateTime.parse("9999-12-31T23:59:59Z"));
        newVersion.setIsCurrent(true);
        newVersion.setUpdatedBy(updatedBy);

        // Validate new version
        validateAgreement(newVersion);

        // Close current version
        current.setIsCurrent(false);
        current.setValidTo(effectiveFrom);
        current.setUpdatedBy(updatedBy);
        agreementRepository.save(current);

        // Save new version
        Agreement saved = agreementRepository.save(newVersion);
        log.info("Created new version for agreement: {} with transaction time: {}",
            id, transactionTime);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agreement> getCurrentAgreement(UUID id) {
        log.debug("Fetching current agreement: {}", id);
        return agreementRepository.findCurrentById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agreement> getAgreementByNumber(String agreementNumber, String tenantId) {
        log.debug("Fetching agreement by number: {} for tenant: {}", agreementNumber, tenantId);
        return agreementRepository.findCurrentByAgreementNumber(agreementNumber, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agreement> getAgreementAsOf(
        UUID id,
        OffsetDateTime validTime,
        OffsetDateTime transactionTime
    ) {
        log.debug("Fetching agreement: {} as of valid time: {} transaction time: {}",
            id, validTime, transactionTime);
        return agreementRepository.findAsOf(id, validTime, transactionTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agreement> getAgreementHistory(UUID id) {
        log.debug("Fetching complete history for agreement: {}", id);
        return agreementRepository.findAuditTrail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Agreement> findAgreementsByTenantAndContext(
        String tenantId,
        MarketContext marketContext,
        Pageable pageable
    ) {
        log.debug("Finding agreements for tenant: {} market: {}", tenantId, marketContext);
        return agreementRepository.findAllCurrentByTenantAndContext(tenantId, marketContext, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agreement> findAgreementsByStatus(String tenantId, AgreementStatus status) {
        log.debug("Finding agreements for tenant: {} status: {}", tenantId, status);
        return agreementRepository.findAllCurrentByTenantAndStatus(tenantId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agreement> findByAttribute(String tenantId, Map<String, Object> attributes) {
        log.debug("Finding agreements for tenant: {} with attributes: {}", tenantId, attributes);

        try {
            // Convert attributes to JSON string for containment query
            String attributeJson = objectMapper.writeValueAsString(attributes);
            return agreementRepository.findByAttributeContainment(tenantId, attributeJson);
        } catch (Exception e) {
            log.error("Failed to convert attributes to JSON", e);
            throw new IllegalArgumentException("Invalid attributes format", e);
        }
    }

    @Override
    public Agreement changeStatus(
        UUID id,
        AgreementStatus newStatus,
        OffsetDateTime effectiveFrom,
        String updatedBy
    ) {
        log.info("Changing status for agreement: {} to: {} effective: {}",
            id, newStatus, effectiveFrom);

        return updateAgreement(
            id,
            Map.of("status", newStatus),
            effectiveFrom,
            updatedBy
        );
    }

    @Override
    public void validateAgreement(Agreement agreement) {
        log.debug("Validating agreement: {} type: {} market: {}",
            agreement.getAgreementNumber(),
            agreement.getAgreementTypeCode(),
            agreement.getMarketContext());

        List<String> allErrors = new ArrayList<>();

        // Step 1: JSON Schema validation
        SchemaValidator.ValidationResult schemaResult = metadataService.validateAttributes(
            agreement.getAgreementTypeCode(),
            agreement.getMarketContext(),
            agreement.getAttributes()
        );

        if (!schemaResult.isValid()) {
            log.warn("Schema validation failed: {}", schemaResult.errors());
            allErrors.addAll(schemaResult.errors());
        }

        // Step 2: Context-specific business rules validation
        ContextValidationService.ValidationResult contextResult =
            contextValidationService.validate(
                agreement.getMarketContext(),
                agreement.getAgreementTypeCode(),
                agreement.getAttributes()
            );

        if (!contextResult.isValid()) {
            log.warn("Context validation failed: {}", contextResult.errors());
            allErrors.addAll(contextResult.errors());
        }

        // Fail if any validation errors
        if (!allErrors.isEmpty()) {
            String errorMessage = "Agreement validation failed: " + String.join("; ", allErrors);
            log.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        log.debug("Agreement validation passed (schema + context rules)");
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTenantAndContext(String tenantId, MarketContext marketContext) {
        return agreementRepository.countCurrentByTenantAndContext(tenantId, marketContext);
    }
}
