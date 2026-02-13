package com.beema.kernel.api.v1.submission;

import com.beema.kernel.api.v1.submission.dto.BindResponse;
import com.beema.kernel.api.v1.submission.dto.SubmissionDetailResponse;
import com.beema.kernel.api.v1.submission.dto.SubmissionRequest;
import com.beema.kernel.api.v1.submission.dto.SubmissionResponse;
import com.beema.kernel.domain.submission.Submission;
import com.beema.kernel.domain.submission.SubmissionStatus;
import com.beema.kernel.service.submission.SubmissionService;
import com.beema.kernel.service.tenant.TenantContextService;
import com.beema.kernel.workflow.submission.SubmissionWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for quote submissions.
 *
 * Endpoints:
 * - POST   /api/v1/submissions              - Submit a new quote
 * - GET    /api/v1/submissions              - List submissions
 * - GET    /api/v1/submissions/{id}         - Get submission detail
 * - POST   /api/v1/submissions/{id}/bind    - Bind a quoted submission
 */
@RestController
@RequestMapping("/api/v1/submissions")
@Tag(name = "Submissions", description = "Quote submission and workflow triggering")
@ConditionalOnBean(WorkflowClient.class)
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);
    private static final String SUBMISSION_TASK_QUEUE = "SUBMISSION_QUEUE";

    private final WorkflowClient workflowClient;
    private final SubmissionService submissionService;
    private final TenantContextService tenantContextService;

    public SubmissionController(
            WorkflowClient workflowClient,
            SubmissionService submissionService,
            TenantContextService tenantContextService
    ) {
        this.workflowClient = workflowClient;
        this.submissionService = submissionService;
        this.tenantContextService = tenantContextService;
    }

    @PostMapping
    @Operation(summary = "Submit quote", description = "Submit a new quote for workflow processing")
    public ResponseEntity<SubmissionResponse> submitQuote(
            @Valid @RequestBody SubmissionRequest request
    ) {
        String submissionId = UUID.randomUUID().toString();
        log.info("Submitting quote: submissionId={}, product={}", submissionId, request.product());

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(submissionId)
                .setTaskQueue(SUBMISSION_TASK_QUEUE)
                .build();

        SubmissionWorkflow workflow = workflowClient.newWorkflowStub(SubmissionWorkflow.class, options);
        WorkflowClient.start(workflow::execute, submissionId, request.product(), request.data());

        log.info("Workflow started for submission: {}", submissionId);

        SubmissionResponse response = new SubmissionResponse(submissionId, "STARTED");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    @Operation(summary = "List submissions", description = "List all submissions for the current tenant")
    public ResponseEntity<Page<SubmissionDetailResponse>> listSubmissions(
            @RequestParam(required = false) SubmissionStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        String tenantId = tenantContextService.getCurrentTenantId();
        log.info("Listing submissions: tenant={}, status={}", tenantId, status);

        Page<Submission> submissions = submissionService.listSubmissions(tenantId, status, pageable);
        Page<SubmissionDetailResponse> response = submissions.map(this::toDetailResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Get submission", description = "Get submission details by ID")
    public ResponseEntity<SubmissionDetailResponse> getSubmission(
            @PathVariable UUID submissionId
    ) {
        log.info("Getting submission: {}", submissionId);

        Submission submission = submissionService.getSubmission(submissionId);
        if (submission == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDetailResponse(submission));
    }

    @PostMapping("/{submissionId}/bind")
    @Operation(summary = "Bind submission", description = "Bind a quoted submission to create a policy")
    public ResponseEntity<BindResponse> bindSubmission(
            @PathVariable UUID submissionId
    ) {
        log.info("Binding submission: {}", submissionId);

        // Validate submission exists and is in QUOTED status
        Submission submission = submissionService.getSubmission(submissionId);
        if (submission == null) {
            return ResponseEntity.notFound().build();
        }

        if (submission.getStatus() != SubmissionStatus.QUOTED) {
            BindResponse error = new BindResponse(
                    submissionId.toString(),
                    submission.getStatus().name(),
                    "Submission must be in QUOTED status to bind. Current status: " + submission.getStatus()
            );
            return ResponseEntity.badRequest().body(error);
        }

        // Send bind signal to the Temporal workflow
        SubmissionWorkflow workflow = workflowClient.newWorkflowStub(
                SubmissionWorkflow.class, submissionId.toString());
        workflow.bind();

        log.info("Bind signal sent for submission: {}", submissionId);

        BindResponse response = new BindResponse(
                submissionId.toString(),
                "BINDING",
                "Bind signal sent successfully"
        );
        return ResponseEntity.ok(response);
    }

    private SubmissionDetailResponse toDetailResponse(Submission submission) {
        return new SubmissionDetailResponse(
                submission.getSubmissionId(),
                submission.getProduct(),
                submission.getStatus().name(),
                submission.getTenantId(),
                submission.getFormData(),
                submission.getRatingResult(),
                submission.getCreatedAt(),
                submission.getUpdatedAt(),
                submission.getVersion()
        );
    }
}
