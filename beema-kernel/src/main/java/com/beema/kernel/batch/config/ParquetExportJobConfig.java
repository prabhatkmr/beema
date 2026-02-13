package com.beema.kernel.batch.config;

import com.beema.kernel.batch.export.AgreementItemReader;
import com.beema.kernel.batch.export.JsonToAvroProcessor;
import com.beema.kernel.batch.export.ParquetBlobWriter;
import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.service.storage.BlobStorageService;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch configuration for the universal Parquet export job.
 *
 * Job: universalParquetExport
 *   Step: exportAgreementsStep
 *     Reader: AgreementItemReader (JDBC cursor, tenant-filtered)
 *     Processor: JsonToAvroProcessor (Agreement -> Avro GenericRecord)
 *     Writer: ParquetBlobWriter (Parquet file -> BlobStorageService)
 *     Chunk size: 1000
 *
 * Job parameters:
 *   - tenantId (required): tenant to export
 */
@Configuration
public class ParquetExportJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ParquetExportJobConfig.class);

    private static final int CHUNK_SIZE = 1000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final BlobStorageService blobStorageService;

    public ParquetExportJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            BlobStorageService blobStorageService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
        this.blobStorageService = blobStorageService;
    }

    @Bean
    public Job universalParquetExportJob() {
        return new JobBuilder("universalParquetExport", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(exportAgreementsStep())
                .build();
    }

    @Bean
    public Step exportAgreementsStep() {
        // Use a default tenant for bean creation; actual tenant comes from job parameters at runtime
        // The real reader/writer are created per-execution in BatchController
        JsonToAvroProcessor processor = new JsonToAvroProcessor();
        return new StepBuilder("exportAgreementsStep", jobRepository)
                .<Agreement, GenericRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(defaultAgreementReader())
                .processor(processor)
                .writer(defaultParquetWriter(processor))
                .build();
    }

    @Bean
    public ItemReader<Agreement> defaultAgreementReader() {
        return new AgreementItemReader(dataSource, "default");
    }

    @Bean
    public ItemWriter<GenericRecord> defaultParquetWriter(JsonToAvroProcessor processor) {
        // Schema will be built lazily on first record processed
        return chunk -> {
            if (processor.getSchema() == null) {
                log.warn("No schema available yet - skipping empty chunk write");
                return;
            }
            ParquetBlobWriter writer = new ParquetBlobWriter(blobStorageService, "default", processor.getSchema());
            writer.write(chunk);
        };
    }
}
