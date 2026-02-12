package com.beema.kernel.domain.agreement;

import com.beema.kernel.domain.base.BitemporalEntity;
import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agreement party entity - Bitemporal party relationships.
 *
 * Tracks parties involved in agreements with their own temporal history.
 * Party details can change over time (address, contact info) without
 * losing historical accuracy.
 *
 * Example:
 * <pre>
 * AgreementParty party = new AgreementParty();
 * party.setAgreementId(agreementId);
 * party.setPartyRole(PartyRole.INSURED);
 * party.setPartyType(PartyType.INDIVIDUAL);
 * party.setPartyData(Map.of(
 *     "first_name", "John",
 *     "last_name", "Doe",
 *     "date_of_birth", "1988-05-15",
 *     "email", "john.doe@example.com",
 *     "phone", "555-0123"
 * ));
 * </pre>
 */
@Entity
@Table(
    name = "agreement_parties",
    indexes = {
        @Index(name = "idx_agreement_parties_current", columnList = "id"),
        @Index(name = "idx_agreement_parties_agreement", columnList = "agreement_id, party_role"),
        @Index(name = "idx_agreement_parties_temporal_range", columnList = "id, valid_from, valid_to"),
        @Index(name = "idx_agreement_parties_tenant", columnList = "tenant_id")
    }
)
public class AgreementParty extends BitemporalEntity {

    /**
     * Agreement business ID (not composite PK).
     */
    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    /**
     * Role of party in the agreement.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "party_role", nullable = false, length = 50)
    private PartyRole partyRole;

    /**
     * Type of party (individual or organization).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 50)
    private PartyType partyType;

    /**
     * Flex-schema party data (JSONB).
     *
     * Example for INDIVIDUAL:
     * {
     *   "first_name": "John",
     *   "last_name": "Doe",
     *   "date_of_birth": "1988-05-15",
     *   "email": "john.doe@example.com",
     *   "phone": "555-0123",
     *   "address": {
     *     "street": "123 Main St",
     *     "city": "San Francisco",
     *     "state": "CA",
     *     "zip": "94102"
     *   }
     * }
     *
     * Example for ORGANIZATION:
     * {
     *   "business_name": "Acme Corp",
     *   "tax_id": "12-3456789",
     *   "email": "contact@acme.com",
     *   "phone": "555-0199",
     *   "address": { ... }
     * }
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "party_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> partyData = new HashMap<>();

    // Constructors

    public AgreementParty() {
        super();
    }

    // Business methods

    public Object getPartyDataField(String key) {
        return partyData.get(key);
    }

    public void setPartyDataField(String key, Object value) {
        this.partyData.put(key, value);
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

    public PartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(PartyType partyType) {
        this.partyType = partyType;
    }

    public Map<String, Object> getPartyData() {
        return partyData;
    }

    public void setPartyData(Map<String, Object> partyData) {
        this.partyData = partyData;
    }

    @Override
    public String toString() {
        return "AgreementParty{" +
               "id=" + getId() +
               ", agreementId=" + agreementId +
               ", role=" + partyRole +
               ", type=" + partyType +
               ", validFrom=" + getValidFrom() +
               ", validTo=" + getValidTo() +
               '}';
    }
}
