package com.beema.kernel.workflow.batch;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;

/**
 * Implementation of BatchOrchestratorWorkflow.
 *
 * Flow:
 * 1. Validate batch job config exists
 * 2. Start batch job via activity
 * 3. Monitor job status (with polling)
 * 4. Return results or handle failures
 */
public class BatchOrchestratorWorkflowImpl implements BatchOrchestratorWorkflow {

    private static final Logger log = Workflow.getLogger(BatchOrchestratorWorkflowImpl.class);

    private final BatchActivities activities = Workflow.newActivityStub(
            BatchActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofHours(1))
                    .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .build())
                    .build()
    );

    @Override
    public BatchExecutionResult executeBatchJob(String jobName, Map<String, Object> parameters) {
        log.info("Starting batch job orchestration: {}", jobName);

        try {
            // Step 1: Validate job configuration
            log.info("Validating batch job config: {}", jobName);
            activities.validateBatchJobConfig(jobName);

            // Step 2: Start batch job
            log.info("Starting batch job: {}", jobName);
            Long jobExecutionId = activities.startBatchJob(jobName, parameters);

            // Step 3: Monitor job status
            log.info("Monitoring batch job execution: {}", jobExecutionId);
            BatchJobStatus status = monitorJobExecution(jobExecutionId);

            // Step 4: Return results
            BatchExecutionResult result = activities.getBatchJobResult(jobExecutionId);
            log.info("Batch job completed: {} - Status: {}", jobName, status);

            return result;

        } catch (Exception e) {
            log.error("Batch job orchestration failed: {}", jobName, e);
            throw new RuntimeException("Batch job execution failed: " + e.getMessage(), e);
        }
    }

    private BatchJobStatus monitorJobExecution(Long jobExecutionId) {
        BatchJobStatus status;
        int pollCount = 0;
        int maxPolls = 120; // 2 hours max (60s interval)

        do {
            Workflow.sleep(Duration.ofSeconds(60)); // Poll every minute
            status = activities.checkBatchJobStatus(jobExecutionId);
            pollCount++;

            log.info("Batch job status check {}/{}: {}", pollCount, maxPolls, status);

            if (pollCount >= maxPolls) {
                throw new RuntimeException("Batch job execution timeout");
            }

        } while (status == BatchJobStatus.STARTED || status == BatchJobStatus.RUNNING);

        return status;
    }
}
