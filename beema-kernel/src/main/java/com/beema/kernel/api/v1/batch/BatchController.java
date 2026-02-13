package com.beema.kernel.api.v1.batch;

import com.beema.kernel.api.v1.batch.dto.ParquetExportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for triggering batch export jobs.
 */
@RestController
@RequestMapping("/api/v1/batch")
@Tag(name = "Batch", description = "Batch export operations")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final JobLauncher jobLauncher;
    private final Job universalParquetExportJob;

    public BatchController(JobLauncher jobLauncher, Job universalParquetExportJob) {
        this.jobLauncher = jobLauncher;
        this.universalParquetExportJob = universalParquetExportJob;
    }

    @PostMapping("/export/parquet")
    @Operation(summary = "Export agreements to Parquet",
            description = "Triggers an async batch job to export current agreements to Parquet format and upload to blob storage.")
    public ResponseEntity<Map<String, Object>> exportParquet(
            @Valid @RequestBody ParquetExportRequest request) {
        try {
            JobParametersBuilder paramsBuilder = new JobParametersBuilder()
                    .addString("tenantId", request.getTenantId())
                    .addLong("timestamp", System.currentTimeMillis());

            if (request.getFromDate() != null) {
                paramsBuilder.addString("fromDate", request.getFromDate().toString());
            }
            if (request.getToDate() != null) {
                paramsBuilder.addString("toDate", request.getToDate().toString());
            }

            JobParameters params = paramsBuilder.toJobParameters();
            var execution = jobLauncher.run(universalParquetExportJob, params);

            Map<String, Object> response = new HashMap<>();
            response.put("jobExecutionId", execution.getId());
            response.put("jobName", execution.getJobInstance().getJobName());
            response.put("status", execution.getStatus().name());
            response.put("tenantId", request.getTenantId());
            response.put("outputPath", String.format(
                    "tenant=%s/object=agreement/date=<today>/<uuid>.parquet",
                    request.getTenantId()));

            log.info("Started Parquet export job {} for tenant {}",
                    execution.getId(), request.getTenantId());

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to launch Parquet export job for tenant {}",
                    request.getTenantId(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to launch export job");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
