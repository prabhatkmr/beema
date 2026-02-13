package com.beema.streaming.mapper;

import com.beema.streaming.model.PolicyEvent;
import com.beema.streaming.model.PolicyFlatRecord;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEventMapperTest {

    private PolicyEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PolicyEventMapper();
    }

    @Test
    void shouldMapAllCoreFields() throws Exception {
        PolicyEvent event = createSampleEvent();
        PolicyFlatRecord record = mapper.map(event);

        assertThat(record.getEventId()).isEqualTo("evt-001");
        assertThat(record.getEventType()).isEqualTo("POLICY_CREATED");
        assertThat(record.getTenantId()).isEqualTo("tenant-1");
        assertThat(record.getPolicyId()).isEqualTo("pol-001");
        assertThat(record.getPolicyNumber()).isEqualTo("POL-2024-001");
        assertThat(record.getVersion()).isEqualTo(1);
        assertThat(record.getStatus()).isEqualTo("ACTIVE");
        assertThat(record.getProductCode()).isEqualTo("MOTOR");
        assertThat(record.getLineOfBusiness()).isEqualTo("RETAIL");
        assertThat(record.getGrossPremium()).isEqualTo(1500.00);
        assertThat(record.getNetPremium()).isEqualTo(1350.00);
        assertThat(record.getCurrency()).isEqualTo("GBP");
        assertThat(record.getVehicleMake()).isEqualTo("Toyota");
        assertThat(record.getVehicleModel()).isEqualTo("Corolla");
        assertThat(record.getVehicleYear()).isEqualTo(2023);
    }

    @Test
    void shouldSerializeAttributesAsJson() throws Exception {
        PolicyEvent event = createSampleEvent();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("excess", 500);
        attrs.put("ncd_years", 5);
        event.setAttributes(attrs);

        PolicyFlatRecord record = mapper.map(event);

        assertThat(record.getAttributesJson()).isNotNull();
        assertThat(record.getAttributesJson()).contains("excess");
        assertThat(record.getAttributesJson()).contains("ncd_years");
    }

    @Test
    void shouldExtractPartitionFromIsoTimestamp() throws Exception {
        PolicyEvent event = createSampleEvent();
        event.setEventTimestamp("2024-06-15T14:30:00Z");

        PolicyFlatRecord record = mapper.map(event);

        assertThat(record.getPartitionDate()).isEqualTo("2024-06-15");
        assertThat(record.getPartitionHour()).isEqualTo("14");
    }

    @Test
    void shouldExtractPartitionFromEpochMillis() throws Exception {
        PolicyEvent event = createSampleEvent();
        // 2024-06-15T14:30:00Z in epoch millis
        event.setEventTimestamp("1718461800000");

        PolicyFlatRecord record = mapper.map(event);

        assertThat(record.getPartitionDate()).isEqualTo("2024-06-15");
        assertThat(record.getPartitionHour()).isEqualTo("14");
    }

    @Test
    void shouldDefaultToCurrentTimeForNullTimestamp() throws Exception {
        PolicyEvent event = createSampleEvent();
        event.setEventTimestamp(null);

        PolicyFlatRecord record = mapper.map(event);

        assertThat(record.getPartitionDate()).isNotNull();
        assertThat(record.getPartitionHour()).isNotNull();
    }

    @Test
    void shouldConvertToAvroGenericRecord() throws Exception {
        PolicyEvent event = createSampleEvent();
        PolicyFlatRecord record = mapper.map(event);

        GenericRecord avroRecord = record.toGenericRecord();

        assertThat(avroRecord.get("policy_id")).isEqualTo("pol-001");
        assertThat(avroRecord.get("event_type")).isEqualTo("POLICY_CREATED");
        assertThat(avroRecord.get("gross_premium")).isEqualTo(1500.00);
        assertThat(avroRecord.get("vehicle_make")).isEqualTo("Toyota");
    }

    @Test
    void shouldHandleNullOptionalFields() throws Exception {
        PolicyEvent event = new PolicyEvent();
        event.setEventId("evt-002");
        event.setEventType("POLICY_CREATED");
        event.setEventTimestamp("2024-06-15T14:30:00Z");
        event.setPolicyId("pol-002");

        PolicyFlatRecord record = mapper.map(event);

        assertThat(record.getPolicyId()).isEqualTo("pol-002");
        assertThat(record.getVehicleMake()).isNull();
        assertThat(record.getGrossPremium()).isNull();
        assertThat(record.getAttributesJson()).isNull();
    }

    private PolicyEvent createSampleEvent() {
        PolicyEvent event = new PolicyEvent();
        event.setEventId("evt-001");
        event.setEventType("POLICY_CREATED");
        event.setEventTimestamp("2024-06-15T14:30:00Z");
        event.setTenantId("tenant-1");
        event.setPolicyId("pol-001");
        event.setPolicyNumber("POL-2024-001");
        event.setVersion(1);
        event.setStatus("ACTIVE");
        event.setProductCode("MOTOR");
        event.setLineOfBusiness("RETAIL");
        event.setEffectiveDate("2024-06-15");
        event.setExpiryDate("2025-06-15");
        event.setInceptionDate("2024-06-15");
        event.setPolicyholderName("John Smith");
        event.setPolicyholderId("cust-001");
        event.setVehicleMake("Toyota");
        event.setVehicleModel("Corolla");
        event.setVehicleYear(2023);
        event.setVehicleRegistration("AB12 CDE");
        event.setGrossPremium(1500.00);
        event.setNetPremium(1350.00);
        event.setCurrency("GBP");
        event.setSumInsured(25000.00);
        event.setBrokerCode("BRK-001");
        event.setAgentCode("AGT-001");
        event.setValidFrom("2024-06-15T00:00:00Z");
        event.setValidTo("9999-12-31T23:59:59Z");
        event.setSourceSystem("beema-kernel");
        event.setCorrelationId("corr-001");
        return event;
    }
}
