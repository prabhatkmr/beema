package com.beema.kernel.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch configuration.
 *
 * Enables batch processing infrastructure including:
 * - JobRepository (metadata storage)
 * - JobLauncher (job execution)
 * - JobExplorer (job monitoring)
 * - Transaction management
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // Spring Boot auto-configures JobRepository, JobLauncher, etc.
    // Custom beans can be added here if needed
}
