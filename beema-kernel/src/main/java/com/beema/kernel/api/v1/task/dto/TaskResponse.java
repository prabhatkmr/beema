package com.beema.kernel.api.v1.task.dto;

import com.beema.kernel.domain.task.SysTask;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String workflowId,
    String runId,
    String signalName,
    String taskType,
    String title,
    String description,
    String assigneeRole,
    String assigneeUser,
    String status,
    String outcome,
    Map<String, Object> outcomeData,
    OffsetDateTime dueAt,
    OffsetDateTime completedAt,
    String completedBy,
    String tenantId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static TaskResponse from(SysTask task) {
        return new TaskResponse(
            task.getId(),
            task.getWorkflowId(),
            task.getRunId(),
            task.getSignalName(),
            task.getTaskType(),
            task.getTitle(),
            task.getDescription(),
            task.getAssigneeRole(),
            task.getAssigneeUser(),
            task.getStatus(),
            task.getOutcome(),
            task.getOutcomeData(),
            task.getDueAt(),
            task.getCompletedAt(),
            task.getCompletedBy(),
            task.getTenantId(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
