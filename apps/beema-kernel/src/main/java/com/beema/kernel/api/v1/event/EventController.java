package com.beema.kernel.api.v1.event;

import com.beema.kernel.event.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for testing domain event publishing
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final DomainEventPublisher eventPublisher;

    public EventController(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Test endpoint for PolicyBound event
     */
    @PostMapping("/test/policy-bound")
    public ResponseEntity<Map<String, String>> testPolicyBound() {
        PolicyBoundEvent event = new PolicyBoundEvent(
            "POL-TEST-001",
            "agreement-123",
            "RETAIL"
        );
        event.withUser("user-1", "test@beema.io");
        event.withData("premium", 1250.00);
        event.withData("productType", "motor_comprehensive");

        eventPublisher.publish(event);

        return ResponseEntity.ok(Map.of(
            "message", "PolicyBound event published",
            "eventId", event.getEventId(),
            "eventName", event.getEventName()
        ));
    }

    /**
     * Test endpoint for ClaimOpened event
     */
    @PostMapping("/test/claim-opened")
    public ResponseEntity<Map<String, String>> testClaimOpened() {
        ClaimOpenedEvent event = new ClaimOpenedEvent(
            "CLM-TEST-001",
            "claim-123",
            5000.00,
            "motor_accident"
        );
        event.withUser("user-1", "test@beema.io");
        event.withData("claimDate", "2026-02-12");
        event.withData("location", "Nairobi");

        eventPublisher.publish(event);

        return ResponseEntity.ok(Map.of(
            "message", "ClaimOpened event published",
            "eventId", event.getEventId(),
            "eventName", event.getEventName()
        ));
    }

    /**
     * Test endpoint for ClaimSettled event
     */
    @PostMapping("/test/claim-settled")
    public ResponseEntity<Map<String, String>> testClaimSettled() {
        ClaimSettledEvent event = new ClaimSettledEvent(
            "CLM-TEST-001",
            4500.00,
            "full_settlement"
        );
        event.withUser("user-1", "test@beema.io");
        event.withData("settlementDate", "2026-03-15");

        eventPublisher.publish(event);

        return ResponseEntity.ok(Map.of(
            "message", "ClaimSettled event published",
            "eventId", event.getEventId(),
            "eventName", event.getEventName()
        ));
    }

    /**
     * Test endpoint for PolicyRenewed event
     */
    @PostMapping("/test/policy-renewed")
    public ResponseEntity<Map<String, String>> testPolicyRenewed() {
        PolicyRenewedEvent event = new PolicyRenewedEvent(
            "POL-2025-001",
            "POL-2026-001",
            java.time.OffsetDateTime.now()
        );
        event.withUser("user-1", "test@beema.io");
        event.withData("premiumChange", 5.5);

        eventPublisher.publish(event);

        return ResponseEntity.ok(Map.of(
            "message", "PolicyRenewed event published",
            "eventId", event.getEventId(),
            "eventName", event.getEventName()
        ));
    }

    /**
     * Publish a custom event
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, String>> publishCustomEvent(
            @RequestBody Map<String, Object> payload) {

        String eventName = (String) payload.get("eventName");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        DomainEvent event = new DomainEvent(eventName) {};
        if (data != null) {
            event.setData(data);
        }

        eventPublisher.publish(event);

        return ResponseEntity.ok(Map.of(
            "message", "Event published: " + eventName,
            "eventId", event.getEventId(),
            "eventName", event.getEventName()
        ));
    }

    /**
     * Test batch event publishing
     */
    @PostMapping("/test/batch")
    public ResponseEntity<Map<String, String>> testBatchPublish() {
        PolicyBoundEvent event1 = new PolicyBoundEvent("POL-BATCH-001", "agr-1", "RETAIL");
        PolicyBoundEvent event2 = new PolicyBoundEvent("POL-BATCH-002", "agr-2", "COMMERCIAL");
        ClaimOpenedEvent event3 = new ClaimOpenedEvent("CLM-BATCH-001", "clm-1", 1000.00, "property");

        eventPublisher.publishBatch(event1, event2, event3);

        return ResponseEntity.ok(Map.of(
            "message", "Batch of 3 events published",
            "count", "3"
        ));
    }
}
