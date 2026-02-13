package com.beema.kernel.workflow.submission;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;

/**
 * Implementation of the SubmissionWorkflow.
 *
 * Orchestrates the quote submission lifecycle:
 * 1. DRAFT - Submission initialized, persisted to database
 * 2. Rating - Calls RatingActivities to compute premium
 * 3. QUOTED - Rating result stored, persisted, awaiting bind signal
 * 4. BOUND - Bind signal received, persisted, submission finalized
 *
 * The workflow is deterministic and uses Workflow.await() to pause
 * execution until an external bind signal is received.
 */
public class SubmissionWorkflowImpl implements SubmissionWorkflow {

    private static final Logger log = Workflow.getLogger(SubmissionWorkflowImpl.class);

    private final RatingActivities ratingActivities = Workflow.newActivityStub(
            RatingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private final SubmissionPersistenceActivities persistenceActivities = Workflow.newActivityStub(
            SubmissionPersistenceActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private String status;
    private Map<String, Object> ratingResult;
    private boolean bindSignalReceived;

    @Override
    public String execute(String submissionId, String product, Map<String, Object> data) {
        // Step 1: Initialize as DRAFT and persist
        status = "DRAFT";
        log.info("Submission {} started for product '{}', status={}", submissionId, product, status);
        persistenceActivities.saveDraft(submissionId, product, data, "default");

        // Step 2: Call rating activity
        log.info("Submission {} sending to rating engine", submissionId);
        ratingResult = ratingActivities.rate(data);
        log.info("Submission {} rated: premium={}, tax={}, total={}",
                submissionId,
                ratingResult.get("premium"),
                ratingResult.get("tax"),
                ratingResult.get("total"));

        // Step 3: Update status to QUOTED and persist with rating result
        status = "QUOTED";
        log.info("Submission {} status={}, persisting rating result", submissionId, status);
        persistenceActivities.saveQuoted(submissionId, ratingResult);
        log.info("Submission {} waiting for bind signal", submissionId);

        // Step 4: Wait for bind signal
        Workflow.await(() -> bindSignalReceived);

        // Step 5: Bind the submission and persist
        status = "BOUND";
        log.info("Submission {} bound, persisting final state", submissionId);
        persistenceActivities.saveBound(submissionId);
        log.info("Submission {} bound successfully, status={}", submissionId, status);

        return submissionId;
    }

    @Override
    public void bind() {
        log.info("Bind signal received");
        this.bindSignalReceived = true;
    }
}
