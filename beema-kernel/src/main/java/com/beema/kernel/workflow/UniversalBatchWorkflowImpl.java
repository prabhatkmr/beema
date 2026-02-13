package com.beema.kernel.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;

/**
 * Implementation of the UniversalBatchWorkflow.
 *
 * Delegates execution to UniversalBatchActivity which runs the actual
 * Spring Batch job. The workflow provides durability guarantees -
 * if the worker crashes, Temporal will retry the workflow from its last state.
 */
public class UniversalBatchWorkflowImpl implements UniversalBatchWorkflow {

    private static final Logger log = Workflow.getLogger(UniversalBatchWorkflowImpl.class);

    private final UniversalBatchActivity activity = Workflow.newActivityStub(
            UniversalBatchActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofHours(2))
                    .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(10))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    @Override
    public String execute(String tenantId, String jobType, Map<String, Object> jobParams) {
        log.info("Starting batch workflow for tenant={}, jobType={}", tenantId, jobType);

        String result = activity.runBatchJob(tenantId, jobType, jobParams);

        log.info("Completed batch workflow for tenant={}, jobType={}, result={}",
                tenantId, jobType, result);
        return result;
    }
}
