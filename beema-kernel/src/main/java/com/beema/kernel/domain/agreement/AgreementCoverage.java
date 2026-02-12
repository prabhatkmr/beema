package com.beema.kernel.domain.agreement;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agreement coverage entity - Bitemporal coverage items.
 *
 * Represents individual coverage items with financial details.
 * Coverages can change over time (endorsements, adjustments).
 *
 * Example:
 * <pre>
 * AgreementCoverage coverage = new AgreementCoverage();
 * coverage.setAgreementId(agreementId);
 * coverage.setCoverageCode("COLLISION");
 * coverage.setCoverageName("Collision Coverage");
 * coverage.setCoverageLimit(new BigDecimal("50000.00"));
 * coverage.setDeductible(new BigDecimal("500.00"));
 * coverage.setPremium(new BigDecimal("850.00"));
 * coverage.setCurrency("USD");
 * </pre>
 */
@Entity
@Table(
    name = "agreement_coverages",
    indexes = {
        @Index(name = "idx_agreement_coverages_current", columnList = "id"),
        @Index(name = "idx_agreement_coverages_agreement", columnList = "agreement_id, coverage_code"),
        @Index(name = "idx_agreement_coverages_temporal_range", columnList = "id, valid_from, valid_to"),
        @Index(name = "idx_agreement_coverages_tenant", columnList = "tenant_id")
    }
)
public class AgreementCoverage extends BitemporalEntity {

    /**
     * Agreement business ID (not composite PK).
     */
    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    /**
     * Coverage code (e.g., "COLLISION", "COMPREHENSIVE", "LIABILITY").
     */
    @Column(name = "coverage_code", nullable = false, length = 100)
    private String coverageCode;

    /**
     * Human-readable coverage name.
     */
    @Column(name = "coverage_name", nullable = false, length = 200)
    private String coverageName;

    /**
     * Maximum payout amount.
     */
    @Column(name = "coverage_limit", precision = 19, scale = 4)
    private BigDecimal coverageLimit;

    /**
     * Amount insured pays before coverage applies.
     */
    @Column(name = "deductible", precision = 19, scale = 4)
    private BigDecimal deductible;

    /**
     * Cost of this coverage.
     */
    @Column(name = "premium", precision = 19, scale = 4)
    private BigDecimal premium;

    /**
     * Currency code (ISO 4217).
     */
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    /**
     * Flex-schema coverage attributes (JSONB).
     *
     * Example for AUTO_COLLISION:
     * {
     *   "coverage_applies_to": "OWNED_VEHICLE",
     *   "rental_reimbursement": true,
     *   "rental_daily_limit": 50.00,
     *   "towing_coverage": 100.00,
     *   "glass_deductible_waiver": true
     * }
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "coverage_attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> coverageAttributes = new HashMap<>();

    // Constructors

    public AgreementCoverage() {
        super();
    }

    // Business methods

    public Object getCoverageAttribute(String key) {
        return coverageAttributes.get(key);
    }

    public void setCoverageAttribute(String key, Object value) {
        this.coverageAttributes.put(key, value);
    }

    // Getters and setters

    public UUID getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(UUID agreementId) {
        this.agreementId = agreementId;
    }

    public String getCoverageCode() {
        return coverageCode;
    }

    public void setCoverageCode(String coverageCode) {
        this.coverageCode = coverageCode;
    }

    public String getCoverageName() {
        return coverageName;
    }

    public void setCoverageName(String coverageName) {
        this.coverageName = coverageName;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(BigDecimal coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getCoverageAttributes() {
        return coverageAttributes;
    }

    public void setCoverageAttributes(Map<String, Object> coverageAttributes) {
        this.coverageAttributes = coverageAttributes;
    }

    @Override
    public String toString() {
        return "AgreementCoverage{" +
               "id=" + getId() +
               ", agreementId=" + agreementId +
               ", code='" + coverageCode + '\'' +
               ", name='" + coverageName + '\'' +
               ", limit=" + coverageLimit +
               ", premium=" + premium +
               ", currency='" + currency + '\'' +
               '}';
    }
}
