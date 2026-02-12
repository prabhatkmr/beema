package com.beema.kernel.repository.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetadataAgreementTypeRepository extends JpaRepository<MetadataAgreementType, UUID> {

    Optional<MetadataAgreementType> findByTenantIdAndTypeCodeAndMarketContext(
            UUID tenantId, String typeCode, MarketContext marketContext);

    List<MetadataAgreementType> findByTenantIdAndMarketContext(UUID tenantId, MarketContext marketContext);

    List<MetadataAgreementType> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<MetadataAgreementType> findByTenantIdAndMarketContextAndIsActiveTrue(
            UUID tenantId, MarketContext marketContext);

    Optional<MetadataAgreementType> findByTenantIdAndTypeCodeAndMarketContextAndSchemaVersion(
            UUID tenantId, String typeCode, MarketContext marketContext, Integer schemaVersion);

    boolean existsByTenantIdAndTypeCodeAndMarketContext(
            UUID tenantId, String typeCode, MarketContext marketContext);
}
