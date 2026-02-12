package com.beema.kernel.domain.batch;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Configuration for dynamic batch jobs.
 *
 * Each config defines:
 * - SQL query for reading data (Reader)
 * - JEXL script for processing/transforming (Processor)
 * - Chunk size and scheduling
 */
@Entity
@Table(name = "sys_batch_job_config")
public class BatchJobConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "job_config_id")
    private UUID jobConfigId;

    @Column(name = "job_name", nullable = false, unique = true)
    private String jobName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reader_sql", nullable = false, columnDefinition = "TEXT")
    private String readerSql;

    @Column(name = "processor_jexl", nullable = false, columnDefinition = "TEXT")
    private String processorJexl;

    @Column(name = "writer_sql", columnDefinition = "TEXT")
    private String writerSql;

    @Column(name = "chunk_size")
    private Integer chunkSize = 1000;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getJobConfigId() {
        return jobConfigId;
    }

    public void setJobConfigId(UUID jobConfigId) {
        this.jobConfigId = jobConfigId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReaderSql() {
        return readerSql;
    }

    public void setReaderSql(String readerSql) {
        this.readerSql = readerSql;
    }

    public String getProcessorJexl() {
        return processorJexl;
    }

    public void setProcessorJexl(String processorJexl) {
        this.processorJexl = processorJexl;
    }

    public String getWriterSql() {
        return writerSql;
    }

    public void setWriterSql(String writerSql) {
        this.writerSql = writerSql;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
