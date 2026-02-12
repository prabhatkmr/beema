package com.beema.processor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JexlTransformServiceTest {

    private JexlTransformService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = new JexlTransformService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testSimpleFieldMapping() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "policyRef": "pol-12345",
                    "customer": {
                        "firstName": "John",
                        "lastName": "Doe"
                    },
                    "policy": {
                        "premium": 1000.00,
                        "currency": "GBP"
                    }
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        String fieldMappingJson = """
                {
                    "policy_number": {
                        "source": "policyRef",
                        "jexl": "message.policyRef.toUpperCase()"
                    },
                    "policy_holder_name": {
                        "source": "customerName",
                        "jexl": "message.customer.firstName + ' ' + message.customer.lastName"
                    },
                    "premium_amount": {
                        "source": "premium",
                        "jexl": "message.policy.premium"
                    }
                }
                """;
        JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);

        // Act
        Map<String, Object> result = service.transformMessage(payload, fieldMapping);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("policy_number")).isEqualTo("POL-12345");
        assertThat(result.get("policy_holder_name")).isEqualTo("John Doe");
        assertThat(result.get("premium_amount")).isEqualTo(1000.00);
    }

    @Test
    void testArithmeticExpression() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "policy": {
                        "premium": 1000.00
                    }
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        String fieldMappingJson = """
                {
                    "premium_with_tax": {
                        "jexl": "message.policy.premium * 1.05"
                    }
                }
                """;
        JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);

        // Act
        Map<String, Object> result = service.transformMessage(payload, fieldMapping);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("premium_with_tax")).isEqualTo(1050.00);
    }

    @Test
    void testConditionalExpression() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "payment": {
                        "status": "SUCCESS",
                        "currency": null
                    }
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        String fieldMappingJson = """
                {
                    "status": {
                        "jexl": "message.payment.status == 'SUCCESS' ? 'COMPLETED' : 'PENDING'"
                    },
                    "currency": {
                        "jexl": "message.payment.currency != null ? message.payment.currency : 'GBP'"
                    }
                }
                """;
        JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);

        // Act
        Map<String, Object> result = service.transformMessage(payload, fieldMapping);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("COMPLETED");
        assertThat(result.get("currency")).isEqualTo("GBP");
    }

    @Test
    void testNullHandling() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "customer": {
                        "firstName": "Jane"
                    }
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        String fieldMappingJson = """
                {
                    "full_name": {
                        "jexl": "message.customer.firstName + ' ' + message.customer.lastName"
                    }
                }
                """;
        JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);

        // Act
        Map<String, Object> result = service.transformMessage(payload, fieldMapping);

        // Assert - JEXL should handle null gracefully
        assertThat(result).isNotNull();
        assertThat(result.containsKey("full_name")).isTrue();
    }

    @Test
    void testValidJexlSyntax() {
        assertThat(service.isValidJexlSyntax("message.policy.premium * 1.05")).isTrue();
        assertThat(service.isValidJexlSyntax("message.customer.firstName + ' ' + message.customer.lastName")).isTrue();
        assertThat(service.isValidJexlSyntax("message.status == 'ACTIVE'")).isTrue();
    }

    @Test
    void testInvalidJexlSyntax() {
        assertThat(service.isValidJexlSyntax("message.policy.premium * * 1.05")).isFalse();
        assertThat(service.isValidJexlSyntax("")).isFalse();
        assertThat(service.isValidJexlSyntax(null)).isFalse();
    }
}
