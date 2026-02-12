package com.beema.kernel.config;

import com.beema.kernel.workflow.AgreementWorkflowImpl;
import com.beema.kernel.workflow.WorkflowActivities;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Temporal Worker
 *
 * Registers workflows and activities, and starts worker to process tasks.
 * The worker polls the task queue and executes workflows and activities.
 */
@Component
public class TemporalWorker {

    private static final Logger log = LoggerFactory.getLogger(TemporalWorker.class);

    private static final String TASK_QUEUE = "BEEMA_AGREEMENT_TASK_QUEUE";

    private final WorkflowClient workflowClient;
    private final WorkflowActivities workflowActivities;
    private WorkerFactory workerFactory;

    @Value("${temporal.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${temporal.worker.max-concurrent-activities:10}")
    private int maxConcurrentActivities;

    @Value("${temporal.worker.max-concurrent-workflows:10}")
    private int maxConcurrentWorkflows;

    public TemporalWorker(WorkflowClient workflowClient, WorkflowActivities workflowActivities) {
        this.workflowClient = workflowClient;
        this.workflowActivities = workflowActivities;
    }

    /**
     * Initialize and start Temporal worker
     */
    @PostConstruct
    public void start() {
        if (!workerEnabled) {
            log.info("Temporal worker is disabled");
            return;
        }

        log.info("Starting Temporal worker: taskQueue={}", TASK_QUEUE);

        try {
            // Create worker factory
            workerFactory = WorkerFactory.newInstance(workflowClient);

            // Create worker for task queue
            Worker worker = workerFactory.newWorker(TASK_QUEUE);

            // Register workflow implementations
            worker.registerWorkflowImplementationTypes(AgreementWorkflowImpl.class);
            log.info("Registered workflow implementation: AgreementWorkflowImpl");

            // Register activity implementations
            worker.registerActivitiesImplementations(workflowActivities);
            log.info("Registered activity implementation: WorkflowActivitiesImpl");

            // Configure worker options
            log.info("Worker configuration: maxConcurrentActivities={}, maxConcurrentWorkflows={}",
                    maxConcurrentActivities, maxConcurrentWorkflows);

            // Start worker factory (non-blocking)
            workerFactory.start();

            log.info("Temporal worker started successfully on task queue: {}", TASK_QUEUE);

        } catch (Exception e) {
            log.error("Failed to start Temporal worker", e);
            throw new RuntimeException("Failed to start Temporal worker", e);
        }
    }

    /**
     * Shutdown Temporal worker gracefully
     */
    @PreDestroy
    public void shutdown() {
        if (workerFactory != null) {
            log.info("Shutting down Temporal worker");

            try {
                workerFactory.shutdown();
                log.info("Temporal worker shut down successfully");
            } catch (Exception e) {
                log.error("Error shutting down Temporal worker", e);
            }
        }
    }
}
