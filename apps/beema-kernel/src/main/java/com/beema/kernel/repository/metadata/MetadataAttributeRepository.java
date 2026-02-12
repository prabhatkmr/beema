package com.beema.kernel.repository.metadata;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAttribute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetadataAttributeRepository extends JpaRepository<MetadataAttribute, UUID> {

    Optional<MetadataAttribute> findByTenantIdAndAttributeNameAndMarketContext(
            UUID tenantId, String attributeName, MarketContext marketContext);

    List<MetadataAttribute> findByTenantIdAndMarketContext(UUID tenantId, MarketContext marketContext);

    List<MetadataAttribute> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<MetadataAttribute> findByTenantIdAndMarketContextAndIsActiveTrue(
            UUID tenantId, MarketContext marketContext);

    List<MetadataAttribute> findByTenantIdAndIsSearchableTrue(UUID tenantId);

    List<MetadataAttribute> findByTenantIdAndCategory(UUID tenantId, String category);

    boolean existsByTenantIdAndAttributeNameAndMarketContext(
            UUID tenantId, String attributeName, MarketContext marketContext);
}
