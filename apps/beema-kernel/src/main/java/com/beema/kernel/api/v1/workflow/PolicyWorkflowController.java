package com.beema.kernel.api.v1.workflow;

import com.beema.kernel.workflow.policy.PolicyWorkflow;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowRequest;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowResult;
import com.beema.kernel.workflow.policy.model.StateUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Policy Workflow Controller
 *
 * REST API for managing policy workflows including starting workflows,
 * sending signals, querying state, and retrieving results.
 */
@RestController
@RequestMapping("/api/v1/workflows/policy")
@Tag(name = "Policy Workflows", description = "Policy lifecycle workflow management")
public class PolicyWorkflowController {

    private static final Logger log = LoggerFactory.getLogger(PolicyWorkflowController.class);
    private static final String POLICY_TASK_QUEUE = "POLICY_TASK_QUEUE";
    private static final Duration DEFAULT_WORKFLOW_TIMEOUT = Duration.ofHours(1);
    private static final Duration RESULT_QUERY_TIMEOUT = Duration.ofSeconds(30);

    private final WorkflowClient workflowClient;

    public PolicyWorkflowController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * Start a new policy workflow
     */
    @PostMapping("/start")
    @Operation(summary = "Start policy workflow", description = "Initiates a new policy lifecycle workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Workflow started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Workflow already exists")
    })
    public ResponseEntity<WorkflowStartResponse> startPolicyWorkflow(
            @Valid @RequestBody PolicyWorkflowRequest request) {

        String workflowId = generateWorkflowId(request.getPolicyId());
        log.info("Starting policy workflow: workflowId={}, policyId={}", workflowId, request.getPolicyId());

        try {
            // Configure workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(POLICY_TASK_QUEUE)
                    .setWorkflowExecutionTimeout(DEFAULT_WORKFLOW_TIMEOUT)
                    .build();

            // Create workflow stub
            PolicyWorkflow workflow = workflowClient.newWorkflowStub(PolicyWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowClient.start(workflow::processPolicyLifecycle, request);

            log.info("Policy workflow started successfully: workflowId={}", workflowId);

            WorkflowStartResponse response = new WorkflowStartResponse(
                    workflowId,
                    request.getPolicyId(),
                    "Workflow started successfully",
                    true
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn("Workflow already exists: workflowId={}", workflowId);
            WorkflowStartResponse response = new WorkflowStartResponse(
                    workflowId,
                    request.getPolicyId(),
                    "Workflow already exists",
                    false
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Failed to start workflow: workflowId={}", workflowId, e);
            WorkflowStartResponse response = new WorkflowStartResponse(
                    workflowId,
                    request.getPolicyId(),
                    "Failed to start workflow: " + e.getMessage(),
                    false
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send a signal to update policy state
     */
    @PostMapping("/{workflowId}/signal")
    @Operation(summary = "Update policy state", description = "Sends a signal to update the state of a running workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signal sent successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found"),
            @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    public ResponseEntity<Map<String, Object>> updatePolicyState(
            @Parameter(description = "Workflow ID") @PathVariable String workflowId,
            @Valid @RequestBody StateUpdateRequest request) {

        log.info("Sending state update signal: workflowId={}, newState={}", workflowId, request.getNewState());

        try {
            // Get workflow stub
            PolicyWorkflow workflow = workflowClient.newWorkflowStub(PolicyWorkflow.class, workflowId);

            // Send signal
            workflow.updateState(request.getNewState());

            log.info("State update signal sent successfully: workflowId={}", workflowId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "State update signal sent successfully");
            response.put("workflowId", workflowId);
            response.put("newState", request.getNewState());

            return ResponseEntity.ok(response);

        } catch (WorkflowNotFoundException e) {
            log.error("Workflow not found: workflowId={}", workflowId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Workflow not found");
            response.put("workflowId", workflowId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Failed to send signal: workflowId={}", workflowId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send signal: " + e.getMessage());
            response.put("workflowId", workflowId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Query the current state of a workflow
     */
    @GetMapping("/{workflowId}/state")
    @Operation(summary = "Get policy state", description = "Queries the current state of a running workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "State retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    public ResponseEntity<Map<String, Object>> getPolicyState(
            @Parameter(description = "Workflow ID") @PathVariable String workflowId) {

        log.info("Querying workflow state: workflowId={}", workflowId);

        try {
            // Get workflow stub
            PolicyWorkflow workflow = workflowClient.newWorkflowStub(PolicyWorkflow.class, workflowId);

            // Query current state
            String currentState = workflow.getCurrentState();

            log.info("Workflow state retrieved: workflowId={}, state={}", workflowId, currentState);

            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("currentState", currentState);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (WorkflowNotFoundException e) {
            log.error("Workflow not found: workflowId={}", workflowId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Workflow not found");
            response.put("workflowId", workflowId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Failed to query workflow state: workflowId={}", workflowId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to query state: " + e.getMessage());
            response.put("workflowId", workflowId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get the workflow result (blocking call with timeout)
     */
    @GetMapping("/{workflowId}/result")
    @Operation(summary = "Get workflow result", description = "Retrieves the result of a completed workflow (blocks until completion or timeout)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found"),
            @ApiResponse(responseCode = "408", description = "Request timeout")
    })
    public ResponseEntity<PolicyWorkflowResult> getWorkflowResult(
            @Parameter(description = "Workflow ID") @PathVariable String workflowId,
            @Parameter(description = "Wait for result (in seconds, default 30)")
            @RequestParam(defaultValue = "30") long timeoutSeconds) {

        log.info("Getting workflow result: workflowId={}, timeout={}s", workflowId, timeoutSeconds);

        try {
            // Get untyped workflow stub for result retrieval
            WorkflowStub untypedWorkflow = workflowClient.newUntypedWorkflowStub(workflowId);

            // Get result with timeout
            PolicyWorkflowResult result = untypedWorkflow.getResult(
                    Duration.ofSeconds(timeoutSeconds),
                    PolicyWorkflowResult.class
            );

            log.info("Workflow result retrieved: workflowId={}, success={}", workflowId, result.isSuccess());
            return ResponseEntity.ok(result);

        } catch (WorkflowNotFoundException e) {
            log.error("Workflow not found: workflowId={}", workflowId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("Workflow result timeout: workflowId={}", workflowId);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();

        } catch (Exception e) {
            log.error("Failed to get workflow result: workflowId={}", workflowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate a unique workflow ID based on policy ID
     */
    private String generateWorkflowId(String policyId) {
        return String.format("policy-workflow-%s-%s", policyId, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Response DTO for workflow start operation
     */
    public static class WorkflowStartResponse {
        private String workflowId;
        private String policyId;
        private String message;
        private boolean success;

        public WorkflowStartResponse(String workflowId, String policyId, String message, boolean success) {
            this.workflowId = workflowId;
            this.policyId = policyId;
            this.message = message;
            this.success = success;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
}
