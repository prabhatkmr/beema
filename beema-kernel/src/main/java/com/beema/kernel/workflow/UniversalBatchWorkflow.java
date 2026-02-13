package com.beema.kernel.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * Temporal workflow interface for universal batch job execution.
 *
 * Temporal schedules trigger this workflow with tenant-specific parameters.
 * The implementation delegates to the appropriate Spring Batch job based on jobType.
 */
@WorkflowInterface
public interface UniversalBatchWorkflow {

    /**
     * Execute a batch job for a specific tenant.
     *
     * @param tenantId  the tenant identifier
     * @param jobType   the type of batch job (e.g., "PARQUET_EXPORT", "DATA_CLEANUP")
     * @param jobParams additional parameters for the job execution
     * @return execution result summary
     */
    @WorkflowMethod
    String execute(String tenantId, String jobType, Map<String, Object> jobParams);
}
