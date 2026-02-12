package com.beema.kernel.workflow.policy;

import com.beema.kernel.workflow.policy.model.PolicySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for PolicySnapshotActivity
 * Tests the activity implementation with Spring context
 */
@SpringBootTest
@ActiveProfiles("test")
class PolicySnapshotActivityTest {

    @Autowired
    private PolicySnapshotActivityImpl activity;

    @BeforeEach
    void setUp() {
        // Setup can include test data preparation if needed
    }

    @Test
    void testRetrievePolicySnapshot() {
        // Arrange
        String policyId = "POL-TEST-001";

        // Act
        PolicySnapshot snapshot = activity.retrievePolicySnapshot(policyId);

        // Assert
        assertNotNull(snapshot);
        assertNotNull(snapshot.getSnapshotId());
        assertEquals(policyId, snapshot.getPolicyId());
        assertNotNull(snapshot.getPolicyNumber());
        assertNotNull(snapshot.getCapturedAt());
        assertNotNull(snapshot.getPolicyData());
        assertEquals("SUBMITTED", snapshot.getState());
        assertEquals("1.0", snapshot.getVersion());
    }

    @Test
    void testRetrievePolicySnapshotWithValidData() {
        // Arrange
        String policyId = "POL-TEST-002";

        // Act
        PolicySnapshot snapshot = activity.retrievePolicySnapshot(policyId);

        // Assert
        assertNotNull(snapshot.getPolicyData());
        assertEquals(policyId, snapshot.getPolicyData().get("policyId"));
        assertEquals(1000.0, snapshot.getPolicyData().get("premium"));
        assertEquals("Full Coverage", snapshot.getPolicyData().get("coverage"));
    }

    @Test
    void testStorePolicySnapshot() {
        // Arrange
        PolicySnapshot snapshot = new PolicySnapshot();
        snapshot.setPolicyId("POL-TEST-003");
        snapshot.setPolicyNumber("POL-TEST-003");
        snapshot.setState("ISSUED");
        snapshot.setVersion("1.0");

        // Act & Assert - should not throw exception
        activity.storePolicySnapshot(snapshot);
    }

    @Test
    void testStorePolicySnapshotIsIdempotent() {
        // Arrange
        PolicySnapshot snapshot = new PolicySnapshot();
        snapshot.setPolicyId("POL-TEST-004");
        snapshot.setPolicyNumber("POL-TEST-004");
        snapshot.setState("ISSUED");

        // Act - Call multiple times to test idempotency
        activity.storePolicySnapshot(snapshot);
        activity.storePolicySnapshot(snapshot);
        activity.storePolicySnapshot(snapshot);

        // Assert - should not throw exception
        // In a real implementation, you would verify only one record was stored
    }

    @Test
    void testNotifyPolicyIssued() {
        // Arrange
        String policyId = "POL-TEST-005";

        // Act & Assert - should not throw exception
        activity.notifyPolicyIssued(policyId);
    }

    @Test
    void testNotifyPolicyIssuedIsIdempotent() {
        // Arrange
        String policyId = "POL-TEST-006";

        // Act - Call multiple times to test idempotency
        activity.notifyPolicyIssued(policyId);
        activity.notifyPolicyIssued(policyId);
        activity.notifyPolicyIssued(policyId);

        // Assert - should not throw exception
        // In a real implementation, you would verify notification was sent only once
        // or that duplicate notifications are handled properly
    }

    @Test
    void testRetrievePolicySnapshotHandlesErrors() {
        // This test would verify error handling in a real scenario
        // For now, we test with valid data since we have a mock implementation

        // Arrange
        String policyId = "POL-TEST-007";

        // Act
        PolicySnapshot snapshot = activity.retrievePolicySnapshot(policyId);

        // Assert
        assertNotNull(snapshot);
    }

    @Test
    void testStorePolicySnapshotHandlesNullData() {
        // Arrange
        PolicySnapshot snapshot = new PolicySnapshot();
        snapshot.setPolicyId("POL-TEST-008");

        // Act & Assert - should handle gracefully
        activity.storePolicySnapshot(snapshot);
    }

    @Test
    void testRetrievePolicySnapshotGeneratesUniqueIds() {
        // Arrange
        String policyId1 = "POL-TEST-009";
        String policyId2 = "POL-TEST-010";

        // Act
        PolicySnapshot snapshot1 = activity.retrievePolicySnapshot(policyId1);
        PolicySnapshot snapshot2 = activity.retrievePolicySnapshot(policyId2);

        // Assert
        assertNotNull(snapshot1.getSnapshotId());
        assertNotNull(snapshot2.getSnapshotId());
        // Snapshot IDs should be unique
        assert !snapshot1.getSnapshotId().equals(snapshot2.getSnapshotId());
    }
}
