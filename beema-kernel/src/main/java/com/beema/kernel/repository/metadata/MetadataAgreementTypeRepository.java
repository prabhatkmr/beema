package com.beema.kernel.repository.metadata;

import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for metadata agreement types.
 *
 * Provides queries for schema lookups and validation.
 */
@Repository
public interface MetadataAgreementTypeRepository extends JpaRepository<MetadataAgreementType, UUID> {

    /**
     * Find agreement type by code and market context (latest version).
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @return Latest active version of the type
     */
    @Query("""
        SELECT mat FROM MetadataAgreementType mat
        WHERE mat.typeCode = :typeCode
          AND mat.marketContext = :marketContext
          AND mat.isActive = true
        ORDER BY mat.schemaVersion DESC
        LIMIT 1
        """)
    Optional<MetadataAgreementType> findLatestByTypeCodeAndMarketContext(
        @Param("typeCode") String typeCode,
        @Param("marketContext") MarketContext marketContext
    );

    /**
     * Find agreement type by code, market context, and specific version.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @param schemaVersion Schema version
     * @return Specific version of the type
     */
    Optional<MetadataAgreementType> findByTypeCodeAndMarketContextAndSchemaVersion(
        String typeCode,
        MarketContext marketContext,
        Integer schemaVersion
    );

    /**
     * Find all active agreement types for a market context.
     *
     * @param marketContext Market context
     * @return List of active types
     */
    @Query("""
        SELECT mat FROM MetadataAgreementType mat
        WHERE mat.marketContext = :marketContext
          AND mat.isActive = true
        ORDER BY mat.displayName
        """)
    List<MetadataAgreementType> findAllActiveByMarketContext(
        @Param("marketContext") MarketContext marketContext
    );

    /**
     * Find all versions of a type.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @return All versions ordered by schema version descending
     */
    @Query("""
        SELECT mat FROM MetadataAgreementType mat
        WHERE mat.typeCode = :typeCode
          AND mat.marketContext = :marketContext
        ORDER BY mat.schemaVersion DESC
        """)
    List<MetadataAgreementType> findAllVersionsByTypeCodeAndMarketContext(
        @Param("typeCode") String typeCode,
        @Param("marketContext") MarketContext marketContext
    );

    /**
     * Check if a type exists (any version).
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @return True if exists
     */
    boolean existsByTypeCodeAndMarketContext(String typeCode, MarketContext marketContext);

    /**
     * Find all active types across all markets.
     *
     * @return List of all active types
     */
    @Query("""
        SELECT mat FROM MetadataAgreementType mat
        WHERE mat.isActive = true
        ORDER BY mat.marketContext, mat.displayName
        """)
    List<MetadataAgreementType> findAllActive();
}
