package com.beema.streaming.serde;

import com.beema.streaming.model.PolicyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEventDeserializerTest {

    private PolicyEventDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new PolicyEventDeserializer();
        deserializer.open(null);
    }

    @Test
    void shouldDeserializeValidJson() throws Exception {
        String json = """
                {
                    "event_id": "evt-001",
                    "event_type": "POLICY_CREATED",
                    "event_timestamp": "2024-06-15T14:30:00Z",
                    "tenant_id": "tenant-1",
                    "policy_id": "pol-001",
                    "policy_number": "POL-2024-001",
                    "status": "ACTIVE",
                    "gross_premium": 1500.00,
                    "vehicle_make": "Toyota",
                    "vehicle_model": "Corolla",
                    "vehicle_year": 2023
                }
                """;

        PolicyEvent event = deserializer.deserialize(json.getBytes(StandardCharsets.UTF_8));

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isEqualTo("evt-001");
        assertThat(event.getEventType()).isEqualTo("POLICY_CREATED");
        assertThat(event.getPolicyId()).isEqualTo("pol-001");
        assertThat(event.getGrossPremium()).isEqualTo(1500.00);
        assertThat(event.getVehicleMake()).isEqualTo("Toyota");
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        String json = """
                {
                    "event_id": "evt-002",
                    "unknown_field": "some_value",
                    "another_unknown": 42
                }
                """;

        PolicyEvent event = deserializer.deserialize(json.getBytes(StandardCharsets.UTF_8));

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isEqualTo("evt-002");
    }

    @Test
    void shouldReturnNullForMalformedJson() throws Exception {
        String malformed = "this is not json";

        PolicyEvent event = deserializer.deserialize(malformed.getBytes(StandardCharsets.UTF_8));

        assertThat(event).isNull();
    }

    @Test
    void shouldReturnNullForNullInput() throws Exception {
        PolicyEvent event = deserializer.deserialize(null);
        assertThat(event).isNull();
    }

    @Test
    void shouldReturnNullForEmptyInput() throws Exception {
        PolicyEvent event = deserializer.deserialize(new byte[0]);
        assertThat(event).isNull();
    }

    @Test
    void shouldDeserializeWithAttributes() throws Exception {
        String json = """
                {
                    "event_id": "evt-003",
                    "policy_id": "pol-003",
                    "attributes": {
                        "excess": 500,
                        "ncd_years": 5,
                        "voluntary_excess": true
                    }
                }
                """;

        PolicyEvent event = deserializer.deserialize(json.getBytes(StandardCharsets.UTF_8));

        assertThat(event).isNotNull();
        assertThat(event.getAttributes()).isNotNull();
        assertThat(event.getAttributes()).containsKey("excess");
        assertThat(event.getAttributes().get("excess")).isEqualTo(500);
    }

    @Test
    void shouldNotBeEndOfStream() {
        assertThat(deserializer.isEndOfStream(null)).isFalse();
        assertThat(deserializer.isEndOfStream(new PolicyEvent())).isFalse();
    }
}
