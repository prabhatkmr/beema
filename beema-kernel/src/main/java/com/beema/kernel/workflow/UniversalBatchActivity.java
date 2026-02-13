package com.beema.kernel.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Activity interface for batch job execution within a Temporal workflow.
 *
 * Activities are the actual units of work that run batch jobs.
 * They are executed by the Temporal worker and can invoke Spring Batch jobs.
 */
@ActivityInterface
public interface UniversalBatchActivity {

    /**
     * Run a batch job for a specific tenant.
     *
     * @param tenantId  the tenant identifier
     * @param jobType   the type of batch job
     * @param jobParams additional parameters for the job
     * @return execution result summary
     */
    @ActivityMethod
    String runBatchJob(String tenantId, String jobType, Map<String, Object> jobParams);
}
