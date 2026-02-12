package com.beema.kernel.domain.metadata;

import com.beema.kernel.util.JsonbConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "metadata_agreement_type_attributes")
public class MetadataTypeAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agreement_type_id", nullable = false)
    private UUID agreementTypeId;

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(name = "is_required_override")
    private Boolean isRequiredOverride;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "default_value_override", columnDefinition = "JSONB")
    private Map<String, Object> defaultValueOverride;

    @Column(name = "ui_order_override")
    private Integer uiOrderOverride;

    @Column(name = "section_name", length = 100)
    private String sectionName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MetadataTypeAttribute() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgreementTypeId() {
        return agreementTypeId;
    }

    public void setAgreementTypeId(UUID agreementTypeId) {
        this.agreementTypeId = agreementTypeId;
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(UUID attributeId) {
        this.attributeId = attributeId;
    }

    public Boolean getIsRequiredOverride() {
        return isRequiredOverride;
    }

    public void setIsRequiredOverride(Boolean isRequiredOverride) {
        this.isRequiredOverride = isRequiredOverride;
    }

    public Map<String, Object> getDefaultValueOverride() {
        return defaultValueOverride;
    }

    public void setDefaultValueOverride(Map<String, Object> defaultValueOverride) {
        this.defaultValueOverride = defaultValueOverride;
    }

    public Integer getUiOrderOverride() {
        return uiOrderOverride;
    }

    public void setUiOrderOverride(Integer uiOrderOverride) {
        this.uiOrderOverride = uiOrderOverride;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
