package com.beema.processor.processor;

import com.beema.processor.model.MessageHook;
import com.beema.processor.model.ProcessedMessage;
import com.beema.processor.model.RawMessage;
import com.beema.processor.repository.MessageHookRepository;
import com.beema.processor.service.JexlTransformService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HookApplierTest {

    @Mock
    private MessageHookRepository repository;

    private JexlTransformService jexlService;
    private HookApplier hookApplier;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jexlService = new JexlTransformService();
        hookApplier = new HookApplier(repository, jexlService);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testApplyHookSuccess() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "policyRef": "pol-12345",
                    "customer": {
                        "firstName": "John",
                        "lastName": "Doe"
                    },
                    "policy": {
                        "premium": 1000.00
                    }
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        RawMessage rawMessage = new RawMessage(
                "msg-001",
                "policy_created",
                "legacy_system",
                payload,
                Instant.now()
        );

        String fieldMappingJson = """
                {
                    "policy_number": {
                        "jexl": "message.policyRef.toUpperCase()"
                    },
                    "policy_holder_name": {
                        "jexl": "message.customer.firstName + ' ' + message.customer.lastName"
                    },
                    "premium_amount": {
                        "jexl": "message.policy.premium * 1.05"
                    }
                }
                """;
        JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);

        MessageHook hook = new MessageHook();
        hook.setHookId(1L);
        hook.setHookName("legacy_policy_transform");
        hook.setMessageType("policy_created");
        hook.setSourceSystem("legacy_system");
        hook.setFieldMapping(fieldMapping);
        hook.setEnabled(true);

        when(repository.findHookForMessage("policy_created", "legacy_system"))
                .thenReturn(Optional.of(hook));

        // Act
        Optional<ProcessedMessage> result = hookApplier.apply(rawMessage);

        // Assert
        assertThat(result).isPresent();
        ProcessedMessage processed = result.get();
        assertThat(processed.getMessageId()).isEqualTo("msg-001");
        assertThat(processed.getHookName()).isEqualTo("legacy_policy_transform");
        assertThat(processed.getTransformedData()).isNotNull();
        assertThat(processed.getTransformedData().get("policy_number")).isEqualTo("POL-12345");
        assertThat(processed.getTransformedData().get("policy_holder_name")).isEqualTo("John Doe");
        assertThat(processed.getTransformedData().get("premium_amount")).isEqualTo(1050.00);
    }

    @Test
    void testApplyHookNoHookFound() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "policyRef": "pol-12345"
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        RawMessage rawMessage = new RawMessage(
                "msg-002",
                "unknown_type",
                "unknown_system",
                payload,
                Instant.now()
        );

        when(repository.findHookForMessage(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        Optional<ProcessedMessage> result = hookApplier.apply(rawMessage);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void testApplyHookWithComplexJexl() throws Exception {
        // Arrange
        String payloadJson = """
                {
                    "slip": {
                        "totalPremium": 50000.00,
                        "totalLine": 100
                    }
                }
                """;
        JsonNode payload = objectMapper.readTree(payloadJson);

        RawMessage rawMessage = new RawMessage(
                "msg-003",
                "slip_created",
                "london_market",
                payload,
                Instant.now()
        );

        String fieldMappingJson = """
                {
                    "premium": {
                        "jexl": "message.slip.totalPremium"
                    },
                    "line_percentage": {
                        "jexl": "message.slip.totalLine"
                    },
                    "net_premium": {
                        "jexl": "message.slip.totalPremium * (message.slip.totalLine / 100.0)"
                    }
                }
                """;
        JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);

        MessageHook hook = new MessageHook();
        hook.setHookId(5L);
        hook.setHookName("london_market_slip_transform");
        hook.setMessageType("slip_created");
        hook.setSourceSystem("london_market");
        hook.setFieldMapping(fieldMapping);
        hook.setEnabled(true);

        when(repository.findHookForMessage("slip_created", "london_market"))
                .thenReturn(Optional.of(hook));

        // Act
        Optional<ProcessedMessage> result = hookApplier.apply(rawMessage);

        // Assert
        assertThat(result).isPresent();
        ProcessedMessage processed = result.get();
        assertThat(processed.getTransformedData().get("premium")).isEqualTo(50000.00);
        assertThat(processed.getTransformedData().get("line_percentage")).isEqualTo(100.0);
        assertThat(processed.getTransformedData().get("net_premium")).isEqualTo(50000.00);
    }
}
