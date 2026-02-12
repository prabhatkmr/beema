package com.beema.kernel.api.v1.claim;

import com.beema.kernel.ai.service.ClaimAnalyzerService;
import com.beema.kernel.domain.claim.Claim;
import com.beema.kernel.workflow.claim.ClaimWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * REST controller for AI-powered claim analysis
 */
@RestController
@RequestMapping("/api/v1/claims/analysis")
public class ClaimAnalysisController {

    private static final String CLAIM_TASK_QUEUE = "CLAIM_TASK_QUEUE";

    private final ClaimAnalyzerService claimAnalyzerService;
    private final WorkflowClient workflowClient;

    public ClaimAnalysisController(ClaimAnalyzerService claimAnalyzerService,
                                   WorkflowClient workflowClient) {
        this.claimAnalyzerService = claimAnalyzerService;
        this.workflowClient = workflowClient;
    }

    /**
     * Analyze a claim using AI (synchronous)
     */
    @PostMapping("/analyze")
    public ResponseEntity<ClaimAnalyzerService.ClaimAnalysisResult> analyzeClaim(
            @RequestBody Claim claim) {

        ClaimAnalyzerService.ClaimAnalysisResult result = claimAnalyzerService.analyzeClaim(claim);
        return ResponseEntity.ok(result);
    }

    /**
     * Process a claim with Temporal workflow (asynchronous)
     */
    @PostMapping("/process-with-workflow")
    public ResponseEntity<WorkflowStartResponse> processClaimWithWorkflow(
            @RequestBody Claim claim) {

        String workflowId = "claim-ai-" + claim.getClaimNumber();

        ClaimWorkflow workflow = workflowClient.newWorkflowStub(
            ClaimWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(CLAIM_TASK_QUEUE)
                .setWorkflowExecutionTimeout(Duration.ofHours(1))
                .build()
        );

        // Start workflow asynchronously
        WorkflowClient.start(workflow::processClaimWithAI, claim);

        return ResponseEntity.ok(new WorkflowStartResponse(workflowId, "STARTED"));
    }

    /**
     * Get workflow execution result
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<ClaimWorkflow.ClaimWorkflowResult> getWorkflowResult(
            @PathVariable String workflowId) {

        ClaimWorkflow workflow = workflowClient.newWorkflowStub(
            ClaimWorkflow.class,
            workflowId
        );

        // This will block until workflow completes
        ClaimWorkflow.ClaimWorkflowResult result = workflow.processClaimWithAI(null);

        return ResponseEntity.ok(result);
    }

    /**
     * Response for workflow start requests
     */
    public record WorkflowStartResponse(String workflowId, String status) {}
}
