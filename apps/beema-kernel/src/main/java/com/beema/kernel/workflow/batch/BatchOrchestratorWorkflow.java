package com.beema.kernel.workflow.batch;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow for orchestrating Spring Batch jobs.
 *
 * Provides durable, retryable batch job execution with monitoring.
 */
@WorkflowInterface
public interface BatchOrchestratorWorkflow {

    /**
     * Executes a batch job by name.
     *
     * @param jobName The name of the batch job configuration
     * @param parameters Optional job parameters
     * @return Batch job execution summary
     */
    @WorkflowMethod
    BatchExecutionResult executeBatchJob(String jobName, java.util.Map<String, Object> parameters);
}
