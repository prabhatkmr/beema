package com.beema.kernel.config;

import com.beema.kernel.workflow.policy.PolicySnapshotActivityImpl;
import com.beema.kernel.workflow.policy.PolicyWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal Configuration
 *
 * Configures Temporal client, service stubs, and workers for connecting to Temporal server.
 * Includes configuration for policy workflow workers.
 */
@Configuration
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);
    private static final String POLICY_TASK_QUEUE = "POLICY_TASK_QUEUE";

    @Value("${temporal.service.host:localhost}")
    private String temporalHost;

    @Value("${temporal.service.port:7233}")
    private int temporalPort;

    @Value("${temporal.namespace:default}")
    private String temporalNamespace;

    @Value("${temporal.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${temporal.worker.max-concurrent-activities:10}")
    private int maxConcurrentActivities;

    @Value("${temporal.worker.max-concurrent-workflows:10}")
    private int maxConcurrentWorkflows;

    private WorkerFactory workerFactory;

    /**
     * Create WorkflowServiceStubs for connecting to Temporal server
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        String target = String.format("%s:%d", temporalHost, temporalPort);
        log.info("Creating Temporal WorkflowServiceStubs: target={}", target);

        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(target)
                .build();

        WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(options);

        log.info("Temporal WorkflowServiceStubs created successfully");
        return stubs;
    }

    /**
     * Create WorkflowClient for starting and managing workflows
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        log.info("Creating Temporal WorkflowClient: namespace={}", temporalNamespace);

        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(temporalNamespace)
                .build();

        WorkflowClient client = WorkflowClient.newInstance(serviceStubs, options);

        log.info("Temporal WorkflowClient created successfully");
        return client;
    }

    /**
     * Create WorkerFactory for managing all workers
     */
    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("Creating Temporal WorkerFactory");
        this.workerFactory = WorkerFactory.newInstance(workflowClient);
        log.info("Temporal WorkerFactory created successfully");
        return this.workerFactory;
    }

    /**
     * Create and configure the Policy Worker
     */
    @Bean
    public Worker policyWorker(WorkerFactory workerFactory,
                                PolicySnapshotActivityImpl activityImpl) {
        log.info("Creating Policy Worker: taskQueue={}", POLICY_TASK_QUEUE);

        Worker worker = workerFactory.newWorker(POLICY_TASK_QUEUE);

        // Register workflow implementations
        worker.registerWorkflowImplementationTypes(PolicyWorkflowImpl.class);

        // Register activity implementations
        worker.registerActivitiesImplementations(activityImpl);

        log.info("Policy Worker configured successfully with workflow and activity implementations");
        return worker;
    }

    /**
     * Start all workers after bean initialization
     */
    @PostConstruct
    public void startWorkers() {
        if (workerEnabled && workerFactory != null) {
            log.info("Starting Temporal workers: maxConcurrentActivities={}, maxConcurrentWorkflows={}",
                    maxConcurrentActivities, maxConcurrentWorkflows);
            workerFactory.start();
            log.info("Temporal workers started successfully");
        } else {
            log.info("Temporal workers disabled or factory not initialized");
        }
    }

    /**
     * Shutdown workers gracefully before application shutdown
     */
    @PreDestroy
    public void shutdownWorkers() {
        if (workerFactory != null) {
            log.info("Shutting down Temporal workers");
            workerFactory.shutdown();
            log.info("Temporal workers shutdown completed");
        }
    }
}
