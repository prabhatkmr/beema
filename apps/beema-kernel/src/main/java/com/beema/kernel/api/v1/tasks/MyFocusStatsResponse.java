package com.beema.kernel.api.v1.tasks;

import java.util.List;

public record MyFocusStatsResponse(
        int currentTasks,
        int maxTasks,
        double capacityUtilization,
        long urgentTasks,
        long highPriorityTasks,
        long normalPriorityTasks,
        long lowPriorityTasks,
        List<String> userSkills
) {
}
