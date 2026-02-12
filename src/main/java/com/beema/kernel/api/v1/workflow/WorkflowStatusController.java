package com.beema.kernel.api.v1.workflow;

import com.beema.kernel.workflow.renewal.RenewalWorkflow;
import com.beema.kernel.workflow.submission.SubmissionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for querying Temporal workflow status.
 *
 * Replaces traditional database job status queries with direct workflow queries.
 * This is the "Job Replacement Layer" pattern - workflow state is the source of truth.
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowStatusController {

    private final WorkflowClient workflowClient;

    public WorkflowStatusController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * Get submission workflow status.
     *
     * @param submissionId Submission ID
     * @return Submission status and details
     */
    @GetMapping("/submission/{submissionId}/status")
    public ResponseEntity<SubmissionStatusResponse> getSubmissionStatus(
            @PathVariable String submissionId) {

        String workflowId = "submission-" + submissionId;

        try {
            SubmissionWorkflow workflow = workflowClient.newWorkflowStub(
                    SubmissionWorkflow.class,
                    workflowId
            );

            // Query workflow state using Temporal queries
            String status = workflow.getStatus();
            Double quotedPremium = workflow.getQuotedPremium();
            String policyNumber = workflow.getPolicyNumber();

            SubmissionStatusResponse response = new SubmissionStatusResponse(
                    submissionId,
                    status,
                    quotedPremium,
                    policyNumber,
                    workflowId
            );

            return ResponseEntity.ok(response);

        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get renewal workflow status.
     *
     * @param policyNumber Policy number
     * @return Renewal status and details
     */
    @GetMapping("/renewal/{policyNumber}/status")
    public ResponseEntity<RenewalStatusResponse> getRenewalStatus(
            @PathVariable String policyNumber) {

        String workflowId = "renewal-" + policyNumber;

        try {
            RenewalWorkflow workflow = workflowClient.newWorkflowStub(
                    RenewalWorkflow.class,
                    workflowId
            );

            // Query workflow state
            String status = workflow.getStatus();
            Double renewalPremium = workflow.getRenewalPremium();
            Double increasePercent = workflow.getPremiumIncreasePercent();

            RenewalStatusResponse response = new RenewalStatusResponse(
                    policyNumber,
                    status,
                    renewalPremium,
                    increasePercent,
                    workflowId
            );

            return ResponseEntity.ok(response);

        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Send quote signal to submission workflow.
     *
     * @param submissionId Submission ID
     * @return Success response
     */
    @PostMapping("/submission/{submissionId}/quote")
    public ResponseEntity<Void> requestQuote(@PathVariable String submissionId) {
        String workflowId = "submission-" + submissionId;

        try {
            SubmissionWorkflow workflow = workflowClient.newWorkflowStub(
                    SubmissionWorkflow.class,
                    workflowId
            );

            workflow.quote();
            return ResponseEntity.ok().build();

        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Send bind signal to submission workflow.
     *
     * @param submissionId Submission ID
     * @return Success response
     */
    @PostMapping("/submission/{submissionId}/bind")
    public ResponseEntity<Void> bindSubmission(@PathVariable String submissionId) {
        String workflowId = "submission-" + submissionId;

        try {
            SubmissionWorkflow workflow = workflowClient.newWorkflowStub(
                    SubmissionWorkflow.class,
                    workflowId
            );

            workflow.bind();
            return ResponseEntity.ok().build();

        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Send underwriter review signal to renewal workflow.
     *
     * @param policyNumber Policy number
     * @param request Review decision
     * @return Success response
     */
    @PostMapping("/renewal/{policyNumber}/review")
    public ResponseEntity<Void> underwriterReview(
            @PathVariable String policyNumber,
            @RequestBody UnderwriterReviewRequest request) {

        String workflowId = "renewal-" + policyNumber;

        try {
            RenewalWorkflow workflow = workflowClient.newWorkflowStub(
                    RenewalWorkflow.class,
                    workflowId
            );

            workflow.underwriterReview(
                    request.approved(),
                    request.adjustedPremium(),
                    request.notes()
            );

            return ResponseEntity.ok().build();

        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
