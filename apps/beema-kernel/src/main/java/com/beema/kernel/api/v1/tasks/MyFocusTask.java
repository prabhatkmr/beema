package com.beema.kernel.api.v1.tasks;

import com.beema.kernel.service.router.TaskComplexity;
import com.beema.kernel.service.router.TaskPriority;

import java.time.LocalDateTime;
import java.util.List;

public record MyFocusTask(
        String taskId,
        String taskType,
        String description,
        TaskPriority priority,
        TaskComplexity complexity,
        List<String> requiredSkills,
        LocalDateTime assignedAt,
        LocalDateTime dueDate
) {
}
