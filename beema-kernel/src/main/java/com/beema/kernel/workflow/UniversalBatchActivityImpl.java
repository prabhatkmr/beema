package com.beema.kernel.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Activity implementation that executes Spring Batch jobs.
 *
 * This runs inside the Temporal worker and bridges Temporal workflows
 * with Spring Batch job execution.
 */
@Component
public class UniversalBatchActivityImpl implements UniversalBatchActivity {

    private static final Logger log = LoggerFactory.getLogger(UniversalBatchActivityImpl.class);

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobRegistry;

    public UniversalBatchActivityImpl(JobLauncher jobLauncher, Map<String, Job> jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @Override
    public String runBatchJob(String tenantId, String jobType, Map<String, Object> jobParams) {
        log.info("Executing batch job: tenant={}, jobType={}", tenantId, jobType);

        Job job = resolveJob(jobType);

        JobParametersBuilder paramsBuilder = new JobParametersBuilder()
                .addString("tenantId", tenantId)
                .addString("jobType", jobType)
                .addLong("timestamp", System.currentTimeMillis());

        if (jobParams != null) {
            jobParams.forEach((key, value) -> {
                if (value != null) {
                    paramsBuilder.addString(key, value.toString());
                }
            });
        }

        try {
            JobParameters params = paramsBuilder.toJobParameters();
            var execution = jobLauncher.run(job, params);

            String result = String.format("Job %s completed with status %s (executionId=%d)",
                    jobType, execution.getStatus(), execution.getId());
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("Batch job failed: tenant={}, jobType={}", tenantId, jobType, e);
            throw new RuntimeException("Batch job execution failed: " + e.getMessage(), e);
        }
    }

    private Job resolveJob(String jobType) {
        String jobName = switch (jobType) {
            case "PARQUET_EXPORT" -> "universalParquetExport";
            default -> jobType.toLowerCase().replace("_", "-");
        };

        Job job = jobRegistry.get(jobName);
        if (job == null) {
            throw new IllegalArgumentException("Unknown job type: " + jobType
                    + ". Available jobs: " + jobRegistry.keySet());
        }
        return job;
    }
}
