package com.beema.kernel.workflow.claim;

import com.beema.kernel.service.router.RoutingResult;
import com.beema.kernel.service.router.TaskComplexity;
import com.beema.kernel.service.router.TaskPriority;
import com.beema.kernel.service.router.TaskRequirements;
import com.beema.kernel.workflow.routing.TaskRoutingActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

/**
 * Example workflow demonstrating smart task routing.
 *
 * Instead of: task.assignTo('jdoe')
 * Use: task.routeTo(requirements)
 *
 * The router automatically finds the best available user based on:
 * - Skill match
 * - Current workload
 * - Location
 * - Availability
 */
class SmartRoutedClaimWorkflowExample {

    private static final Logger log = Workflow.getLogger(SmartRoutedClaimWorkflowExample.class);

    private final TaskRoutingActivities routingActivities = Workflow.newActivityStub(
            TaskRoutingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build()
    );

    public String processClaimWithSmartRouting(String claimId, String claimType) {
        log.info("Processing claim {} with smart routing", claimId);

        // OLD WAY: task.assignTo('jdoe')
        // NEW WAY: Define requirements and let router find best user

        // Example 1: Auto claim (requires "auto" skill)
        if ("AUTO".equals(claimType)) {
            TaskRequirements requirements = new TaskRequirements(
                    List.of("auto"),  // Required skills
                    "US-EAST",        // Location
                    TaskComplexity.MEDIUM,
                    TaskPriority.HIGH
            );

            RoutingResult result = routingActivities.routeTask(requirements);

            if (!result.hasMatch()) {
                log.error("No available users for auto claim");
                return "NO_ADJUSTER_AVAILABLE";
            }

            log.info("Claim routed to: {} (score: {}, reason: {})",
                    result.assignedUser().getEmail(),
                    result.matchScore(),
                    result.reasoning());

            // Process claim...

            // Release task when complete
            routingActivities.releaseTask(result.assignedUser().getId());
        }

        // Example 2: Injury claim (requires "injury" skill)
        if ("INJURY".equals(claimType)) {
            TaskRequirements requirements = new TaskRequirements(
                    List.of("injury", "medical"),  // Required skills (ANY match)
                    TaskComplexity.HIGH
            );

            RoutingResult result = routingActivities.routeTask(requirements);

            if (result.hasMatch()) {
                log.info("Injury claim routed to specialist: {}",
                        result.assignedUser().getEmail());

                // Process claim...

                routingActivities.releaseTask(result.assignedUser().getId());
            }
        }

        // Example 3: Complex property claim (high complexity, urgent priority)
        if ("PROPERTY_COMPLEX".equals(claimType)) {
            TaskRequirements requirements = new TaskRequirements(
                    List.of("property", "appraisal"),
                    "US-WEST",
                    TaskComplexity.CRITICAL,
                    TaskPriority.URGENT
            );

            RoutingResult result = routingActivities.routeTask(requirements);

            if (result.hasMatch()) {
                log.info("Complex property claim routed to expert: {} (score: {})",
                        result.assignedUser().getEmail(),
                        result.matchScore());
                // Process claim...
            }
        }

        return "COMPLETED";
    }
}
