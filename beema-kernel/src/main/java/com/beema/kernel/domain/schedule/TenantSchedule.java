package com.beema.kernel.domain.schedule;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a per-tenant batch job schedule.
 *
 * Maps to sys_tenant_schedules table. Each schedule defines
 * a recurring batch job (e.g., Parquet export) configured per
 * tenant with flexible JSONB parameters.
 */
@Entity
@Table(
    name = "sys_tenant_schedules",
    indexes = {
        @Index(name = "idx_tenant_schedules_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tenant_schedules_active", columnList = "tenant_id, is_active"),
        @Index(name = "idx_tenant_schedules_job_type", columnList = "job_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tenant_schedule_id", columnNames = {"tenant_id", "schedule_id"})
    }
)
public class TenantSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "schedule_id", nullable = false, length = 200)
    private String scheduleId;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "job_params", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> jobParams = new HashMap<>();

    @Column(name = "temporal_schedule_id", length = 500)
    private String temporalScheduleId;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy = "system";

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy = "system";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public TenantSchedule() {}

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Map<String, Object> getJobParams() {
        return jobParams;
    }

    public void setJobParams(Map<String, Object> jobParams) {
        this.jobParams = jobParams;
    }

    public String getTemporalScheduleId() {
        return temporalScheduleId;
    }

    public void setTemporalScheduleId(String temporalScheduleId) {
        this.temporalScheduleId = temporalScheduleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "TenantSchedule{" +
               "id=" + id +
               ", tenantId='" + tenantId + '\'' +
               ", scheduleId='" + scheduleId + '\'' +
               ", jobType='" + jobType + '\'' +
               ", cronExpression='" + cronExpression + '\'' +
               ", isActive=" + isActive +
               '}';
    }
}
