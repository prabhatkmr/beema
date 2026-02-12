package com.beema.kernel.domain.agreement;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "agreements")
public class Agreement extends BitemporalEntity {

    @Column(name = "agreement_number", nullable = false, length = 50)
    private String agreementNumber;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_context", nullable = false)
    private MarketContext marketContext;

    @Column(name = "agreement_type_id", nullable = false)
    private UUID agreementTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "agreement_status_type")
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(name = "data_residency_region", nullable = false, length = 10)
    private String dataResidencyRegion = "EU";

    @Column(name = "inception_date", nullable = false)
    private LocalDate inceptionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "GBP";

    @Column(name = "total_premium", precision = 18, scale = 4)
    private BigDecimal totalPremium;

    @Column(name = "total_sum_insured", precision = 18, scale = 4)
    private BigDecimal totalSumInsured;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "updated_by")
    private String updatedBy;

    // Note: Due to bitemporal composite keys, @OneToMany relationships are not supported.
    // Use repository queries to fetch parties and coverages:
    // - agreementPartyRepository.findByAgreementId(agreement.getTemporalKey().getId())
    // - agreementCoverageRepository.findByAgreementId(agreement.getTemporalKey().getId())

    // Transient fields for in-memory management (not persisted)
    @Transient
    private List<AgreementParty> parties = new ArrayList<>();

    @Transient
    private List<AgreementCoverage> coverages = new ArrayList<>();

    public Agreement() {
    }

    // Business methods

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    public boolean hasAttribute(String key) {
        return this.attributes.containsKey(key);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    // Getters and setters

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(MarketContext marketContext) {
        this.marketContext = marketContext;
    }

    public UUID getAgreementTypeId() {
        return agreementTypeId;
    }

    public void setAgreementTypeId(UUID agreementTypeId) {
        this.agreementTypeId = agreementTypeId;
    }

    public AgreementStatus getStatus() {
        return status;
    }

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public String getDataResidencyRegion() {
        return dataResidencyRegion;
    }

    public void setDataResidencyRegion(String dataResidencyRegion) {
        this.dataResidencyRegion = dataResidencyRegion;
    }

    public LocalDate getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(LocalDate inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public BigDecimal getTotalSumInsured() {
        return totalSumInsured;
    }

    public void setTotalSumInsured(BigDecimal totalSumInsured) {
        this.totalSumInsured = totalSumInsured;
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

    public List<AgreementParty> getParties() {
        return Collections.unmodifiableList(parties);
    }

    public void addParty(AgreementParty party) {
        parties.add(party);
    }

    public List<AgreementCoverage> getCoverages() {
        return Collections.unmodifiableList(coverages);
    }

    public void addCoverage(AgreementCoverage coverage) {
        coverages.add(coverage);
    }
}
