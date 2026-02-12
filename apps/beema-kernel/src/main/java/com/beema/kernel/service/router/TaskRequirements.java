package com.beema.kernel.service.router;

import java.util.List;

/**
 * Requirements for routing a task to the best available user.
 */
public record TaskRequirements(
        List<String> requiredSkills,
        String location,
        TaskComplexity complexity,
        TaskPriority priority
) {

    public TaskRequirements(List<String> requiredSkills) {
        this(requiredSkills, null, TaskComplexity.MEDIUM, TaskPriority.NORMAL);
    }

    public TaskRequirements(List<String> requiredSkills, TaskComplexity complexity) {
        this(requiredSkills, null, complexity, TaskPriority.NORMAL);
    }
}
