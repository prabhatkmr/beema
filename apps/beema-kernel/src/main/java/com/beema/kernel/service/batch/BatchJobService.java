package com.beema.kernel.service.batch;

import com.beema.kernel.domain.batch.BatchJobConfig;
import com.beema.kernel.repository.batch.BatchJobConfigRepository;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service for launching batch jobs using the universal batch job.
 */
@Service
public class BatchJobService {

    private final BatchJobConfigRepository configRepository;
    private final JobLauncher jobLauncher;
    private final Job universalBatchJob;

    public BatchJobService(
            BatchJobConfigRepository configRepository,
            JobLauncher jobLauncher,
            Job universalBatchJob) {
        this.configRepository = configRepository;
        this.jobLauncher = jobLauncher;
        this.universalBatchJob = universalBatchJob;
    }

    /**
     * Executes a batch job by name using the universal batch job.
     */
    public JobExecution executeBatchJob(String jobName, Map<String, Object> additionalParams) {
        // Load config
        BatchJobConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobName));

        if (!config.getEnabled()) {
            throw new IllegalStateException("Batch job is disabled: " + jobName);
        }

        // Build job parameters
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();

        // Add required parameters from config
        paramsBuilder.addString("readerSql", config.getReaderSql());
        paramsBuilder.addString("processorScript", config.getProcessorJexl());
        paramsBuilder.addString("writerSql", config.getWriterSql() != null
                ? config.getWriterSql()
                : generateWriterSql(config));
        paramsBuilder.addLong("chunkSize", config.getChunkSize().longValue());

        // Add unique run ID
        paramsBuilder.addString("runId", UUID.randomUUID().toString());
        paramsBuilder.addDate("startTime", new Date());

        // Add tenant context
        paramsBuilder.addString("tenantId", config.getTenantId());

        // Add any additional parameters
        if (additionalParams != null) {
            additionalParams.forEach((key, value) -> {
                if (value instanceof String) {
                    paramsBuilder.addString(key, (String) value);
                } else if (value instanceof Long) {
                    paramsBuilder.addLong(key, (Long) value);
                } else if (value instanceof Double) {
                    paramsBuilder.addDouble(key, (Double) value);
                } else if (value instanceof Date) {
                    paramsBuilder.addDate(key, (Date) value);
                }
            });
        }

        try {
            return jobLauncher.run(universalBatchJob, paramsBuilder.toJobParameters());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute batch job: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a fallback writer SQL statement from config.
     */
    private String generateWriterSql(BatchJobConfig config) {
        return "UPDATE agreements SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id";
    }
}
