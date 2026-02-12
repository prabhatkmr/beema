package com.beema.kernel.domain.metadata;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Metadata defining agreement types and their JSON schemas.
 *
 * Example:
 * - type_code: "AUTO_POLICY"
 * - market_context: RETAIL
 * - attribute_schema: JSON Schema defining required/optional fields
 * - validation_rules: Business rules for underwriting/rating
 *
 * This enables flex-schema storage where different agreement types
 * can have different attributes without database migrations.
 */
@Entity
@Table(
    name = "metadata_agreement_types",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_agreement_type_context_version",
            columnNames = {"type_code", "market_context", "schema_version"}
        )
    },
    indexes = {
        @Index(name = "idx_metadata_agreement_types_code", columnList = "type_code"),
        @Index(name = "idx_metadata_agreement_types_context", columnList = "market_context"),
        @Index(name = "idx_metadata_agreement_types_active", columnList = "is_active")
    }
)
public class MetadataAgreementType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique code for the agreement type (e.g., "AUTO_POLICY", "HOMEOWNERS").
     */
    @Column(name = "type_code", nullable = false, length = 100)
    private String typeCode;

    /**
     * Market context this type applies to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "market_context", nullable = false, length = 50)
    private MarketContext marketContext;

    /**
     * Schema version (allows schema evolution over time).
     */
    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    /**
     * Human-readable display name.
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /**
     * Description of this agreement type.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * JSON Schema defining the structure of the attributes field.
     *
     * Example:
     * {
     *   "type": "object",
     *   "required": ["vehicle_vin", "vehicle_year"],
     *   "properties": {
     *     "vehicle_vin": {
     *       "type": "string",
     *       "pattern": "^[A-HJ-NPR-Z0-9]{17}$"
     *     },
     *     "vehicle_year": {
     *       "type": "integer",
     *       "minimum": 1900,
     *       "maximum": 2100
     *     }
     *   }
     * }
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "attribute_schema", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributeSchema = new HashMap<>();

    /**
     * Business validation rules (evaluated at runtime).
     *
     * Example:
     * {
     *   "underwriting_rules": {
     *     "max_vehicle_age": 30,
     *     "min_driver_age": 16
     *   },
     *   "rating_factors": {
     *     "base_rate": 500,
     *     "age_discount_threshold": 25
     *   }
     * }
     */
    @Convert(converter = JsonbConverter.class)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules = new HashMap<>();

    /**
     * Active flag - inactive types cannot be used for new agreements.
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

    public MetadataAgreementType() {
    }

    public MetadataAgreementType(UUID id, String typeCode, MarketContext marketContext, Integer schemaVersion,
                                String displayName, String description, Map<String, Object> attributeSchema,
                                Map<String, Object> validationRules, Boolean isActive, String createdBy,
                                String updatedBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                Long version) {
        this.id = id;
        this.typeCode = typeCode;
        this.marketContext = marketContext;
        this.schemaVersion = schemaVersion;
        this.displayName = displayName;
        this.description = description;
        this.attributeSchema = attributeSchema;
        this.validationRules = validationRules;
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

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public MarketContext getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(MarketContext marketContext) {
        this.marketContext = marketContext;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
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

    public Map<String, Object> getAttributeSchema() {
        return attributeSchema;
    }

    public void setAttributeSchema(Map<String, Object> attributeSchema) {
        this.attributeSchema = attributeSchema;
    }

    public Map<String, Object> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
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

    /**
     * Lifecycle callback: Set defaults before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (schemaVersion == null) {
            schemaVersion = 1;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (attributeSchema == null) {
            attributeSchema = new HashMap<>();
        }
        if (validationRules == null) {
            validationRules = new HashMap<>();
        }
    }

    /**
     * Get a composite key for caching/lookups.
     */
    public String getCompositeKey() {
        return String.format("%s:%s:v%d", typeCode, marketContext, schemaVersion);
    }
}
