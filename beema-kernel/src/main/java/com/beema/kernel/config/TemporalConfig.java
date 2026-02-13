package com.beema.kernel.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal SDK configuration.
 *
 * Creates WorkflowServiceStubs, WorkflowClient, and ScheduleClient beans
 * for communicating with the Temporal server.
 *
 * Enabled only when temporal.enabled=true (default: false).
 */
@Configuration
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true")
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);

    private final TemporalProperties temporalProperties;

    public TemporalConfig(TemporalProperties temporalProperties) {
        this.temporalProperties = temporalProperties;
    }

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Connecting to Temporal server at {}", temporalProperties.getEndpoint());
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalProperties.getEndpoint())
                .build();
        return WorkflowServiceStubs.newServiceStubs(options);
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        log.info("Creating Temporal WorkflowClient for namespace '{}'",
                temporalProperties.getNamespace());
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(temporalProperties.getNamespace())
                .build();
        return WorkflowClient.newInstance(serviceStubs, options);
    }

    @Bean
    public ScheduleClient scheduleClient(WorkflowServiceStubs serviceStubs) {
        log.info("Creating Temporal ScheduleClient for namespace '{}'",
                temporalProperties.getNamespace());
        ScheduleClientOptions options = ScheduleClientOptions.newBuilder()
                .setNamespace(temporalProperties.getNamespace())
                .build();
        return ScheduleClient.newInstance(serviceStubs, options);
    }
}
