package com.beema.kernel.batch;

import com.beema.kernel.domain.batch.BatchJobConfig;
import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic batch job that reads from SQL and processes with JEXL.
 *
 * Flow:
 * 1. Reader: Execute SQL query to fetch records
 * 2. Processor: Apply JEXL script to transform each record
 * 3. Writer: Update records in database
 */
@Component
public class DynamicBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final JexlExpressionEngine jexlEngine;

    public DynamicBatchJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            JexlExpressionEngine jexlEngine) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
        this.jexlEngine = jexlEngine;
    }

    /**
     * Creates a batch job from configuration.
     */
    public Job createJob(BatchJobConfig config) {
        return new JobBuilder(config.getJobName(), jobRepository)
                .start(createStep(config))
                .build();
    }

    private Step createStep(BatchJobConfig config) {
        return new StepBuilder(config.getJobName() + "-step", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(config.getChunkSize(), transactionManager)
                .reader(createReader(config))
                .processor(createProcessor(config))
                .writer(createWriter(config))
                .build();
    }

    private ItemReader<Map<String, Object>> createReader(BatchJobConfig config) {
        return new JdbcCursorItemReaderBuilder<Map<String, Object>>()
                .name(config.getJobName() + "-reader")
                .dataSource(dataSource)
                .sql(config.getReaderSql())
                .rowMapper(mapRowMapper())
                .build();
    }

    @SuppressWarnings("unchecked")
    private ItemProcessor<Map<String, Object>, Map<String, Object>> createProcessor(BatchJobConfig config) {
        return item -> {
            // Apply JEXL transformation (note: evaluate takes record first, then script)
            Object result = jexlEngine.evaluate(item, config.getProcessorJexl());
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return item;
        };
    }

    private ItemWriter<Map<String, Object>> createWriter(BatchJobConfig config) {
        // Generic writer - for now just log
        // Can be enhanced to do SQL updates based on config
        return items -> {
            items.forEach(item ->
                System.out.println("Processed item: " + item));
        };
    }

    private RowMapper<Map<String, Object>> mapRowMapper() {
        return (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                row.put(columnName, rs.getObject(i));
            }
            return row;
        };
    }
}
