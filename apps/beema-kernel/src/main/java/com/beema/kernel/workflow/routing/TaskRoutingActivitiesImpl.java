package com.beema.kernel.workflow.routing;

import com.beema.kernel.service.router.RoutingResult;
import com.beema.kernel.service.router.TaskRequirements;
import com.beema.kernel.service.router.WorkRouterService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TaskRoutingActivitiesImpl implements TaskRoutingActivities {

    private final WorkRouterService routerService;

    public TaskRoutingActivitiesImpl(WorkRouterService routerService) {
        this.routerService = routerService;
    }

    @Override
    public RoutingResult routeTask(TaskRequirements requirements) {
        return routerService.routeTask(requirements);
    }

    @Override
    public void releaseTask(UUID userId) {
        routerService.releaseTask(userId);
    }
}
