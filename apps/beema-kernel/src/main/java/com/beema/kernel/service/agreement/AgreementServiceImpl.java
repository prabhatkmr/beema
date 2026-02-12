package com.beema.kernel.service.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.event.AgreementUpdatedEvent;
import com.beema.kernel.event.DomainEventPublisher;
import com.beema.kernel.event.PolicyBoundEvent;
import com.beema.kernel.repository.agreement.AgreementRepository;
import com.beema.kernel.service.expression.ExpressionEvaluator;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.util.SchemaValidator;
import com.beema.kernel.util.SchemaValidator.ValidationResult;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
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

@Service
@Transactional(readOnly = true)
public class AgreementServiceImpl implements AgreementService {

    private static final Logger log = LoggerFactory.getLogger(AgreementServiceImpl.class);

    private final AgreementRepository agreementRepository;
    private final MetadataService metadataService;
    private final ExpressionEvaluator expressionEvaluator;
    private final DomainEventPublisher eventPublisher;

    public AgreementServiceImpl(AgreementRepository agreementRepository,
                                MetadataService metadataService,
                                ExpressionEvaluator expressionEvaluator,
                                DomainEventPublisher eventPublisher) {
        this.agreementRepository = agreementRepository;
        this.metadataService = metadataService;
        this.expressionEvaluator = expressionEvaluator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Agreement createAgreement(Agreement agreement) {
        ValidationResult validation = validateAgreement(agreement);
        if (!validation.isValid()) {
            throw new ValidationException(
                    "Agreement attributes failed schema validation: " + String.join("; ", validation.errors()));
        }

        Agreement saved = agreementRepository.save(agreement);
        log.info("Created agreement {} [{}] for tenant {}",
                saved.getAgreementNumber(), saved.getMarketContext(), saved.getTenantId());

        // Publish PolicyBound event
        publishPolicyBoundEvent(saved);

        return saved;
    }

    @Override
    @Transactional
    public Agreement updateAgreement(UUID id, Agreement update) {
        Agreement current = agreementRepository.findCurrentById(id, update.getTenantId().toString())
                .orElseThrow(() -> new EntityNotFoundException("Agreement not found: " + id));

        ValidationResult validation = validateAgreement(update);
        if (!validation.isValid()) {
            throw new ValidationException(
                    "Agreement attributes failed schema validation: " + String.join("; ", validation.errors()));
        }

        // Close the current version
        OffsetDateTime now = OffsetDateTime.now();
        current.setValidTo(now);
        current.setIsCurrent(false);
        agreementRepository.save(current);

        // Create new bitemporal version
        update.setTemporalKey(new TemporalKey(id, now, now));
        update.setAgreementNumber(current.getAgreementNumber());
        update.setIsCurrent(true);
        update.setValidTo(null);

        Agreement saved = agreementRepository.save(update);
        log.info("Updated agreement {} - new version at {}",
                saved.getAgreementNumber(), saved.getTemporalKey().getTransactionTime());

        // Publish AgreementUpdated event
        publishAgreementUpdatedEvent(saved, "version_update");

        return saved;
    }

    @Override
    public Optional<Agreement> getCurrentAgreement(UUID id) {
        return agreementRepository.findCurrentById(id, null);
    }

    @Override
    public Optional<Agreement> getCurrentAgreementByNumber(String agreementNumber, String tenantId) {
        return agreementRepository.findCurrentByAgreementNumber(agreementNumber, tenantId);
    }

    @Override
    public Optional<Agreement> getAgreementAsOf(UUID id, String tenantId, OffsetDateTime validAt) {
        List<Agreement> results = agreementRepository.findAsOf(id, tenantId, validAt);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<Agreement> getAgreementHistory(UUID id, String tenantId) {
        return agreementRepository.findHistory(id, tenantId);
    }

    @Override
    public Page<Agreement> getAgreementsByTenantAndContext(String tenantId, MarketContext marketContext,
                                                           Pageable pageable) {
        return agreementRepository.findAllCurrentByTenantAndContext(tenantId, marketContext, pageable);
    }

    @Override
    public Page<Agreement> getAgreementsByTenantAndStatus(String tenantId, AgreementStatus status,
                                                           Pageable pageable) {
        return agreementRepository.findAllCurrentByTenantAndStatus(tenantId, status, pageable);
    }

    @Override
    public ValidationResult validateAgreement(Agreement agreement) {
        List<String> errors = new ArrayList<>();

        // Market-context-specific validation
        errors.addAll(validateMarketContextRules(agreement));

        // Evaluate calculated fields before schema validation
        errors.addAll(evaluateCalculatedFields(agreement));

        // Schema validation against metadata type
        if (agreement.getAgreementTypeId() != null) {
            ValidationResult schemaResult = metadataService.validateAgainstSchema(
                    agreement.getAgreementTypeId(), agreement.getAttributes());
            if (!schemaResult.isValid()) {
                errors.addAll(schemaResult.errors());
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private List<String> evaluateCalculatedFields(Agreement agreement) {
        if (agreement.getAgreementTypeId() == null) {
            return List.of();
        }

        Optional<MetadataAgreementType> typeOpt =
                metadataService.getAgreementType(agreement.getAgreementTypeId());
        if (typeOpt.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> calcRules = typeOpt.get().getCalculationRules();
        if (calcRules == null || calcRules.isEmpty()) {
            return List.of();
        }

        ValidationResult result = expressionEvaluator.evaluateCalculations(agreement, calcRules);
        return result.isValid() ? List.of() : result.errors();
    }

    private List<String> validateMarketContextRules(Agreement agreement) {
        List<String> errors = new ArrayList<>();

        if (agreement.getMarketContext() == null) {
            errors.add("Market context is required");
            return errors;
        }

        if (agreement.getInceptionDate() != null && agreement.getExpiryDate() != null
                && agreement.getInceptionDate().isAfter(agreement.getExpiryDate())) {
            errors.add("Inception date must be on or before expiry date");
        }

        switch (agreement.getMarketContext()) {
            case LONDON_MARKET -> {
                if (agreement.getExternalReference() == null || agreement.getExternalReference().isBlank()) {
                    errors.add("London Market agreements require an external reference (UMR)");
                }
                if (agreement.getTotalSumInsured() == null) {
                    errors.add("London Market agreements require total sum insured");
                }
            }
            case COMMERCIAL -> {
                if (agreement.getTotalPremium() != null && agreement.getTotalSumInsured() != null
                        && agreement.getTotalPremium().compareTo(agreement.getTotalSumInsured()) > 0) {
                    errors.add("Commercial agreement premium cannot exceed sum insured");
                }
            }
            case RETAIL -> {
                // Retail has standard validation - no additional rules
            }
        }

        return errors;
    }

    /**
     * Publish PolicyBound event to Inngest
     */
    private void publishPolicyBoundEvent(Agreement agreement) {
        try {
            PolicyBoundEvent event = new PolicyBoundEvent(
                agreement.getAgreementNumber(),
                agreement.getTemporalKey().getId().toString(),
                agreement.getMarketContext().name()
            );

            // Add additional context
            event.withData("agreementTypeId", agreement.getAgreementTypeId());
            event.withData("status", agreement.getStatus().name());
            if (agreement.getTotalPremium() != null) {
                event.withData("premium", agreement.getTotalPremium());
            }
            if (agreement.getInceptionDate() != null) {
                event.withData("inceptionDate", agreement.getInceptionDate().toString());
            }
            if (agreement.getExpiryDate() != null) {
                event.withData("expiryDate", agreement.getExpiryDate().toString());
            }

            eventPublisher.publishWithMetadata(
                event,
                agreement.getTenantId().toString(),
                "system",
                "system@beema.io"
            );
        } catch (Exception e) {
            log.error("Failed to publish PolicyBound event for agreement: {}",
                agreement.getAgreementNumber(), e);
        }
    }

    /**
     * Publish AgreementUpdated event to Inngest
     */
    private void publishAgreementUpdatedEvent(Agreement agreement, String changeType) {
        try {
            Map<String, Object> changes = Map.of(
                "status", agreement.getStatus().name(),
                "transactionTime", agreement.getTemporalKey().getTransactionTime().toString()
            );

            AgreementUpdatedEvent event = new AgreementUpdatedEvent(
                agreement.getTemporalKey().getId().toString(),
                changeType,
                changes
            );

            event.withData("agreementNumber", agreement.getAgreementNumber());
            event.withData("marketContext", agreement.getMarketContext().name());

            eventPublisher.publishWithMetadata(
                event,
                agreement.getTenantId().toString(),
                "system",
                "system@beema.io"
            );
        } catch (Exception e) {
            log.error("Failed to publish AgreementUpdated event for agreement: {}",
                agreement.getAgreementNumber(), e);
        }
    }
}
