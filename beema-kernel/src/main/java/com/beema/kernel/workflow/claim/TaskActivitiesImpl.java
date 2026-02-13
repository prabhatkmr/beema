package com.beema.kernel.workflow.claim;

import com.beema.kernel.domain.task.SysTask;
import com.beema.kernel.repository.task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementation of TaskActivities.
 *
 * Inserts a row into sys_tasks using the Temporal workflow execution
 * context (workflowId, runId) so that the bridge service knows which
 * workflow to signal when the human completes the task.
 */
@Component
public class TaskActivitiesImpl implements TaskActivities {

    private static final Logger log = LoggerFactory.getLogger(TaskActivitiesImpl.class);

    private final TaskRepository taskRepository;

    public TaskActivitiesImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public UUID createHumanTask(
            String workflowId,
            String runId,
            String tenantId,
            String assigneeRole,
            String taskType,
            String title,
            String description,
            String signalName) {

        SysTask task = new SysTask();
        task.setWorkflowId(workflowId);
        task.setRunId(runId);
        task.setTenantId(tenantId);
        task.setAssigneeRole(assigneeRole);
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setDescription(description);
        task.setSignalName(signalName);

        SysTask saved = taskRepository.save(task);

        log.info("Created human task {} for workflow {} assigned to role '{}': {}",
                saved.getId(), workflowId, assigneeRole, title);

        return saved.getId();
    }
}
