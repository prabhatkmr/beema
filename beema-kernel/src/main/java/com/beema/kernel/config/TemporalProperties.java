package com.beema.kernel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Temporal integration.
 *
 * Maps properties from application.yml:
 * <pre>
 * temporal:
 *   endpoint: localhost:7233
 *   namespace: default
 *   task-queue: beema-batch-queue
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "temporal")
public class TemporalProperties {

    private String endpoint = "localhost:7233";
    private String namespace = "default";
    private String taskQueue = "beema-batch-queue";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(String taskQueue) {
        this.taskQueue = taskQueue;
    }
}
