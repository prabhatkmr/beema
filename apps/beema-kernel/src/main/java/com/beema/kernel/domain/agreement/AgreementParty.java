package com.beema.kernel.domain.agreement;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "agreement_parties")
public class AgreementParty extends BitemporalEntity {

    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_role", nullable = false, columnDefinition = "party_role_type")
    private PartyRole partyRole;

    @Column(name = "party_reference", nullable = false, length = 100)
    private String partyReference;

    @Column(name = "party_name", length = 255)
    private String partyName;

    @Column(name = "share_percentage", precision = 7, scale = 4)
    private BigDecimal sharePercentage;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "updated_by")
    private String updatedBy;

    protected AgreementParty() {
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

    public PartyRole getPartyRole() {
        return partyRole;
    }

    public void setPartyRole(PartyRole partyRole) {
        this.partyRole = partyRole;
    }

    public String getPartyReference() {
        return partyReference;
    }

    public void setPartyReference(String partyReference) {
        this.partyReference = partyReference;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public BigDecimal getSharePercentage() {
        return sharePercentage;
    }

    public void setSharePercentage(BigDecimal sharePercentage) {
        this.sharePercentage = sharePercentage;
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
