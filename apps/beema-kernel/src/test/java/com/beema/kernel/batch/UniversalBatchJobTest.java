package com.beema.kernel.batch;

import com.beema.kernel.domain.batch.BatchJobConfig;
import com.beema.kernel.repository.batch.BatchJobConfigRepository;
import com.beema.kernel.service.batch.BatchJobService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Universal Batch Job.
 *
 * Tests the "Close Expired Claims" and "Premium Uplift" batch operations.
 */
@SpringBootTest
@ActiveProfiles("test")
class UniversalBatchJobTest {

    @Autowired
    private BatchJobService batchJobService;

    @Autowired
    private BatchJobConfigRepository configRepository;

    @Test
    void testCloseExpiredClaims() {
        // Setup: Create batch job config
        BatchJobConfig config = new BatchJobConfig();
        config.setJobName("close-expired-claims");
        config.setDescription("Close claims older than 90 days with status OPEN");
        config.setReaderSql(
            "SELECT id, claim_number, status, created_at " +
            "FROM claims " +
            "WHERE status = 'OPEN' " +
            "AND created_at < CURRENT_DATE - INTERVAL '90 days'"
        );
        config.setProcessorJexl(
            "item.status = 'CLOSED'; " +
            "item.close_reason = 'EXPIRED'; " +
            "item"
        );
        config.setWriterSql(
            "UPDATE claims " +
            "SET status = :status, close_reason = :close_reason, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id"
        );
        config.setChunkSize(100);
        config.setEnabled(true);
        config.setTenantId("default");
        config.setCreatedBy("test");

        configRepository.save(config);

        // Execute
        JobExecution execution = batchJobService.executeBatchJob("close-expired-claims", Map.of());

        // Verify
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Verify metrics
        execution.getStepExecutions().forEach(step -> {
            assertThat(step.getReadCount()).isGreaterThanOrEqualTo(0);
            assertThat(step.getWriteCount()).isEqualTo(step.getReadCount());
            assertThat(step.getSkipCount()).isEqualTo(0);
        });
    }

    @Test
    void testPremiumUplift() {
        // Setup: Create premium uplift job
        BatchJobConfig config = new BatchJobConfig();
        config.setJobName("premium-uplift-test");
        config.setDescription("Apply 10% premium increase");
        config.setReaderSql(
            "SELECT id, premium " +
            "FROM agreements " +
            "WHERE tenant_id = 'default' AND status = 'ACTIVE'"
        );
        config.setProcessorJexl(
            "item.premium = item.premium * 1.10; " +
            "item"
        );
        config.setWriterSql(
            "UPDATE agreements " +
            "SET premium = :premium, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id"
        );
        config.setChunkSize(1000);
        config.setEnabled(true);
        config.setTenantId("default");
        config.setCreatedBy("test");

        configRepository.save(config);

        // Execute
        JobExecution execution = batchJobService.executeBatchJob("premium-uplift-test", Map.of());

        // Verify
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
