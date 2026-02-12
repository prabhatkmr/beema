package com.beema.kernel.service.workflow;

import com.beema.kernel.workflow.AgreementWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow Service
 *
 * Spring service that starts and manages Temporal workflows.
 * Provides high-level API for triggering workflows on business events.
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private static final String TASK_QUEUE = "BEEMA_AGREEMENT_TASK_QUEUE";
    private static final Duration WORKFLOW_TIMEOUT = Duration.ofMinutes(10);

    private final WorkflowClient workflowClient;

    public WorkflowService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * Start workflow for agreement creation event
     *
     * @param agreementData Agreement data
     * @return Workflow execution result
     */
    public WorkflowExecutionResult startAgreementCreatedWorkflow(Map<String, Object> agreementData) {
        return startWorkflow("agreement.created", agreementData);
    }

    /**
     * Start workflow for agreement update event
     *
     * @param agreementData Agreement data
     * @return Workflow execution result
     */
    public WorkflowExecutionResult startAgreementUpdatedWorkflow(Map<String, Object> agreementData) {
        return startWorkflow("agreement.updated", agreementData);
    }

    /**
     * Start workflow for agreement endorsement event
     *
     * @param agreementData Agreement data with endorsement
     * @return Workflow execution result
     */
    public WorkflowExecutionResult startAgreementEndorsedWorkflow(Map<String, Object> agreementData) {
        return startWorkflow("agreement.endorsed", agreementData);
    }

    /**
     * Start generic workflow for any event type
     *
     * @param eventType Event type
     * @param agreementData Agreement data
     * @return Workflow execution result
     */
    public WorkflowExecutionResult startWorkflow(String eventType, Map<String, Object> agreementData) {
        log.info("Starting workflow for event: {}", eventType);

        try {
            // Generate unique workflow ID
            String workflowId = generateWorkflowId(eventType, agreementData);

            // Configure workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(TASK_QUEUE)
                    .setWorkflowExecutionTimeout(WORKFLOW_TIMEOUT)
                    .build();

            // Create workflow stub
            AgreementWorkflow workflow = workflowClient.newWorkflowStub(
                    AgreementWorkflow.class,
                    options
            );

            // Start workflow asynchronously
            WorkflowClient.start(workflow::executeAgreementWorkflow, eventType, agreementData);

            log.info("Workflow started successfully: workflowId={}", workflowId);

            return WorkflowExecutionResult.success(workflowId, eventType);

        } catch (Exception e) {
            log.error("Error starting workflow for event: {}", eventType, e);
            return WorkflowExecutionResult.failure(eventType, e.getMessage());
        }
    }

    /**
     * Start workflow synchronously and wait for result
     *
     * @param eventType Event type
     * @param agreementData Agreement data
     * @return Workflow execution result with workflow output
     */
    public WorkflowExecutionResult startWorkflowSync(String eventType, Map<String, Object> agreementData) {
        log.info("Starting workflow synchronously for event: {}", eventType);

        try {
            // Generate unique workflow ID
            String workflowId = generateWorkflowId(eventType, agreementData);

            // Configure workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(TASK_QUEUE)
                    .setWorkflowExecutionTimeout(WORKFLOW_TIMEOUT)
                    .build();

            // Create workflow stub
            AgreementWorkflow workflow = workflowClient.newWorkflowStub(
                    AgreementWorkflow.class,
                    options
            );

            // Execute workflow synchronously
            Map<String, Object> result = workflow.executeAgreementWorkflow(eventType, agreementData);

            log.info("Workflow completed successfully: workflowId={}", workflowId);

            return WorkflowExecutionResult.success(workflowId, eventType, result);

        } catch (Exception e) {
            log.error("Error executing workflow for event: {}", eventType, e);
            return WorkflowExecutionResult.failure(eventType, e.getMessage());
        }
    }

    /**
     * Get workflow status by workflow ID
     *
     * @param workflowId Workflow ID
     * @return Workflow status information
     */
    public WorkflowStatusInfo getWorkflowStatus(String workflowId) {
        log.info("Getting workflow status: workflowId={}", workflowId);

        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);

            // Get workflow execution info
            var description = workflowStub.describe();

            WorkflowStatusInfo statusInfo = new WorkflowStatusInfo();
            statusInfo.setWorkflowId(workflowId);
            statusInfo.setStatus(description.getStatus().name());
            statusInfo.setStartTime(description.getStartTime());
            statusInfo.setExecutionTime(description.getExecutionTime());

            return statusInfo;

        } catch (Exception e) {
            log.error("Error getting workflow status: workflowId={}", workflowId, e);

            WorkflowStatusInfo statusInfo = new WorkflowStatusInfo();
            statusInfo.setWorkflowId(workflowId);
            statusInfo.setStatus("ERROR");
            statusInfo.setError(e.getMessage());

            return statusInfo;
        }
    }

    /**
     * Cancel workflow by workflow ID
     *
     * @param workflowId Workflow ID
     */
    public void cancelWorkflow(String workflowId) {
        log.info("Cancelling workflow: workflowId={}", workflowId);

        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
            workflowStub.cancel();

            log.info("Workflow cancelled successfully: workflowId={}", workflowId);

        } catch (Exception e) {
            log.error("Error cancelling workflow: workflowId={}", workflowId, e);
            throw new RuntimeException("Failed to cancel workflow: " + workflowId, e);
        }
    }

    /**
     * Generate unique workflow ID
     */
    private String generateWorkflowId(String eventType, Map<String, Object> agreementData) {
        String agreementId = agreementData.getOrDefault("agreementId", "unknown").toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("workflow-%s-%s-%s-%s", eventType, agreementId, timestamp, uuid);
    }

    /**
     * Workflow Execution Result
     */
    public static class WorkflowExecutionResult {
        private boolean success;
        private String workflowId;
        private String eventType;
        private Map<String, Object> result;
        private String errorMessage;

        public static WorkflowExecutionResult success(String workflowId, String eventType) {
            WorkflowExecutionResult result = new WorkflowExecutionResult();
            result.success = true;
            result.workflowId = workflowId;
            result.eventType = eventType;
            return result;
        }

        public static WorkflowExecutionResult success(String workflowId, String eventType,
                                                       Map<String, Object> workflowResult) {
            WorkflowExecutionResult result = new WorkflowExecutionResult();
            result.success = true;
            result.workflowId = workflowId;
            result.eventType = eventType;
            result.result = workflowResult;
            return result;
        }

        public static WorkflowExecutionResult failure(String eventType, String errorMessage) {
            WorkflowExecutionResult result = new WorkflowExecutionResult();
            result.success = false;
            result.eventType = eventType;
            result.errorMessage = errorMessage;
            return result;
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public void setResult(Map<String, Object> result) {
            this.result = result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Workflow Status Info
     */
    public static class WorkflowStatusInfo {
        private String workflowId;
        private String status;
        private Long startTime;
        private Long executionTime;
        private String error;

        // Getters and setters
        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getStartTime() {
            return startTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public Long getExecutionTime() {
            return executionTime;
        }

        public void setExecutionTime(Long executionTime) {
            this.executionTime = executionTime;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
