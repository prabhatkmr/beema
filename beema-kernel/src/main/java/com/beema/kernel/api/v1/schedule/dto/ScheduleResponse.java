package com.beema.kernel.api.v1.schedule.dto;

import com.beema.kernel.domain.schedule.TenantSchedule;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for tenant batch schedules.
 */
public record ScheduleResponse(
    UUID id,
    String tenantId,
    String scheduleId,
    String jobType,
    String cronExpression,
    Boolean isActive,
    Map<String, Object> jobParams,
    String temporalScheduleId,
    String createdBy,
    String updatedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static ScheduleResponse from(TenantSchedule entity) {
        return new ScheduleResponse(
            entity.getId(),
            entity.getTenantId(),
            entity.getScheduleId(),
            entity.getJobType(),
            entity.getCronExpression(),
            entity.getIsActive(),
            entity.getJobParams(),
            entity.getTemporalScheduleId(),
            entity.getCreatedBy(),
            entity.getUpdatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
