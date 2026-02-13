package com.beema.kernel.domain.agreement;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Agreement entity - Master agreement table.
 *
 * Represents insurance agreements across all market contexts:
 * - RETAIL: Personal lines (auto, home, etc.)
 * - COMMERCIAL: Business lines (liability, property, etc.)
 * - LONDON_MARKET: Specialty/reinsurance (marine, aviation, etc.)
 *
 * Bitemporal tracking enables:
 * - Time travel: "What did we know on 2024-01-15?"
 * - Point-in-time: "What was valid on 2024-01-15?"
 * - Full audit trail: "Who changed what, when?"
 * - Corrections: Create new transaction version without data loss
 *
 * Example:
 * <pre>
 * Agreement agreement = new Agreement();
 * agreement.setTemporalKey(TemporalKey.now(UUID.randomUUID(), OffsetDateTime.now()));
 * agreement.setAgreementNumber("POL-2024-001234");
 * agreement.setAgreementTypeCode("AUTO_POLICY");
 * agreement.setMarketContext(MarketContext.RETAIL);
 * agreement.setStatus(AgreementStatus.DRAFT);
 * agreement.setTenantId("tenant-123");
 * agreement.setAttributes(Map.of(
 *     "vehicle_vin", "1HGCM82633A123456",
 *     "vehicle_year", 2024,
 *     "primary_driver_age", 35
 * ));
 * </pre>
 */
@Entity
@Table(
    name = "agreements",
    indexes = {
        @Index(name = "idx_agreements_current", columnList = "id"),
        @Index(name = "idx_agreements_number_current", columnList = "agreement_number, tenant_id"),
        @Index(name = "idx_agreements_temporal_range", columnList = "id, valid_from, valid_to"),
        @Index(name = "idx_agreements_tenant_market", columnList = "tenant_id, market_context"),
        @Index(name = "idx_agreements_type_context", columnList = "agreement_type_code, market_context"),
        @Index(name = "idx_agreements_status", columnList = "status, tenant_id")
    }
)
public class Agreement extends BitemporalEntity {

    /**
     * User-visible agreement number (e.g., POL-2024-001234).
     */
    @Column(name = "agreement_number", nullable = false, length = 100)
    private String agreementNumber;

    /**
     * Agreement type code (references metadata_agreement_types.type_code).
     */
    @Column(name = "agreement_type_code", nullable = false, length = 100)
    private String agreementTypeCode;

    /**
     * Market context.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "market_context", nullable = false, length = 50)
    private MarketContext marketContext;

    /**
     * Current status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AgreementStatus status = AgreementStatus.DRAFT;

    /**
     * Flex-schema attributes (JSONB).
     *
     * Structure defined by metadata_agreement_types.attribute_schema.
     *
     * Example for AUTO_POLICY:
     * {
     *   "vehicle_vin": "1HGCM82633A123456",
     *   "vehicle_year": 2024,
     *   "vehicle_make": "Honda",
     *   "vehicle_model": "Accord",
     *   "primary_driver_age": 35,
     *   "annual_mileage": 12000,
     *   "garage_location": {
     *     "city": "San Francisco",
     *     "state": "CA",
     *     "zip": "94102"
     *   }
     * }
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Data residency region (for compliance).
     */
    @Column(name = "data_residency_region", length = 50)
    private String dataResidencyRegion;

    // Constructors

    public Agreement() {
        super();
    }

    // Business methods

    /**
     * Get attribute value by key.
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Set attribute value.
     */
    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * Check if attribute exists.
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Remove attribute.
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * Get all attribute keys.
     */
    public java.util.Set<String> getAttributeKeys() {
        return attributes.keySet();
    }

    // Getters and setters

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public String getAgreementTypeCode() {
        return agreementTypeCode;
    }

    public void setAgreementTypeCode(String agreementTypeCode) {
        this.agreementTypeCode = agreementTypeCode;
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(MarketContext marketContext) {
        this.marketContext = marketContext;
    }

    public AgreementStatus getStatus() {
        return status;
    }

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getDataResidencyRegion() {
        return dataResidencyRegion;
    }

    public void setDataResidencyRegion(String dataResidencyRegion) {
        this.dataResidencyRegion = dataResidencyRegion;
    }

    @Override
    public String toString() {
        return "Agreement{" +
               "id=" + getId() +
               ", agreementNumber='" + agreementNumber + '\'' +
               ", typeCode='" + agreementTypeCode + '\'' +
               ", marketContext=" + marketContext +
               ", status=" + status +
               ", validFrom=" + getValidFrom() +
               ", validTo=" + getValidTo() +
               ", isCurrent=" + getIsCurrent() +
               '}';
    }
}
