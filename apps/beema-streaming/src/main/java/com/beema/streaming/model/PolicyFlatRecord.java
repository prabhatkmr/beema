package com.beema.streaming.model;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.Serializable;

/**
 * Avro-compatible flat POJO for writing policy data as Parquet.
 * Flattens the PolicyEvent into a columnar-friendly structure.
 * Remaining dynamic attributes are stored as a JSON string column.
 */
public class PolicyFlatRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final Schema AVRO_SCHEMA = SchemaBuilder.record("PolicyFlatRecord")
            .namespace("com.beema.streaming.model")
            .fields()
            // Event metadata
            .optionalString("event_id")
            .optionalString("event_type")
            .optionalString("event_timestamp")
            .optionalString("tenant_id")
            // Policy core
            .optionalString("policy_id")
            .optionalString("policy_number")
            .name("version").type().nullable().intType().noDefault()
            .optionalString("status")
            .optionalString("product_code")
            .optionalString("line_of_business")
            // Dates
            .optionalString("effective_date")
            .optionalString("expiry_date")
            .optionalString("inception_date")
            // Policyholder
            .optionalString("policyholder_name")
            .optionalString("policyholder_id")
            // Vehicle
            .optionalString("vehicle_make")
            .optionalString("vehicle_model")
            .name("vehicle_year").type().nullable().intType().noDefault()
            .optionalString("vehicle_registration")
            // Financials
            .name("gross_premium").type().nullable().doubleType().noDefault()
            .name("net_premium").type().nullable().doubleType().noDefault()
            .optionalString("currency")
            .name("sum_insured").type().nullable().doubleType().noDefault()
            // Distribution
            .optionalString("broker_code")
            .optionalString("agent_code")
            // Bitemporal
            .optionalString("valid_from")
            .optionalString("valid_to")
            // Provenance
            .optionalString("source_system")
            .optionalString("correlation_id")
            // Dynamic flex attributes (serialized JSON)
            .optionalString("attributes_json")
            // Partitioning columns
            .optionalString("partition_date")
            .optionalString("partition_hour")
            .endRecord();

    // Fields
    private String eventId;
    private String eventType;
    private String eventTimestamp;
    private String tenantId;
    private String policyId;
    private String policyNumber;
    private Integer version;
    private String status;
    private String productCode;
    private String lineOfBusiness;
    private String effectiveDate;
    private String expiryDate;
    private String inceptionDate;
    private String policyholderName;
    private String policyholderId;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleRegistration;
    private Double grossPremium;
    private Double netPremium;
    private String currency;
    private Double sumInsured;
    private String brokerCode;
    private String agentCode;
    private String validFrom;
    private String validTo;
    private String sourceSystem;
    private String correlationId;
    private String attributesJson;
    private String partitionDate;
    private String partitionHour;

    public PolicyFlatRecord() {}

    /**
     * Convert this POJO to an Avro GenericRecord for Parquet serialization.
     */
    public GenericRecord toGenericRecord() {
        GenericRecord record = new GenericData.Record(AVRO_SCHEMA);
        record.put("event_id", eventId);
        record.put("event_type", eventType);
        record.put("event_timestamp", eventTimestamp);
        record.put("tenant_id", tenantId);
        record.put("policy_id", policyId);
        record.put("policy_number", policyNumber);
        record.put("version", version);
        record.put("status", status);
        record.put("product_code", productCode);
        record.put("line_of_business", lineOfBusiness);
        record.put("effective_date", effectiveDate);
        record.put("expiry_date", expiryDate);
        record.put("inception_date", inceptionDate);
        record.put("policyholder_name", policyholderName);
        record.put("policyholder_id", policyholderId);
        record.put("vehicle_make", vehicleMake);
        record.put("vehicle_model", vehicleModel);
        record.put("vehicle_year", vehicleYear);
        record.put("vehicle_registration", vehicleRegistration);
        record.put("gross_premium", grossPremium);
        record.put("net_premium", netPremium);
        record.put("currency", currency);
        record.put("sum_insured", sumInsured);
        record.put("broker_code", brokerCode);
        record.put("agent_code", agentCode);
        record.put("valid_from", validFrom);
        record.put("valid_to", validTo);
        record.put("source_system", sourceSystem);
        record.put("correlation_id", correlationId);
        record.put("attributes_json", attributesJson);
        record.put("partition_date", partitionDate);
        record.put("partition_hour", partitionHour);
        return record;
    }

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

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }

    public String getValidTo() { return validTo; }
    public void setValidTo(String validTo) { this.validTo = validTo; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }

    public String getPartitionDate() { return partitionDate; }
    public void setPartitionDate(String partitionDate) { this.partitionDate = partitionDate; }

    public String getPartitionHour() { return partitionHour; }
    public void setPartitionHour(String partitionHour) { this.partitionHour = partitionHour; }
}
