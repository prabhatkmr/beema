package com.beema.streaming.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * Kafka event model representing a policy change event from beema.events.policy_change topic.
 * Captures the full event envelope including metadata and nested policy data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_timestamp")
    private String eventTimestamp;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("policy_id")
    private String policyId;

    @JsonProperty("policy_number")
    private String policyNumber;

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("status")
    private String status;

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("line_of_business")
    private String lineOfBusiness;

    @JsonProperty("effective_date")
    private String effectiveDate;

    @JsonProperty("expiry_date")
    private String expiryDate;

    @JsonProperty("inception_date")
    private String inceptionDate;

    // Policyholder
    @JsonProperty("policyholder_name")
    private String policyholderName;

    @JsonProperty("policyholder_id")
    private String policyholderId;

    // Vehicle info (for motor policies)
    @JsonProperty("vehicle_make")
    private String vehicleMake;

    @JsonProperty("vehicle_model")
    private String vehicleModel;

    @JsonProperty("vehicle_year")
    private Integer vehicleYear;

    @JsonProperty("vehicle_registration")
    private String vehicleRegistration;

    // Premium
    @JsonProperty("gross_premium")
    private Double grossPremium;

    @JsonProperty("net_premium")
    private Double netPremium;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("sum_insured")
    private Double sumInsured;

    // Broker / Agent
    @JsonProperty("broker_code")
    private String brokerCode;

    @JsonProperty("agent_code")
    private String agentCode;

    // Flex attributes (metadata-driven JSONB fields)
    @JsonProperty("attributes")
    private Map<String, Object> attributes;

    // Valid time (bitemporal)
    @JsonProperty("valid_from")
    private String validFrom;

    @JsonProperty("valid_to")
    private String validTo;

    // Source system
    @JsonProperty("source_system")
    private String sourceSystem;

    @JsonProperty("correlation_id")
    private String correlationId;

    public PolicyEvent() {}

    // --- Getters and Setters ---

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(String eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getLineOfBusiness() { return lineOfBusiness; }
    public void setLineOfBusiness(String lineOfBusiness) { this.lineOfBusiness = lineOfBusiness; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getInceptionDate() { return inceptionDate; }
    public void setInceptionDate(String inceptionDate) { this.inceptionDate = inceptionDate; }

    public String getPolicyholderName() { return policyholderName; }
    public void setPolicyholderName(String policyholderName) { this.policyholderName = policyholderName; }

    public String getPolicyholderId() { return policyholderId; }
    public void setPolicyholderId(String policyholderId) { this.policyholderId = policyholderId; }

    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }

    public String getVehicleRegistration() { return vehicleRegistration; }
    public void setVehicleRegistration(String vehicleRegistration) { this.vehicleRegistration = vehicleRegistration; }

    public Double getGrossPremium() { return grossPremium; }
    public void setGrossPremium(Double grossPremium) { this.grossPremium = grossPremium; }

    public Double getNetPremium() { return netPremium; }
    public void setNetPremium(Double netPremium) { this.netPremium = netPremium; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getSumInsured() { return sumInsured; }
    public void setSumInsured(Double sumInsured) { this.sumInsured = sumInsured; }

    public String getBrokerCode() { return brokerCode; }
    public void setBrokerCode(String brokerCode) { this.brokerCode = brokerCode; }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }

    public String getValidTo() { return validTo; }
    public void setValidTo(String validTo) { this.validTo = validTo; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
