package com.beema.kernel.workflow.routing;

import com.beema.kernel.service.router.RoutingResult;
import com.beema.kernel.service.router.TaskRequirements;
import io.temporal.activity.ActivityInterface;

import java.util.UUID;

@ActivityInterface
public interface TaskRoutingActivities {

    /**
     * Route a task to the best available user.
     *
     * @param requirements Task requirements
     * @return Routing result with assigned user
     */
    RoutingResult routeTask(TaskRequirements requirements);

    /**
     * Release a task from a user (decrement WIP count).
     *
     * @param userId User ID
     */
    void releaseTask(UUID userId);
}
