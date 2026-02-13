package com.beema.kernel.service.task;

import com.beema.kernel.domain.task.SysTask;
import com.beema.kernel.repository.task.TaskRepository;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bridge Service: connects the user-facing task inbox to Temporal workflows.
 *
 * When a user completes a task:
 *   1. Updates the task status in sys_tasks (DB)
 *   2. Sends a Signal to the paused Temporal workflow (using workflowId + runId)
 *   3. The workflow resumes execution with the outcome
 *
 * This service is the single point of coordination between the
 * database (user's inbox) and the workflow engine (Temporal).
 */
@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final WorkflowClient workflowClient;

    public TaskService(TaskRepository taskRepository, WorkflowClient workflowClient) {
        this.taskRepository = taskRepository;
        this.workflowClient = workflowClient;
    }

    /**
     * Complete a task and signal the waiting Temporal workflow.
     *
     * @param taskId      the task to complete
     * @param outcome     the human decision (e.g., "APPROVED", "REJECTED")
     * @param outcomeData optional structured data from the decision
     * @param completedBy the user who completed the task
     * @return the updated task
     */
    public SysTask completeTask(UUID taskId, String outcome, Map<String, Object> outcomeData, String completedBy) {
        SysTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!"OPEN".equals(task.getStatus())) {
            throw new IllegalStateException("Task " + taskId + " is not OPEN (current status: " + task.getStatus() + ")");
        }

        // Step 1: Update DB
        task.setStatus("COMPLETED");
        task.setOutcome(outcome);
        if (outcomeData != null) {
            task.setOutcomeData(outcomeData);
        }
        task.setCompletedAt(OffsetDateTime.now());
        task.setCompletedBy(completedBy);
        taskRepository.save(task);

        log.info("Task {} completed with outcome '{}' by {}", taskId, outcome, completedBy);

        // Step 2: Signal the waiting Temporal workflow
        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(
                    task.getWorkflowId(),
                    java.util.Optional.of(task.getRunId()),
                    java.util.Optional.empty()
            );
            workflowStub.signal(task.getSignalName(), outcome);

            log.info("Sent signal '{}' with outcome '{}' to workflow {} (run {})",
                    task.getSignalName(), outcome, task.getWorkflowId(), task.getRunId());
        } catch (Exception e) {
            log.error("Failed to signal workflow {} for task {}: {}",
                    task.getWorkflowId(), taskId, e.getMessage(), e);
            throw new RuntimeException("Task completed in DB but failed to signal workflow: " + e.getMessage(), e);
        }

        return task;
    }

    /**
     * Cancel an open task without signaling the workflow.
     */
    public SysTask cancelTask(UUID taskId, String cancelledBy) {
        SysTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!"OPEN".equals(task.getStatus())) {
            throw new IllegalStateException("Task " + taskId + " is not OPEN");
        }

        task.setStatus("CANCELLED");
        task.setCompletedBy(cancelledBy);
        task.setCompletedAt(OffsetDateTime.now());
        return taskRepository.save(task);
    }

    public SysTask getById(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    public List<SysTask> getOpenTasksByRole(String assigneeRole) {
        return taskRepository.findByAssigneeRoleAndStatus(assigneeRole, "OPEN");
    }

    public List<SysTask> getTasksByTenant(String tenantId) {
        return taskRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<SysTask> getOpenTasks() {
        return taskRepository.findByStatusOrderByCreatedAtAsc("OPEN");
    }
}
