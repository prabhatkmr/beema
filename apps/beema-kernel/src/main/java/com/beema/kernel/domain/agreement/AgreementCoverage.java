package com.beema.kernel.domain.agreement;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "agreement_coverages")
public class AgreementCoverage extends BitemporalEntity {

    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    @Column(name = "coverage_code", nullable = false, length = 100)
    private String coverageCode;

    @Column(name = "coverage_name", nullable = false, length = 255)
    private String coverageName;

    @Column(name = "coverage_type", length = 50)
    private String coverageType;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "GBP";

    @Column(name = "premium", precision = 18, scale = 4)
    private BigDecimal premium;

    @Column(name = "sum_insured", precision = 18, scale = 4)
    private BigDecimal sumInsured;

    @Column(name = "deductible", precision = 18, scale = 4)
    private BigDecimal deductible;

    @Column(name = "deductible_type", length = 20)
    private String deductibleType;

    @Column(name = "limit_amount", precision = 18, scale = 4)
    private BigDecimal limitAmount;

    @Column(name = "rate", precision = 12, scale = 8)
    private BigDecimal rate;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "updated_by")
    private String updatedBy;

    protected AgreementCoverage() {
    }

    // Business methods

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
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

    public String getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(String coverageType) {
        this.coverageType = coverageType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public void setSumInsured(BigDecimal sumInsured) {
        this.sumInsured = sumInsured;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public String getDeductibleType() {
        return deductibleType;
    }

    public void setDeductibleType(String deductibleType) {
        this.deductibleType = deductibleType;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
