package com.beema.kernel.api.v1.task.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TaskCompleteRequest(
    @NotBlank(message = "Outcome is required") String outcome,
    Map<String, Object> outcomeData,
    String completedBy
) {
    public TaskCompleteRequest {
        if (completedBy == null || completedBy.isBlank()) {
            completedBy = "system";
        }
    }
}
