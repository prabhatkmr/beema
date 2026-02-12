package com.beema.kernel.service.workflow;

import com.beema.kernel.domain.WorkflowExecution;
import com.beema.kernel.domain.WorkflowHook;
import com.beema.kernel.repository.WorkflowExecutionRepository;
import com.beema.kernel.repository.WorkflowHookRepository;
import com.beema.kernel.workflow.AgreementWorkflowImpl;
import com.beema.kernel.workflow.WorkflowActivities;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for WorkflowService
 *
 * Tests the full workflow execution flow including:
 * - Starting workflows
 * - Workflow status retrieval
 * - Database interactions
 */
@SpringBootTest
@TestPropertySource(properties = {
        "temporal.worker.enabled=false" // Disable auto-start of worker
})
class WorkflowServiceIntegrationTest {

    private static final String TASK_QUEUE = "BEEMA_AGREEMENT_TASK_QUEUE";

    @MockBean
    private WorkflowHookRepository workflowHookRepository;

    @MockBean
    private WorkflowExecutionRepository workflowExecutionRepository;

    @MockBean
    private WorkflowActivities workflowActivities;

    @Autowired
    private WorkflowService workflowService;

    private TestWorkflowEnvironment testEnv;
    private Worker worker;

    @BeforeEach
    void setUp() {
        // Create test environment
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);

        // Register workflow and activities
        worker.registerWorkflowImplementationTypes(AgreementWorkflowImpl.class);
        worker.registerActivitiesImplementations(workflowActivities);

        // Start test environment
        testEnv.start();

        // Mock workflow hooks
        List<WorkflowHook> hooks = createMockWorkflowHooks();
        when(workflowHookRepository.findEnabledHooksByEventType(anyString())).thenReturn(hooks);

        // Mock activities
        when(workflowActivities.fetchWorkflowHooks(anyString()))
                .thenReturn(convertHooksToMaps(hooks));
        when(workflowActivities.evaluateExpression(any(), anyString())).thenReturn(true);
        when(workflowActivities.capturePolicySnapshot(any(), anyString(), anyBoolean()))
                .thenReturn(createSnapshotResponse());
        doNothing().when(workflowActivities).persistWorkflowResult(
                anyString(), anyString(), anyString(), any(), any());

        // Mock workflow execution repository
        when(workflowExecutionRepository.save(any(WorkflowExecution.class)))
                .thenAnswer(invocation -> {
                    WorkflowExecution execution = invocation.getArgument(0);
                    execution.setExecutionId(1L);
                    return execution;
                });
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    @Test
    void testStartAgreementCreatedWorkflow_Async() {
        // Arrange
        Map<String, Object> agreementData = createTestAgreementData(100000);

        // Act
        WorkflowService.WorkflowExecutionResult result =
                workflowService.startAgreementCreatedWorkflow(agreementData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getWorkflowId());
        assertEquals("agreement.created", result.getEventType());
        assertNull(result.getErrorMessage());

        // Allow workflow to start (async)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void testStartWorkflowSync_CompletesSuccessfully() {
        // Arrange
        Map<String, Object> agreementData = createTestAgreementData(150000);

        // Act
        WorkflowService.WorkflowExecutionResult result =
                workflowService.startWorkflowSync("agreement.created", agreementData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getWorkflowId());
        assertEquals("agreement.created", result.getEventType());
        assertNotNull(result.getResult());

        Map<String, Object> workflowResult = result.getResult();
        assertEquals("COMPLETED", workflowResult.get("status"));
        assertTrue(workflowResult.containsKey("actionResults"));

        // Verify activities were called
        verify(workflowActivities).fetchWorkflowHooks("agreement.created");
        verify(workflowActivities).persistWorkflowResult(
                anyString(), anyString(), eq("agreement.created"), any(), any());
    }

    @Test
    void testStartAgreementUpdatedWorkflow() {
        // Arrange
        Map<String, Object> agreementData = createTestAgreementData(75000);
        agreementData.put("status", "PENDING_REVIEW");

        // Act
        WorkflowService.WorkflowExecutionResult result =
                workflowService.startAgreementUpdatedWorkflow(agreementData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getWorkflowId());
        assertEquals("agreement.updated", result.getEventType());
    }

    @Test
    void testStartAgreementEndorsedWorkflow() {
        // Arrange
        Map<String, Object> agreementData = createTestAgreementData(100000);
        agreementData.put("endorsementId", 5678L);

        // Act
        WorkflowService.WorkflowExecutionResult result =
                workflowService.startAgreementEndorsedWorkflow(agreementData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getWorkflowId());
        assertEquals("agreement.endorsed", result.getEventType());
    }

    @Test
    void testMultipleWorkflowExecutions() {
        // Arrange
        List<Map<String, Object>> agreements = Arrays.asList(
                createTestAgreementData(100000),
                createTestAgreementData(200000),
                createTestAgreementData(300000)
        );

        // Act
        List<WorkflowService.WorkflowExecutionResult> results = new ArrayList<>();
        for (Map<String, Object> agreement : agreements) {
            results.add(workflowService.startAgreementCreatedWorkflow(agreement));
        }

        // Assert
        assertEquals(3, results.size());
        for (WorkflowService.WorkflowExecutionResult result : results) {
            assertTrue(result.isSuccess());
            assertNotNull(result.getWorkflowId());
        }

        // Verify all workflow IDs are unique
        Set<String> workflowIds = new HashSet<>();
        for (WorkflowService.WorkflowExecutionResult result : results) {
            workflowIds.add(result.getWorkflowId());
        }
        assertEquals(3, workflowIds.size());
    }

    @Test
    void testWorkflowWithDifferentEventTypes() {
        // Arrange
        Map<String, Object> agreementData = createTestAgreementData(100000);

        // Act
        WorkflowService.WorkflowExecutionResult createdResult =
                workflowService.startWorkflow("agreement.created", agreementData);
        WorkflowService.WorkflowExecutionResult updatedResult =
                workflowService.startWorkflow("agreement.updated", agreementData);
        WorkflowService.WorkflowExecutionResult endorsedResult =
                workflowService.startWorkflow("agreement.endorsed", agreementData);

        // Assert
        assertTrue(createdResult.isSuccess());
        assertTrue(updatedResult.isSuccess());
        assertTrue(endorsedResult.isSuccess());

        assertEquals("agreement.created", createdResult.getEventType());
        assertEquals("agreement.updated", updatedResult.getEventType());
        assertEquals("agreement.endorsed", endorsedResult.getEventType());
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
        data.put("placementType", "DIRECT");
        return data;
    }

    private List<WorkflowHook> createMockWorkflowHooks() {
        List<WorkflowHook> hooks = new ArrayList<>();

        WorkflowHook hook = new WorkflowHook();
        hook.setHookId(1L);
        hook.setHookName("test_snapshot_hook");
        hook.setEventType("agreement.created");
        hook.setTriggerCondition("agreement != null && agreement.agreementType != null");
        hook.setActionType("snapshot");

        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("endpoint", "/mock-policy-api/snapshots");
        actionConfig.put("method", "POST");
        hook.setActionConfig(actionConfig);

        hook.setExecutionOrder(10);
        hook.setEnabled(true);

        hooks.add(hook);
        return hooks;
    }

    private List<Map<String, Object>> convertHooksToMaps(List<WorkflowHook> hooks) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (WorkflowHook hook : hooks) {
            Map<String, Object> map = new HashMap<>();
            map.put("hookId", hook.getHookId());
            map.put("hookName", hook.getHookName());
            map.put("eventType", hook.getEventType());
            map.put("triggerCondition", hook.getTriggerCondition());
            map.put("actionType", hook.getActionType());
            map.put("actionConfig", hook.getActionConfig());
            map.put("executionOrder", hook.getExecutionOrder());
            maps.add(map);
        }
        return maps;
    }

    private Map<String, Object> createSnapshotResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("snapshotId", UUID.randomUUID().toString());
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "CAPTURED");
        return response;
    }
}
