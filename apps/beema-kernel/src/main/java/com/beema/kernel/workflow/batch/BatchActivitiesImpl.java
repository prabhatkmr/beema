package com.beema.kernel.workflow.batch;

import com.beema.kernel.domain.batch.BatchJobConfig;
import com.beema.kernel.repository.batch.BatchJobConfigRepository;
import com.beema.kernel.batch.DynamicBatchJob;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Date;

@Component
public class BatchActivitiesImpl implements BatchActivities {

    private final BatchJobConfigRepository configRepository;
    private final DynamicBatchJob dynamicBatchJob;
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;

    public BatchActivitiesImpl(
            BatchJobConfigRepository configRepository,
            DynamicBatchJob dynamicBatchJob,
            JobLauncher jobLauncher,
            JobExplorer jobExplorer) {
        this.configRepository = configRepository;
        this.dynamicBatchJob = dynamicBatchJob;
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
    }

    @Override
    public void validateBatchJobConfig(String jobName) {
        BatchJobConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new IllegalArgumentException("Batch job config not found: " + jobName));

        if (!config.getEnabled()) {
            throw new IllegalStateException("Batch job is disabled: " + jobName);
        }
    }

    @Override
    public Long startBatchJob(String jobName, Map<String, Object> parameters) {
        BatchJobConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new IllegalArgumentException("Batch job config not found: " + jobName));

        Job job = dynamicBatchJob.createJob(config);

        // Build job parameters
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addDate("startTime", new Date());
        parameters.forEach((key, value) -> {
            if (value instanceof String) {
                paramsBuilder.addString(key, (String) value);
            } else if (value instanceof Long) {
                paramsBuilder.addLong(key, (Long) value);
            } else if (value instanceof Double) {
                paramsBuilder.addDouble(key, (Double) value);
            }
        });

        try {
            JobExecution execution = jobLauncher.run(job, paramsBuilder.toJobParameters());
            return execution.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start batch job: " + e.getMessage(), e);
        }
    }

    @Override
    public BatchJobStatus checkBatchJobStatus(Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            throw new IllegalArgumentException("Job execution not found: " + jobExecutionId);
        }

        return switch (execution.getStatus()) {
            case COMPLETED -> BatchJobStatus.COMPLETED;
            case FAILED -> BatchJobStatus.FAILED;
            case STOPPED -> BatchJobStatus.STOPPED;
            case STARTED, STARTING -> BatchJobStatus.RUNNING;
            default -> BatchJobStatus.UNKNOWN;
        };
    }

    @Override
    public BatchExecutionResult getBatchJobResult(Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            throw new IllegalArgumentException("Job execution not found: " + jobExecutionId);
        }

        long readCount = 0;
        long writeCount = 0;
        long skipCount = 0;

        for (StepExecution stepExecution : execution.getStepExecutions()) {
            readCount += stepExecution.getReadCount();
            writeCount += stepExecution.getWriteCount();
            skipCount += stepExecution.getSkipCount();
        }

        return new BatchExecutionResult(
                jobExecutionId,
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                execution.getStartTime(),
                execution.getEndTime(),
                readCount,
                writeCount,
                skipCount,
                execution.getExitStatus().getExitCode(),
                execution.getAllFailureExceptions().isEmpty() ? null :
                    execution.getAllFailureExceptions().get(0).getMessage()
        );
    }
}
