package com.beema.kernel.api.v1.submission.dto;

import java.util.List;

/**
 * Response DTO for workflow execution status from Temporal.
 */
public record WorkflowStatusResponse(
    String workflowId,
    String runId,
    String status,
    String startTime,
    String closeTime,
    String taskQueue,
    List<WorkflowEvent> events
) {
    public record WorkflowEvent(
        long eventId,
        String eventType,
        String timestamp,
        String detail
    ) {}
}
