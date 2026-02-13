package com.beema.streaming.mapper;

import com.beema.streaming.model.PolicyEvent;
import com.beema.streaming.model.PolicyFlatRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.functions.MapFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Maps a PolicyEvent from Kafka to a flat PolicyFlatRecord for Parquet writing.
 * Extracts common policy attributes into typed columns and serializes remaining
 * dynamic attributes as a JSON string.
 * Adds partition_date (yyyy-MM-dd) and partition_hour (HH) based on event timestamp.
 */
public class PolicyEventMapper implements MapFunction<PolicyEvent, PolicyFlatRecord> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PolicyEventMapper.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH");

    private transient ObjectMapper objectMapper;

    @Override
    public PolicyFlatRecord map(PolicyEvent event) throws Exception {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        }

        PolicyFlatRecord record = new PolicyFlatRecord();

        // Event metadata
        record.setEventId(event.getEventId());
        record.setEventType(event.getEventType());
        record.setEventTimestamp(event.getEventTimestamp());
        record.setTenantId(event.getTenantId());

        // Policy core
        record.setPolicyId(event.getPolicyId());
        record.setPolicyNumber(event.getPolicyNumber());
        record.setVersion(event.getVersion());
        record.setStatus(event.getStatus());
        record.setProductCode(event.getProductCode());
        record.setLineOfBusiness(event.getLineOfBusiness());

        // Dates
        record.setEffectiveDate(event.getEffectiveDate());
        record.setExpiryDate(event.getExpiryDate());
        record.setInceptionDate(event.getInceptionDate());

        // Policyholder
        record.setPolicyholderName(event.getPolicyholderName());
        record.setPolicyholderId(event.getPolicyholderId());

        // Vehicle
        record.setVehicleMake(event.getVehicleMake());
        record.setVehicleModel(event.getVehicleModel());
        record.setVehicleYear(event.getVehicleYear());
        record.setVehicleRegistration(event.getVehicleRegistration());

        // Financials
        record.setGrossPremium(event.getGrossPremium());
        record.setNetPremium(event.getNetPremium());
        record.setCurrency(event.getCurrency());
        record.setSumInsured(event.getSumInsured());

        // Distribution
        record.setBrokerCode(event.getBrokerCode());
        record.setAgentCode(event.getAgentCode());

        // Bitemporal
        record.setValidFrom(event.getValidFrom());
        record.setValidTo(event.getValidTo());

        // Provenance
        record.setSourceSystem(event.getSourceSystem());
        record.setCorrelationId(event.getCorrelationId());

        // Serialize remaining attributes as JSON string
        if (event.getAttributes() != null && !event.getAttributes().isEmpty()) {
            try {
                record.setAttributesJson(objectMapper.writeValueAsString(event.getAttributes()));
            } catch (Exception e) {
                log.warn("Failed to serialize attributes for event {}: {}", event.getEventId(), e.getMessage());
                record.setAttributesJson("{}");
            }
        }

        // Compute partitioning columns from event timestamp
        ZonedDateTime eventTime = parseTimestamp(event.getEventTimestamp());
        record.setPartitionDate(eventTime.format(DATE_FMT));
        record.setPartitionHour(eventTime.format(HOUR_FMT));

        return record;
    }

    /**
     * Parse event timestamp. Supports ISO-8601 and epoch millis.
     * Falls back to current time if parsing fails.
     */
    private ZonedDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return ZonedDateTime.now(ZoneOffset.UTC);
        }
        // Try ISO-8601 first
        try {
            return ZonedDateTime.parse(timestamp).withZoneSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {}

        // Try Instant (ISO without zone)
        try {
            return Instant.parse(timestamp).atZone(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {}

        // Try epoch millis
        try {
            long epochMillis = Long.parseLong(timestamp);
            return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC);
        } catch (NumberFormatException ignored) {}

        log.warn("Unable to parse timestamp '{}', using current time", timestamp);
        return ZonedDateTime.now(ZoneOffset.UTC);
    }
}
