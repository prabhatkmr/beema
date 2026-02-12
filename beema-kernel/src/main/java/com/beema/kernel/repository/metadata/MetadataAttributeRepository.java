package com.beema.kernel.repository.metadata;

import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for metadata attributes catalog.
 */
@Repository
public interface MetadataAttributeRepository extends JpaRepository<MetadataAttribute, UUID> {

    /**
     * Find attribute by key and market context.
     *
     * @param attributeKey Attribute key
     * @param marketContext Market context
     * @return Attribute definition if found
     */
    Optional<MetadataAttribute> findByAttributeKeyAndMarketContext(
        String attributeKey,
        MarketContext marketContext
    );

    /**
     * Find all active attributes for a market context.
     *
     * @param marketContext Market context
     * @return List of active attributes
     */
    @Query("""
        SELECT ma FROM MetadataAttribute ma
        WHERE ma.marketContext = :marketContext
          AND ma.isActive = true
        ORDER BY ma.displayName
        """)
    List<MetadataAttribute> findAllActiveByMarketContext(
        @Param("marketContext") MarketContext marketContext
    );

    /**
     * Find all active attributes across all markets.
     *
     * @return List of all active attributes
     */
    @Query("""
        SELECT ma FROM MetadataAttribute ma
        WHERE ma.isActive = true
        ORDER BY ma.marketContext, ma.displayName
        """)
    List<MetadataAttribute> findAllActive();

    /**
     * Find required attributes for a market context.
     *
     * @param marketContext Market context
     * @return List of required attributes
     */
    @Query("""
        SELECT ma FROM MetadataAttribute ma
        WHERE ma.marketContext = :marketContext
          AND ma.isRequired = true
          AND ma.isActive = true
        ORDER BY ma.displayName
        """)
    List<MetadataAttribute> findRequiredByMarketContext(
        @Param("marketContext") MarketContext marketContext
    );
}
