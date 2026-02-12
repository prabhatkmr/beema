package com.beema.kernel.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AgreementWorkflow
 *
 * Uses Temporal TestWorkflowEnvironment for in-memory workflow testing
 */
class AgreementWorkflowTest {

    private static final String TASK_QUEUE = "BEEMA_AGREEMENT_TASK_QUEUE_TEST";

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(AgreementWorkflowImpl.class)
                    .setDoNotStart(true)
                    .build();

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient workflowClient;

    @Mock
    private WorkflowActivities mockActivities;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Create test environment
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);

        // Register workflow and mock activities
        worker.registerWorkflowImplementationTypes(AgreementWorkflowImpl.class);
        worker.registerActivitiesImplementations(mockActivities);

        // Start test environment
        testEnv.start();

        // Get workflow client
        workflowClient = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        testEnv.close();
        mocks.close();
    }

    @Test
    void testAgreementCreatedWorkflow_WithValidHooks() {
        // Arrange
        String eventType = "agreement.created";
        Map<String, Object> agreementData = createTestAgreementData(100000);

        List<Map<String, Object>> hooks = createTestHooks();
        when(mockActivities.fetchWorkflowHooks(eventType)).thenReturn(hooks);
        when(mockActivities.evaluateExpression(any(), anyString())).thenReturn(true);
        when(mockActivities.capturePolicySnapshot(any(), anyString(), anyBoolean()))
                .thenReturn(createSnapshotResponse());

        // Act
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("test-workflow-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build();

        AgreementWorkflow workflow = workflowClient.newWorkflowStub(AgreementWorkflow.class, options);
        Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.get("status"));
        assertEquals(eventType, result.get("eventType"));
        assertTrue(result.containsKey("actionResults"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actionResults = (List<Map<String, Object>>) result.get("actionResults");
        assertNotNull(actionResults);
        assertTrue(actionResults.size() > 0);

        // Verify activities were called
        verify(mockActivities).fetchWorkflowHooks(eventType);
        verify(mockActivities, atLeastOnce()).evaluateExpression(any(), anyString());
        verify(mockActivities).persistWorkflowResult(anyString(), anyString(), eq(eventType), any(), any());
    }

    @Test
    void testAgreementWorkflow_NoHooksConfigured() {
        // Arrange
        String eventType = "agreement.updated";
        Map<String, Object> agreementData = createTestAgreementData(50000);

        when(mockActivities.fetchWorkflowHooks(eventType)).thenReturn(Collections.emptyList());

        // Act
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("test-workflow-no-hooks-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build();

        AgreementWorkflow workflow = workflowClient.newWorkflowStub(AgreementWorkflow.class, options);
        Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

        // Assert
        assertNotNull(result);
        assertEquals("NO_HOOKS", result.get("status"));
        verify(mockActivities).fetchWorkflowHooks(eventType);
        verify(mockActivities).persistWorkflowResult(anyString(), anyString(), eq(eventType), any(), any());
    }

    @Test
    void testAgreementWorkflow_ConditionNotMet() {
        // Arrange
        String eventType = "agreement.created";
        Map<String, Object> agreementData = createTestAgreementData(50000); // Below threshold

        List<Map<String, Object>> hooks = createTestHooks();
        when(mockActivities.fetchWorkflowHooks(eventType)).thenReturn(hooks);
        when(mockActivities.evaluateExpression(any(), anyString())).thenReturn(false); // Condition fails

        // Act
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("test-workflow-condition-failed-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build();

        AgreementWorkflow workflow = workflowClient.newWorkflowStub(AgreementWorkflow.class, options);
        Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.get("status"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actionResults = (List<Map<String, Object>>) result.get("actionResults");
        assertNotNull(actionResults);

        // Verify that actions were skipped
        for (Map<String, Object> actionResult : actionResults) {
            assertEquals("SKIPPED", actionResult.get("status"));
        }

        verify(mockActivities, never()).capturePolicySnapshot(any(), anyString(), anyBoolean());
        verify(mockActivities, never()).executeWebhook(anyString(), anyString(), any(), any());
    }

    @Test
    void testAgreementWorkflow_WebhookAction() {
        // Arrange
        String eventType = "agreement.created";
        Map<String, Object> agreementData = createTestAgreementData(150000);

        List<Map<String, Object>> hooks = createWebhookHook();
        when(mockActivities.fetchWorkflowHooks(eventType)).thenReturn(hooks);
        when(mockActivities.evaluateExpression(any(), anyString())).thenReturn(true);
        when(mockActivities.executeWebhook(anyString(), anyString(), any(), any()))
                .thenReturn(createWebhookResponse());

        // Act
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("test-workflow-webhook-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build();

        AgreementWorkflow workflow = workflowClient.newWorkflowStub(AgreementWorkflow.class, options);
        Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.get("status"));

        verify(mockActivities).executeWebhook(anyString(), eq("POST"), any(), any());
    }

    @Test
    void testAgreementWorkflow_SnapshotAction() {
        // Arrange
        String eventType = "agreement.created";
        Map<String, Object> agreementData = createTestAgreementData(100000);

        List<Map<String, Object>> hooks = createSnapshotHook();
        when(mockActivities.fetchWorkflowHooks(eventType)).thenReturn(hooks);
        when(mockActivities.evaluateExpression(any(), anyString())).thenReturn(true);
        when(mockActivities.capturePolicySnapshot(any(), anyString(), anyBoolean()))
                .thenReturn(createSnapshotResponse());

        // Act
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("test-workflow-snapshot-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build();

        AgreementWorkflow workflow = workflowClient.newWorkflowStub(AgreementWorkflow.class, options);
        Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.get("status"));

        verify(mockActivities).capturePolicySnapshot(eq(agreementData),
                eq("/mock-policy-api/snapshots"), eq(false));
    }

    @Test
    void testAgreementWorkflow_ExpressionAction() {
        // Arrange
        String eventType = "agreement.updated";
        Map<String, Object> agreementData = createTestAgreementData(80000);

        List<Map<String, Object>> hooks = createExpressionHook();
        when(mockActivities.fetchWorkflowHooks(eventType)).thenReturn(hooks);
        when(mockActivities.evaluateExpression(any(), anyString())).thenReturn(true);
        when(mockActivities.evaluateExpressionForResult(any(), anyString())).thenReturn(850.0);

        // Act
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("test-workflow-expression-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build();

        AgreementWorkflow workflow = workflowClient.newWorkflowStub(AgreementWorkflow.class, options);
        Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.get("status"));

        verify(mockActivities).evaluateExpressionForResult(eq(agreementData), anyString());
    }

    // Helper methods

    private Map<String, Object> createTestAgreementData(int premiumAmount) {
        Map<String, Object> data = new HashMap<>();
        data.put("agreementId", 12345L);
        data.put("agreementType", "POLICY");
        data.put("premiumAmount", premiumAmount);
        data.put("marketType", "LONDON_MARKET");
        data.put("lineOfBusiness", "COMMERCIAL");
        data.put("status", "ACTIVE");
        return data;
    }

    private List<Map<String, Object>> createTestHooks() {
        List<Map<String, Object>> hooks = new ArrayList<>();

        Map<String, Object> hook = new HashMap<>();
        hook.put("hookName", "test_snapshot_hook");
        hook.put("eventType", "agreement.created");
        hook.put("triggerCondition", "agreement != null && agreement.agreementType != null");
        hook.put("actionType", "snapshot");

        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("endpoint", "/mock-policy-api/snapshots");
        actionConfig.put("method", "POST");
        hook.put("actionConfig", actionConfig);

        hooks.add(hook);
        return hooks;
    }

    private List<Map<String, Object>> createWebhookHook() {
        List<Map<String, Object>> hooks = new ArrayList<>();

        Map<String, Object> hook = new HashMap<>();
        hook.put("hookName", "test_webhook_hook");
        hook.put("eventType", "agreement.created");
        hook.put("triggerCondition", "agreement.premiumAmount > 100000");
        hook.put("actionType", "webhook");

        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("url", "https://webhook.site/test");
        actionConfig.put("method", "POST");
        hook.put("actionConfig", actionConfig);

        hooks.add(hook);
        return hooks;
    }

    private List<Map<String, Object>> createSnapshotHook() {
        List<Map<String, Object>> hooks = new ArrayList<>();

        Map<String, Object> hook = new HashMap<>();
        hook.put("hookName", "test_snapshot_hook");
        hook.put("eventType", "agreement.created");
        hook.put("triggerCondition", "agreement != null");
        hook.put("actionType", "snapshot");

        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("endpoint", "/mock-policy-api/snapshots");
        hook.put("actionConfig", actionConfig);

        hooks.add(hook);
        return hooks;
    }

    private List<Map<String, Object>> createExpressionHook() {
        List<Map<String, Object>> hooks = new ArrayList<>();

        Map<String, Object> hook = new HashMap<>();
        hook.put("hookName", "test_expression_hook");
        hook.put("eventType", "agreement.updated");
        hook.put("triggerCondition", "agreement.status == 'ACTIVE'");
        hook.put("actionType", "expression");

        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("expression", "agreement.premiumAmount / 100");
        actionConfig.put("resultField", "calculatedValue");
        hook.put("actionConfig", actionConfig);

        hooks.add(hook);
        return hooks;
    }

    private Map<String, Object> createSnapshotResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("snapshotId", UUID.randomUUID().toString());
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "CAPTURED");
        return response;
    }

    private Map<String, Object> createWebhookResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("success", true);
        response.put("responseBody", "{\"status\":\"received\"}");
        return response;
    }
}
