package com.beema.kernel.api;

import com.beema.kernel.service.workflow.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Workflow Controller
 *
 * REST API for triggering and managing Temporal workflows
 */
@RestController
@RequestMapping("/api/workflows")
@Tag(name = "Workflows", description = "Temporal workflow management API")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Start workflow asynchronously
     *
     * POST /api/workflows/start
     */
    @PostMapping("/start")
    @Operation(summary = "Start workflow asynchronously",
               description = "Starts a Temporal workflow based on event type and returns immediately")
    public ResponseEntity<WorkflowService.WorkflowExecutionResult> startWorkflow(
            @RequestBody WorkflowStartRequest request) {

        log.info("Starting workflow: eventType={}, agreementId={}",
                request.getEventType(), request.getAgreementData().get("agreementId"));

        WorkflowService.WorkflowExecutionResult result =
                workflowService.startWorkflow(request.getEventType(), request.getAgreementData());

        if (result.isSuccess()) {
            return ResponseEntity.accepted().body(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Start workflow synchronously and wait for result
     *
     * POST /api/workflows/start-sync
     */
    @PostMapping("/start-sync")
    @Operation(summary = "Start workflow synchronously",
               description = "Starts a Temporal workflow and waits for completion (use for testing only)")
    public ResponseEntity<WorkflowService.WorkflowExecutionResult> startWorkflowSync(
            @RequestBody WorkflowStartRequest request) {

        log.info("Starting workflow synchronously: eventType={}, agreementId={}",
                request.getEventType(), request.getAgreementData().get("agreementId"));

        WorkflowService.WorkflowExecutionResult result =
                workflowService.startWorkflowSync(request.getEventType(), request.getAgreementData());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Start workflow for agreement created event
     *
     * POST /api/workflows/agreement/created
     */
    @PostMapping("/agreement/created")
    @Operation(summary = "Trigger workflow for agreement created event",
               description = "Starts workflow when a new agreement is created")
    public ResponseEntity<WorkflowService.WorkflowExecutionResult> onAgreementCreated(
            @RequestBody Map<String, Object> agreementData) {

        log.info("Agreement created event: agreementId={}", agreementData.get("agreementId"));

        WorkflowService.WorkflowExecutionResult result =
                workflowService.startAgreementCreatedWorkflow(agreementData);

        return result.isSuccess()
                ? ResponseEntity.accepted().body(result)
                : ResponseEntity.internalServerError().body(result);
    }

    /**
     * Start workflow for agreement updated event
     *
     * POST /api/workflows/agreement/updated
     */
    @PostMapping("/agreement/updated")
    @Operation(summary = "Trigger workflow for agreement updated event",
               description = "Starts workflow when an agreement is updated")
    public ResponseEntity<WorkflowService.WorkflowExecutionResult> onAgreementUpdated(
            @RequestBody Map<String, Object> agreementData) {

        log.info("Agreement updated event: agreementId={}", agreementData.get("agreementId"));

        WorkflowService.WorkflowExecutionResult result =
                workflowService.startAgreementUpdatedWorkflow(agreementData);

        return result.isSuccess()
                ? ResponseEntity.accepted().body(result)
                : ResponseEntity.internalServerError().body(result);
    }

    /**
     * Start workflow for agreement endorsed event
     *
     * POST /api/workflows/agreement/endorsed
     */
    @PostMapping("/agreement/endorsed")
    @Operation(summary = "Trigger workflow for agreement endorsed event",
               description = "Starts workflow when an agreement endorsement is applied")
    public ResponseEntity<WorkflowService.WorkflowExecutionResult> onAgreementEndorsed(
            @RequestBody Map<String, Object> agreementData) {

        log.info("Agreement endorsed event: agreementId={}", agreementData.get("agreementId"));

        WorkflowService.WorkflowExecutionResult result =
                workflowService.startAgreementEndorsedWorkflow(agreementData);

        return result.isSuccess()
                ? ResponseEntity.accepted().body(result)
                : ResponseEntity.internalServerError().body(result);
    }

    /**
     * Get workflow status
     *
     * GET /api/workflows/{workflowId}/status
     */
    @GetMapping("/{workflowId}/status")
    @Operation(summary = "Get workflow status",
               description = "Retrieves the current status of a workflow execution")
    public ResponseEntity<WorkflowService.WorkflowStatusInfo> getWorkflowStatus(
            @PathVariable String workflowId) {

        log.info("Getting workflow status: workflowId={}", workflowId);

        WorkflowService.WorkflowStatusInfo statusInfo = workflowService.getWorkflowStatus(workflowId);

        return ResponseEntity.ok(statusInfo);
    }

    /**
     * Cancel workflow
     *
     * DELETE /api/workflows/{workflowId}
     */
    @DeleteMapping("/{workflowId}")
    @Operation(summary = "Cancel workflow",
               description = "Cancels a running workflow execution")
    public ResponseEntity<Map<String, String>> cancelWorkflow(@PathVariable String workflowId) {

        log.info("Cancelling workflow: workflowId={}", workflowId);

        try {
            workflowService.cancelWorkflow(workflowId);

            return ResponseEntity.ok(Map.of(
                    "status", "CANCELLED",
                    "workflowId", workflowId,
                    "message", "Workflow cancelled successfully"
            ));

        } catch (Exception e) {
            log.error("Error cancelling workflow: workflowId={}", workflowId, e);

            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "workflowId", workflowId,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Workflow Start Request DTO
     */
    public static class WorkflowStartRequest {
        private String eventType;
        private Map<String, Object> agreementData;

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public Map<String, Object> getAgreementData() {
            return agreementData;
        }

        public void setAgreementData(Map<String, Object> agreementData) {
            this.agreementData = agreementData;
        }
    }
}
