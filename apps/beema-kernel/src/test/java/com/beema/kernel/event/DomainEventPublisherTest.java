package com.beema.kernel.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DomainEventPublisher
 */
@SpringBootTest
@TestPropertySource(properties = {
    "beema.events.publisher.enabled=true",
    "inngest.base-url=http://localhost:8288",
    "inngest.event-key=test"
})
class DomainEventPublisherTest {

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Test
    void publishPolicyBoundEvent_shouldSucceed() {
        // Given
        PolicyBoundEvent event = new PolicyBoundEvent(
            "POL-TEST-001",
            "agreement-123",
            "RETAIL"
        );
        event.withData("premium", 1250.00);
        event.withUser("test-user", "test@beema.io");

        // When
        eventPublisher.publish(event);

        // Then - verify event structure
        assertThat(event.getEventName()).isEqualTo("policy/bound");
        assertThat(event.getData()).containsKey("policyNumber");
        assertThat(event.getData()).containsKey("agreementId");
        assertThat(event.getData()).containsKey("marketContext");
        assertThat(event.getData()).containsKey("premium");
        assertThat(event.getUser()).containsEntry("id", "test-user");
        assertThat(event.getUser()).containsEntry("email", "test@beema.io");
    }

    @Test
    void publishClaimOpenedEvent_shouldSucceed() {
        // Given
        ClaimOpenedEvent event = new ClaimOpenedEvent(
            "CLM-TEST-001",
            "claim-123",
            5000.00,
            "motor_accident"
        );
        event.withUser("test-user", "test@beema.io");

        // When
        eventPublisher.publish(event);

        // Then
        assertThat(event.getEventName()).isEqualTo("claim/opened");
        assertThat(event.getData().get("claimAmount")).isEqualTo(5000.00);
        assertThat(event.getData().get("claimType")).isEqualTo("motor_accident");
    }

    @Test
    void publishClaimSettledEvent_shouldSucceed() {
        // Given
        ClaimSettledEvent event = new ClaimSettledEvent(
            "CLM-TEST-002",
            4500.00,
            "full_settlement"
        );
        event.withUser("test-user", "test@beema.io");

        // When
        eventPublisher.publish(event);

        // Then
        assertThat(event.getEventName()).isEqualTo("claim/settled");
        assertThat(event.getData().get("settlementAmount")).isEqualTo(4500.00);
    }

    @Test
    void publishPolicyRenewedEvent_shouldSucceed() {
        // Given
        OffsetDateTime renewalDate = OffsetDateTime.now();
        PolicyRenewedEvent event = new PolicyRenewedEvent(
            "POL-2025-001",
            "POL-2026-001",
            renewalDate
        );
        event.withUser("test-user", "test@beema.io");

        // When
        eventPublisher.publish(event);

        // Then
        assertThat(event.getEventName()).isEqualTo("policy/renewed");
        assertThat(event.getData()).containsKey("policyNumber");
        assertThat(event.getData()).containsKey("newPolicyNumber");
        assertThat(event.getData()).containsKey("renewalDate");
    }

    @Test
    void publishAgreementUpdatedEvent_shouldSucceed() {
        // Given
        Map<String, Object> changes = Map.of(
            "status", "ACTIVE",
            "premium", 1500.00
        );
        AgreementUpdatedEvent event = new AgreementUpdatedEvent(
            "agreement-123",
            "premium_adjustment",
            changes
        );
        event.withUser("test-user", "test@beema.io");

        // When
        eventPublisher.publish(event);

        // Then
        assertThat(event.getEventName()).isEqualTo("agreement/updated");
        assertThat(event.getData().get("changeType")).isEqualTo("premium_adjustment");
    }

    @Test
    void publishBatch_shouldPublishMultipleEvents() {
        // Given
        PolicyBoundEvent event1 = new PolicyBoundEvent("POL-001", "agr-1", "RETAIL");
        PolicyBoundEvent event2 = new PolicyBoundEvent("POL-002", "agr-2", "COMMERCIAL");
        ClaimOpenedEvent event3 = new ClaimOpenedEvent("CLM-001", "clm-1", 1000.00, "property");

        // When
        eventPublisher.publishBatch(event1, event2, event3);

        // Then - verify all events have correct structure
        assertThat(event1.getEventName()).isEqualTo("policy/bound");
        assertThat(event2.getEventName()).isEqualTo("policy/bound");
        assertThat(event3.getEventName()).isEqualTo("claim/opened");
    }

    @Test
    void publishWithMetadata_shouldAddTenantAndUserInfo() {
        // Given
        PolicyBoundEvent event = new PolicyBoundEvent(
            "POL-TEST-003",
            "agreement-456",
            "LONDON_MARKET"
        );

        // When
        eventPublisher.publishWithMetadata(event, "tenant-123", "user-456", "user@example.com");

        // Then
        assertThat(event.getData()).containsEntry("tenantId", "tenant-123");
        assertThat(event.getUser()).containsEntry("id", "user-456");
        assertThat(event.getUser()).containsEntry("email", "user@example.com");
    }

    @Test
    void eventShouldHaveUniqueId() {
        // Given
        PolicyBoundEvent event1 = new PolicyBoundEvent();
        PolicyBoundEvent event2 = new PolicyBoundEvent();

        // Then
        assertThat(event1.getEventId()).isNotNull();
        assertThat(event2.getEventId()).isNotNull();
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void eventShouldHaveTimestamp() {
        // Given
        long before = System.currentTimeMillis();
        PolicyBoundEvent event = new PolicyBoundEvent();
        long after = System.currentTimeMillis();

        // Then
        assertThat(event.getTimestamp()).isGreaterThanOrEqualTo(before);
        assertThat(event.getTimestamp()).isLessThanOrEqualTo(after);
    }
}
