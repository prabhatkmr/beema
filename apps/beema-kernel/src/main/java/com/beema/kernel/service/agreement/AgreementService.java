package com.beema.kernel.service.agreement;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.util.SchemaValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementService {

    Agreement createAgreement(Agreement agreement);

    Agreement updateAgreement(UUID id, Agreement agreement);

    Optional<Agreement> getCurrentAgreement(UUID id);

    Optional<Agreement> getCurrentAgreementByNumber(String agreementNumber, String tenantId);

    Optional<Agreement> getAgreementAsOf(UUID id, String tenantId, OffsetDateTime validAt);

    List<Agreement> getAgreementHistory(UUID id, String tenantId);

    Page<Agreement> getAgreementsByTenantAndContext(String tenantId, MarketContext marketContext, Pageable pageable);

    Page<Agreement> getAgreementsByTenantAndStatus(String tenantId, AgreementStatus status, Pageable pageable);

    SchemaValidator.ValidationResult validateAgreement(Agreement agreement);
}
