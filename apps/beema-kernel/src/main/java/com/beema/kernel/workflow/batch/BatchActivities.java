package com.beema.kernel.workflow.batch;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Temporal activities for batch job operations.
 */
@ActivityInterface
public interface BatchActivities {

    /**
     * Validates that a batch job configuration exists and is enabled.
     */
    @ActivityMethod
    void validateBatchJobConfig(String jobName);

    /**
     * Starts a Spring Batch job.
     *
     * @return Job execution ID
     */
    @ActivityMethod
    Long startBatchJob(String jobName, Map<String, Object> parameters);

    /**
     * Checks the status of a running batch job.
     */
    @ActivityMethod
    BatchJobStatus checkBatchJobStatus(Long jobExecutionId);

    /**
     * Retrieves the final results of a batch job.
     */
    @ActivityMethod
    BatchExecutionResult getBatchJobResult(Long jobExecutionId);
}
