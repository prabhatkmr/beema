package com.beema.kernel.config;

import com.beema.kernel.service.submission.SubmissionService;
import com.beema.kernel.workflow.submission.RatingActivitiesImpl;
import com.beema.kernel.workflow.submission.SubmissionPersistenceActivitiesImpl;
import com.beema.kernel.workflow.submission.SubmissionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Temporal worker configuration for the submission workflow.
 *
 * Registers the SubmissionWorkflow, RatingActivities, and
 * SubmissionPersistenceActivities with a worker listening on
 * the SUBMISSION_QUEUE task queue.
 */
@Configuration
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true")
public class SubmissionWorkerConfig {

    private static final Logger log = LoggerFactory.getLogger(SubmissionWorkerConfig.class);

    public static final String SUBMISSION_QUEUE = "SUBMISSION_QUEUE";

    private final WorkflowClient workflowClient;
    private final SubmissionService submissionService;

    public SubmissionWorkerConfig(WorkflowClient workflowClient, SubmissionService submissionService) {
        this.workflowClient = workflowClient;
        this.submissionService = submissionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        log.info("Starting Temporal worker for task queue '{}'", SUBMISSION_QUEUE);

        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(SUBMISSION_QUEUE);

        worker.registerWorkflowImplementationTypes(SubmissionWorkflowImpl.class);
        worker.registerActivitiesImplementations(
                new RatingActivitiesImpl(),
                new SubmissionPersistenceActivitiesImpl(submissionService)
        );

        factory.start();

        log.info("Temporal submission worker started on queue '{}'", SUBMISSION_QUEUE);
    }
}
