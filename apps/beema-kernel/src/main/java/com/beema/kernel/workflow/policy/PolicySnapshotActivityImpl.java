package com.beema.kernel.workflow.policy;

import com.beema.kernel.workflow.policy.model.PolicySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Policy Snapshot Activity Implementation
 *
 * Implements retryable activities for policy snapshot operations.
 * All activities are designed to be idempotent for safe retries.
 */
@Component
public class PolicySnapshotActivityImpl implements PolicySnapshotActivity {

    private static final Logger log = LoggerFactory.getLogger(PolicySnapshotActivityImpl.class);
    private static final Duration ACTIVITY_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    public PolicySnapshotActivityImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8080") // Default base URL, should be configurable
                .build();
    }

    /**
     * Retrieves policy snapshot from the policy system.
     * Retryable operation - may fail on network issues but will be retried by Temporal.
     *
     * @param policyId The policy ID to retrieve
     * @return PolicySnapshot with policy data
     */
    @Override
    public PolicySnapshot retrievePolicySnapshot(String policyId) {
        log.info("Retrieving policy snapshot for policyId: {}", policyId);

        try {
            // In a real implementation, this would call an external policy service
            // For now, we'll create a mock response or call a local endpoint
            PolicySnapshot snapshot = fetchPolicyFromSystem(policyId);

            log.info("Successfully retrieved policy snapshot: snapshotId={}, policyId={}",
                    snapshot.getSnapshotId(), policyId);
            return snapshot;

        } catch (Exception e) {
            log.error("Failed to retrieve policy snapshot for policyId: {}", policyId, e);
            // Temporal will retry this activity based on retry configuration
            throw new RuntimeException("Failed to retrieve policy snapshot: " + e.getMessage(), e);
        }
    }

    /**
     * Stores policy snapshot to persistent storage.
     * Idempotent operation - safe to call multiple times with same data.
     *
     * @param snapshot The policy snapshot to store
     */
    @Override
    public void storePolicySnapshot(PolicySnapshot snapshot) {
        log.info("Storing policy snapshot: snapshotId={}, policyId={}",
                snapshot.getSnapshotId(), snapshot.getPolicyId());

        try {
            // In a real implementation, this would:
            // 1. Store to database
            // 2. Store to file system
            // 3. Store to object storage (S3, etc.)

            // For now, we'll log the operation
            // This should be idempotent - checking if snapshot already exists before storing
            persistSnapshotToStorage(snapshot);

            log.info("Successfully stored policy snapshot: snapshotId={}", snapshot.getSnapshotId());

        } catch (Exception e) {
            log.error("Failed to store policy snapshot: snapshotId={}", snapshot.getSnapshotId(), e);
            throw new RuntimeException("Failed to store policy snapshot: " + e.getMessage(), e);
        }
    }

    /**
     * Sends notification that policy has been issued.
     * Idempotent operation - safe to call multiple times.
     *
     * @param policyId The policy ID that was issued
     */
    @Override
    public void notifyPolicyIssued(String policyId) {
        log.info("Sending policy issued notification for policyId: {}", policyId);

        try {
            // In a real implementation, this would:
            // 1. Send email notification
            // 2. Trigger webhook
            // 3. Publish to message queue
            // 4. Update external systems

            sendNotification(policyId);

            log.info("Successfully sent policy issued notification for policyId: {}", policyId);

        } catch (Exception e) {
            log.error("Failed to send notification for policyId: {}", policyId, e);
            // Depending on requirements, notification failures might be non-critical
            // For now, we'll let it retry
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches policy data from the policy system.
     * This is a mock implementation - in production, this would call a real API.
     */
    private PolicySnapshot fetchPolicyFromSystem(String policyId) {
        // Mock implementation - replace with actual HTTP call in production
        // Example of how to make the call with WebClient:
        /*
        return webClient.get()
                .uri("/api/v1/policies/{policyId}", policyId)
                .retrieve()
                .bodyToMono(PolicySnapshot.class)
                .timeout(HTTP_TIMEOUT)
                .block();
        */

        // Mock response for demonstration
        Map<String, Object> policyData = new HashMap<>();
        policyData.put("policyId", policyId);
        policyData.put("premium", 1000.0);
        policyData.put("coverage", "Full Coverage");
        policyData.put("effectiveDate", Instant.now().toString());

        PolicySnapshot snapshot = new PolicySnapshot();
        snapshot.setPolicyId(policyId);
        snapshot.setPolicyNumber("POL-" + policyId);
        snapshot.setState("SUBMITTED");
        snapshot.setPolicyData(policyData);
        snapshot.setVersion("1.0");
        snapshot.setCapturedAt(Instant.now());

        return snapshot;
    }

    /**
     * Persists snapshot to storage.
     * This is a mock implementation - in production, this would store to database/file system.
     */
    private void persistSnapshotToStorage(PolicySnapshot snapshot) {
        // Mock implementation - replace with actual persistence logic
        // In production, this would:
        // 1. Check if snapshot already exists (idempotency)
        // 2. Store to database with proper transactions
        // 3. Store documents to object storage

        log.debug("Persisting snapshot to storage: {}", snapshot);

        // Simulate storage operation
        try {
            Thread.sleep(100); // Simulate I/O
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Storage operation interrupted", e);
        }
    }

    /**
     * Sends notification about policy issuance.
     * This is a mock implementation - in production, this would send real notifications.
     */
    private void sendNotification(String policyId) {
        // Mock implementation - replace with actual notification logic
        // In production, this would:
        // 1. Send email via email service
        // 2. Trigger webhooks to external systems
        // 3. Publish events to message queue

        log.debug("Sending notification for policyId: {}", policyId);

        // Example of how to make webhook call with WebClient:
        /*
        webClient.post()
                .uri("/api/v1/notifications/policy-issued")
                .bodyValue(Map.of("policyId", policyId, "timestamp", Instant.now()))
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(HTTP_TIMEOUT)
                .block();
        */

        // Simulate notification sending
        try {
            Thread.sleep(50); // Simulate network call
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Notification operation interrupted", e);
        }
    }
}
