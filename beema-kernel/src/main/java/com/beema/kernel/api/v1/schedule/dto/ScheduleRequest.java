package com.beema.kernel.api.v1.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for creating/updating a tenant batch schedule.
 */
public record ScheduleRequest(
    @NotBlank(message = "Tenant ID is required")
    String tenantId,

    @NotBlank(message = "Schedule ID is required")
    String scheduleId,

    @NotBlank(message = "Job type is required")
    String jobType,

    @NotBlank(message = "Cron expression is required")
    String cronExpression,

    @NotNull(message = "Job params are required")
    Map<String, Object> jobParams,

    String createdBy
) {
    public ScheduleRequest {
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = "system";
        }
    }
}
