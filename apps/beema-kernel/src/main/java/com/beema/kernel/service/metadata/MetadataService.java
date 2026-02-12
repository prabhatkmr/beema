package com.beema.kernel.service.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.util.SchemaValidator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetadataService {

    MetadataAgreementType registerAgreementType(MetadataAgreementType agreementType);

    MetadataAgreementType updateAgreementType(UUID id, MetadataAgreementType agreementType);

    Optional<MetadataAgreementType> getAgreementType(UUID id);

    Optional<MetadataAgreementType> getAgreementTypeByCode(UUID tenantId, String typeCode, MarketContext marketContext);

    List<MetadataAgreementType> getAgreementTypesByTenant(UUID tenantId);

    List<MetadataAgreementType> getActiveAgreementTypesByTenantAndContext(UUID tenantId, MarketContext marketContext);

    SchemaValidator.ValidationResult validateAgainstSchema(UUID agreementTypeId, java.util.Map<String, Object> attributes);

    void deactivateAgreementType(UUID id);
}
