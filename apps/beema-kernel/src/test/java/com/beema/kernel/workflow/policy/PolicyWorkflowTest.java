package com.beema.kernel.workflow.policy;

import com.beema.kernel.workflow.policy.model.PolicySnapshot;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowRequest;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowResult;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PolicyWorkflow using Temporal TestWorkflowEnvironment
 */
class PolicyWorkflowTest {

    private static final String TASK_QUEUE = "test-policy-queue";

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(PolicyWorkflowImpl.class)
                    .setDoNotStart(true)
                    .build();

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private PolicySnapshotActivity mockActivity;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(PolicyWorkflowImpl.class);

        // Create mock activity
        mockActivity = Mockito.mock(PolicySnapshotActivity.class);
        worker.registerActivitiesImplementations(mockActivity);

        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void testSubmittedToIssuedTransition() {
        // Arrange
        String policyId = "POL-001";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("premium", 1000.0);
        metadata.put("coverage", "Full");

        PolicyWorkflowRequest request = new PolicyWorkflowRequest(policyId, "SUBMITTED", metadata);

        PolicySnapshot mockSnapshot = new PolicySnapshot();
        mockSnapshot.setPolicyId(policyId);
        mockSnapshot.setPolicyNumber("POL-001");
        mockSnapshot.setState("SUBMITTED");
        mockSnapshot.setSnapshotId("SNAP-001");
        mockSnapshot.setCapturedAt(Instant.now());

        // Configure mock activity responses
        when(mockActivity.retrievePolicySnapshot(anyString())).thenReturn(mockSnapshot);

        // Act
        PolicyWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                PolicyWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()
        );

        PolicyWorkflowResult result = workflow.processPolicyLifecycle(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("ISSUED", result.getFinalState());
        assertNotNull(result.getSnapshotId());
        assertNotNull(result.getTimestamp());

        // Verify activity calls
        verify(mockActivity, times(1)).retrievePolicySnapshot(policyId);
        verify(mockActivity, times(1)).storePolicySnapshot(any(PolicySnapshot.class));
        verify(mockActivity, times(1)).notifyPolicyIssued(policyId);
    }

    @Test
    void testActivityRetryOnFailure() {
        // Arrange
        String policyId = "POL-002";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("premium", 2000.0);

        PolicyWorkflowRequest request = new PolicyWorkflowRequest(policyId, "SUBMITTED", metadata);

        PolicySnapshot mockSnapshot = new PolicySnapshot();
        mockSnapshot.setPolicyId(policyId);
        mockSnapshot.setPolicyNumber("POL-002");
        mockSnapshot.setState("SUBMITTED");
        mockSnapshot.setSnapshotId("SNAP-002");

        // Configure mock to fail first 2 times, then succeed
        when(mockActivity.retrievePolicySnapshot(anyString()))
                .thenThrow(new RuntimeException("Network error"))
                .thenThrow(new RuntimeException("Network error"))
                .thenReturn(mockSnapshot);

        // Act
        PolicyWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                PolicyWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()
        );

        PolicyWorkflowResult result = workflow.processPolicyLifecycle(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("ISSUED", result.getFinalState());

        // Verify retry happened (3 calls: 2 failures + 1 success)
        verify(mockActivity, times(3)).retrievePolicySnapshot(policyId);
        verify(mockActivity, times(1)).storePolicySnapshot(any(PolicySnapshot.class));
        verify(mockActivity, times(1)).notifyPolicyIssued(policyId);
    }

    @Test
    void testStateQueryDuringExecution() {
        // Arrange
        String policyId = "POL-003";
        Map<String, Object> metadata = new HashMap<>();
        PolicyWorkflowRequest request = new PolicyWorkflowRequest(policyId, "SUBMITTED", metadata);

        PolicySnapshot mockSnapshot = new PolicySnapshot();
        mockSnapshot.setPolicyId(policyId);
        mockSnapshot.setSnapshotId("SNAP-003");

        when(mockActivity.retrievePolicySnapshot(anyString())).thenReturn(mockSnapshot);

        // Act
        PolicyWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                PolicyWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()
        );

        // Start workflow asynchronously
        io.temporal.client.WorkflowClient.start(workflow::processPolicyLifecycle, request);

        // Query state while workflow is running
        testEnv.sleep(java.time.Duration.ofMillis(100));
        String currentState = workflow.getCurrentState();

        // Assert
        assertNotNull(currentState);
        assertTrue(currentState.equals("SUBMITTED") || currentState.equals("ISSUED"));

        // Complete workflow
        PolicyWorkflowResult result = io.temporal.client.WorkflowStub.fromTyped(workflow)
                .getResult(PolicyWorkflowResult.class);
        assertNotNull(result);
    }

    @Test
    void testSignalUpdateState() {
        // Arrange
        String policyId = "POL-004";
        Map<String, Object> metadata = new HashMap<>();
        PolicyWorkflowRequest request = new PolicyWorkflowRequest(policyId, "SUBMITTED", metadata);

        PolicySnapshot mockSnapshot = new PolicySnapshot();
        mockSnapshot.setPolicyId(policyId);
        mockSnapshot.setSnapshotId("SNAP-004");

        when(mockActivity.retrievePolicySnapshot(anyString())).thenReturn(mockSnapshot);

        // Act
        PolicyWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                PolicyWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()
        );

        // Start workflow asynchronously
        io.temporal.client.WorkflowClient.start(workflow::processPolicyLifecycle, request);

        // Send signal to update state
        testEnv.sleep(java.time.Duration.ofMillis(50));
        workflow.updateState("CANCELLED");

        // Query state
        testEnv.sleep(java.time.Duration.ofMillis(50));
        String currentState = workflow.getCurrentState();

        // Assert
        assertEquals("CANCELLED", currentState);
    }

    @Test
    void testValidationFailure() {
        // Arrange - Create request with invalid data (null policyId)
        Map<String, Object> metadata = new HashMap<>();
        PolicyWorkflowRequest request = new PolicyWorkflowRequest(null, "SUBMITTED", metadata);

        // Act
        PolicyWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                PolicyWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()
        );

        PolicyWorkflowResult result = workflow.processPolicyLifecycle(request);

        // Assert
        assertNotNull(result);
        assertTrue(!result.isSuccess());
        assertEquals("VALIDATION_FAILED", result.getFinalState());
        assertTrue(result.getMessage().contains("validation failed"));

        // Verify activities were not called
        verify(mockActivity, times(0)).retrievePolicySnapshot(anyString());
    }
}
