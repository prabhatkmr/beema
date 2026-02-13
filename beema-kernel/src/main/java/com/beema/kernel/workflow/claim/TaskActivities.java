package com.beema.kernel.workflow.claim;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.UUID;

/**
 * Temporal Activity interface for creating human-in-the-loop tasks.
 *
 * Called from within a workflow to insert a task into the sys_tasks
 * "Inbox" table. The workflow then pauses (via Workflow.await) until
 * a human completes the task and the bridge service sends a signal.
 */
@ActivityInterface
public interface TaskActivities {

    /**
     * Create a human task in the inbox.
     *
     * @param workflowId   the current workflow execution ID
     * @param runId        the current workflow run ID
     * @param tenantId     tenant context
     * @param assigneeRole role that should handle this task (e.g., "CLAIMS_ADJUSTER")
     * @param taskType     type of task: APPROVAL, REVIEW, ACTION, ESCALATION
     * @param title        short title for the task
     * @param description  detailed description of what needs to be done
     * @param signalName   Temporal signal name to send on completion
     * @return the created task UUID
     */
    @ActivityMethod
    UUID createHumanTask(
        String workflowId,
        String runId,
        String tenantId,
        String assigneeRole,
        String taskType,
        String title,
        String description,
        String signalName
    );
}
