package com.beema.kernel.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import com.beema.kernel.service.expression.JexlExpressionEngine;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Universal Batch Job Configuration.
 *
 * Creates a generic batch job that can execute any batch operation
 * by accepting SQL and JEXL parameters at runtime.
 *
 * Job Parameters:
 * - readerSql: SQL query to read data
 * - processorScript: JEXL script to transform data
 * - writerSql: SQL statement to write data
 * - chunkSize: Number of items to process in each chunk (default: 1000)
 */
@Configuration
public class UniversalBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final JexlExpressionEngine jexlEngine;

    public UniversalBatchJobConfig(
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
     * Universal batch job that uses parameter-driven components.
     */
    @Bean(name = "universalBatchJob")
    public Job universalBatchJob() {
        return new JobBuilder("universalBatchJob", jobRepository)
                .start(universalStep())
                .build();
    }

    @Bean
    public Step universalStep() {
        return new StepBuilder("universal-step", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(1000, transactionManager)
                .reader(universalReader(null))
                .processor(universalProcessor(null))
                .writer(universalWriter(null))
                .build();
    }

    /**
     * JDBC Reader with @StepScope for late binding of readerSql parameter.
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Map<String, Object>> universalReader(
            @Value("#{jobParameters['readerSql']}") String readerSql) {

        return new JdbcCursorItemReaderBuilder<Map<String, Object>>()
                .name("universal-reader")
                .dataSource(dataSource)
                .sql(readerSql)
                .rowMapper(new ColumnMapRowMapper())
                .build();
    }

    /**
     * JEXL Processor with @StepScope for late binding of processorScript parameter.
     */
    @Bean
    @StepScope
    public ItemProcessor<Map<String, Object>, Map<String, Object>> universalProcessor(
            @Value("#{jobParameters['processorScript']}") String processorScript) {

        return new JexlItemProcessor(processorScript, jexlEngine);
    }

    /**
     * JDBC Writer with @StepScope for late binding of writerSql parameter.
     */
    @Bean
    @StepScope
    public JdbcBatchItemWriter<Map<String, Object>> universalWriter(
            @Value("#{jobParameters['writerSql']}") String writerSql) {

        return new JdbcBatchItemWriterBuilder<Map<String, Object>>()
                .dataSource(dataSource)
                .sql(writerSql)
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource(item))
                .build();
    }
}
