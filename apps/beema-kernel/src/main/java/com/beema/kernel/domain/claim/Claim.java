package com.beema.kernel.domain.claim;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Claim domain model for AI-powered claim analysis
 */
public class Claim {
    private String claimId;
    private String claimNumber;
    private String policyNumber;
    private String claimType;
    private ClaimStatus status;
    private Double claimAmount;
    private String description;
    private OffsetDateTime incidentDate;
    private OffsetDateTime reportedDate;
    private String marketContext;
    private Map<String, Object> claimData;

    // Getters and Setters
    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public Double getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(Double claimAmount) {
        this.claimAmount = claimAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(OffsetDateTime incidentDate) {
        this.incidentDate = incidentDate;
    }

    public OffsetDateTime getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(OffsetDateTime reportedDate) {
        this.reportedDate = reportedDate;
    }

    public String getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(String marketContext) {
        this.marketContext = marketContext;
    }

    public Map<String, Object> getClaimData() {
        return claimData;
    }

    public void setClaimData(Map<String, Object> claimData) {
        this.claimData = claimData;
    }

    public enum ClaimStatus {
        REPORTED,
        INVESTIGATING,
        APPROVED,
        REJECTED,
        SETTLED
    }
}
