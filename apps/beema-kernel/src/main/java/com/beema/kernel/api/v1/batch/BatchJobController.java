package com.beema.kernel.api.v1.batch;

import com.beema.kernel.workflow.batch.BatchOrchestratorWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API for triggering batch jobs via Temporal.
 */
@RestController
@RequestMapping("/api/v1/batch")
public class BatchJobController {

    private final WorkflowClient workflowClient;

    public BatchJobController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * Triggers a batch job asynchronously.
     *
     * @param jobName Name of the batch job config
     * @param parameters Optional job parameters
     * @return Workflow ID for tracking
     */
    @PostMapping("/jobs/{jobName}/trigger")
    public ResponseEntity<Map<String, String>> triggerBatchJob(
            @PathVariable String jobName,
            @RequestBody(required = false) Map<String, Object> parameters) {

        String workflowId = "batch-job-" + jobName + "-" + UUID.randomUUID();

        BatchOrchestratorWorkflow workflow = workflowClient.newWorkflowStub(
                BatchOrchestratorWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue("batch-queue")
                        .build()
        );

        // Start workflow asynchronously
        WorkflowClient.start(workflow::executeBatchJob, jobName, parameters != null ? parameters : Map.of());

        return ResponseEntity.accepted()
                .body(Map.of(
                        "workflowId", workflowId,
                        "jobName", jobName,
                        "status", "STARTED",
                        "message", "Batch job started successfully"
                ));
    }
}
