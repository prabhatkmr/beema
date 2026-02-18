package com.beema.kernel.api.v1.submission;

import com.beema.kernel.api.v1.submission.dto.BindResponse;
import com.beema.kernel.api.v1.submission.dto.SubmissionDetailResponse;
import com.beema.kernel.api.v1.submission.dto.SubmissionRequest;
import com.beema.kernel.api.v1.submission.dto.SubmissionResponse;
import com.beema.kernel.api.v1.submission.dto.WorkflowStatusResponse;
import com.beema.kernel.domain.submission.Submission;
import com.beema.kernel.domain.submission.SubmissionStatus;
import com.beema.kernel.service.submission.SubmissionService;
import com.beema.kernel.service.tenant.TenantContextService;
import com.beema.kernel.workflow.submission.SubmissionWorkflow;
import com.google.protobuf.util.Timestamps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final WorkflowServiceStubs serviceStubs;
    private final SubmissionService submissionService;
    private final TenantContextService tenantContextService;
    private final String namespace;

    public SubmissionController(
            WorkflowClient workflowClient,
            WorkflowServiceStubs serviceStubs,
            SubmissionService submissionService,
            TenantContextService tenantContextService,
            @org.springframework.beans.factory.annotation.Value("${temporal.namespace:default}") String namespace
    ) {
        this.workflowClient = workflowClient;
        this.serviceStubs = serviceStubs;
        this.submissionService = submissionService;
        this.tenantContextService = tenantContextService;
        this.namespace = namespace;
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

    // Event types that represent meaningful workflow milestones
    private static final Set<EventType> MILESTONE_EVENTS = Set.of(
            EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED,
            EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED,
            EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED,
            EventType.EVENT_TYPE_ACTIVITY_TASK_FAILED,
            EventType.EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED,
            EventType.EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED,
            EventType.EVENT_TYPE_WORKFLOW_EXECUTION_FAILED,
            EventType.EVENT_TYPE_WORKFLOW_EXECUTION_TIMED_OUT,
            EventType.EVENT_TYPE_WORKFLOW_EXECUTION_CANCELED
    );

    @GetMapping("/{submissionId}/workflow")
    @Operation(summary = "Get workflow status", description = "Get Temporal workflow execution status for a submission")
    public ResponseEntity<WorkflowStatusResponse> getWorkflowStatus(
            @PathVariable String submissionId
    ) {
        log.info("Getting workflow status for submission: {}", submissionId);

        try {
            WorkflowExecution execution = WorkflowExecution.newBuilder()
                    .setWorkflowId(submissionId)
                    .build();

            // Describe the workflow execution
            DescribeWorkflowExecutionResponse description = serviceStubs.blockingStub()
                    .describeWorkflowExecution(
                            DescribeWorkflowExecutionRequest.newBuilder()
                                    .setNamespace(namespace)
                                    .setExecution(execution)
                                    .build()
                    );

            var info = description.getWorkflowExecutionInfo();
            String status = info.getStatus().name().replace("WORKFLOW_EXECUTION_STATUS_", "");
            String runId = info.getExecution().getRunId();
            String taskQueue = info.getTaskQueue();
            String startTime = Instant.ofEpochSecond(
                    info.getStartTime().getSeconds(),
                    info.getStartTime().getNanos()
            ).toString();
            String closeTime = info.hasCloseTime()
                    ? Instant.ofEpochSecond(
                            info.getCloseTime().getSeconds(),
                            info.getCloseTime().getNanos()
                    ).toString()
                    : null;

            // Fetch workflow history for milestone events
            GetWorkflowExecutionHistoryResponse history = serviceStubs.blockingStub()
                    .getWorkflowExecutionHistory(
                            GetWorkflowExecutionHistoryRequest.newBuilder()
                                    .setNamespace(namespace)
                                    .setExecution(execution)
                                    .build()
                    );

            List<WorkflowStatusResponse.WorkflowEvent> events = new ArrayList<>();
            for (HistoryEvent event : history.getHistory().getEventsList()) {
                if (!MILESTONE_EVENTS.contains(event.getEventType())) {
                    continue;
                }

                String eventTime = Instant.ofEpochSecond(
                        event.getEventTime().getSeconds(),
                        event.getEventTime().getNanos()
                ).toString();

                String detail = formatEventDetail(event);

                events.add(new WorkflowStatusResponse.WorkflowEvent(
                        event.getEventId(),
                        event.getEventType().name().replace("EVENT_TYPE_", ""),
                        eventTime,
                        detail
                ));
            }

            return ResponseEntity.ok(new WorkflowStatusResponse(
                    submissionId, runId, status, startTime, closeTime, taskQueue, events
            ));

        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                return ResponseEntity.notFound().build();
            }
            log.error("Failed to fetch workflow status for {}: {}", submissionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private String formatEventDetail(HistoryEvent event) {
        return switch (event.getEventType()) {
            case EVENT_TYPE_ACTIVITY_TASK_SCHEDULED ->
                    event.getActivityTaskScheduledEventAttributes().getActivityType().getName();
            case EVENT_TYPE_ACTIVITY_TASK_COMPLETED ->
                    "Activity completed";
            case EVENT_TYPE_ACTIVITY_TASK_FAILED ->
                    "Activity failed: " + event.getActivityTaskFailedEventAttributes()
                            .getFailure().getMessage();
            case EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED ->
                    "Signal: " + event.getWorkflowExecutionSignaledEventAttributes().getSignalName();
            case EVENT_TYPE_WORKFLOW_EXECUTION_STARTED ->
                    "Workflow started";
            case EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED ->
                    "Workflow completed";
            case EVENT_TYPE_WORKFLOW_EXECUTION_FAILED ->
                    "Workflow failed";
            case EVENT_TYPE_WORKFLOW_EXECUTION_TIMED_OUT ->
                    "Workflow timed out";
            case EVENT_TYPE_WORKFLOW_EXECUTION_CANCELED ->
                    "Workflow canceled";
            default -> event.getEventType().name();
        };
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
