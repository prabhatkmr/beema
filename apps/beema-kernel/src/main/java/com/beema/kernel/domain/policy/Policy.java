package com.beema.kernel.domain.policy;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "policy_number", nullable = false)
    private String policyNumber;

    @Column(name = "version", nullable = false)
    private Integer version;

    // Bitemporal fields
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    // Policy details
    @Column(name = "status")
    private String status;

    @Column(name = "premium")
    private Double premium;

    @Column(name = "inception_date")
    private LocalDateTime inceptionDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "coverage_details", columnDefinition = "jsonb")
    private Map<String, Object> coverageDetails = new HashMap<>();

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (transactionTime == null) {
            transactionTime = LocalDateTime.now();
        }
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getPremium() {
        return premium;
    }

    public void setPremium(Double premium) {
        this.premium = premium;
    }

    public LocalDateTime getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(LocalDateTime inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Map<String, Object> getCoverageDetails() {
        return coverageDetails;
    }

    public void setCoverageDetails(Map<String, Object> coverageDetails) {
        this.coverageDetails = coverageDetails != null ? new HashMap<>(coverageDetails) : new HashMap<>();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
}
