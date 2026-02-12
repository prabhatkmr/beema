package com.beema.kernel.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal Configuration
 *
 * Configures Temporal client and service stubs for connecting to Temporal server.
 */
@Configuration
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);

    @Value("${temporal.service.host:localhost}")
    private String temporalHost;

    @Value("${temporal.service.port:7233}")
    private int temporalPort;

    @Value("${temporal.namespace:default}")
    private String temporalNamespace;

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
}
