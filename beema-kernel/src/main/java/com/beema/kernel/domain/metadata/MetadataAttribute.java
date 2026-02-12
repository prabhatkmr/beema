package com.beema.kernel.domain.metadata;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catalog of all possible attributes across all market contexts.
 *
 * Defines:
 * - Data type (STRING, INTEGER, DECIMAL, etc.)
 * - Validation patterns (regex, ranges)
 * - UI component hints (TEXT_INPUT, DROPDOWN, etc.)
 * - Default values and dropdown options
 */
@Entity
@Table(
    name = "metadata_attributes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_attribute_key_context",
            columnNames = {"attribute_key", "market_context"}
        )
    },
    indexes = {
        @Index(name = "idx_metadata_attributes_key", columnList = "attribute_key"),
        @Index(name = "idx_metadata_attributes_context", columnList = "market_context"),
        @Index(name = "idx_metadata_attributes_active", columnList = "is_active")
    }
)
public class MetadataAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique key for this attribute (e.g., "vehicle_vin", "driver_age").
     */
    @Column(name = "attribute_key", nullable = false, length = 100)
    private String attributeKey;

    /**
     * Market context (attribute may have different meaning per context).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "market_context", nullable = false, length = 50)
    private MarketContext marketContext;

    /**
     * Display name for UI.
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /**
     * Description of this attribute.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Data type for validation and UI rendering.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private AttributeDataType dataType;

    /**
     * Validation pattern (regex for strings, range for numbers).
     */
    @Column(name = "validation_pattern", length = 500)
    private String validationPattern;

    /**
     * UI component hint.
     */
    @Column(name = "ui_component", length = 50)
    private String uiComponent;

    /**
     * Dropdown options (if applicable).
     * Example: ["SEDAN", "SUV", "TRUCK"]
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<String> options;

    /**
     * Default value (if applicable).
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private Map<String, Object> defaultValue;

    /**
     * Whether this attribute is required.
     */
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    /**
     * Active flag.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Audit fields

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    // Constructors

    public MetadataAttribute() {
    }

    public MetadataAttribute(UUID id, String attributeKey, MarketContext marketContext, String displayName,
                             String description, AttributeDataType dataType, String validationPattern,
                             String uiComponent, List<String> options, Map<String, Object> defaultValue,
                             Boolean isRequired, Boolean isActive, String createdBy, String updatedBy,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt, Long version) {
        this.id = id;
        this.attributeKey = attributeKey;
        this.marketContext = marketContext;
        this.displayName = displayName;
        this.description = description;
        this.dataType = dataType;
        this.validationPattern = validationPattern;
        this.uiComponent = uiComponent;
        this.options = options;
        this.defaultValue = defaultValue;
        this.isRequired = isRequired;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(MarketContext marketContext) {
        this.marketContext = marketContext;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AttributeDataType getDataType() {
        return dataType;
    }

    public void setDataType(AttributeDataType dataType) {
        this.dataType = dataType;
    }

    public String getValidationPattern() {
        return validationPattern;
    }

    public void setValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
    }

    public String getUiComponent() {
        return uiComponent;
    }

    public void setUiComponent(String uiComponent) {
        this.uiComponent = uiComponent;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Map<String, Object> getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Map<String, Object> defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Lifecycle methods

    @PrePersist
    protected void onCreate() {
        if (isRequired == null) {
            isRequired = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
