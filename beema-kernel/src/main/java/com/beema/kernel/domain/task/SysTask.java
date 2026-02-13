package com.beema.kernel.domain.task;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Human-in-the-loop task entity.
 *
 * Represents a task in the "Inbox" created by a Temporal workflow
 * that requires human action (approval, review, etc.) before the
 * workflow can resume. Acts as the bridge between Temporal workflows
 * and the user-facing task UI.
 *
 * Lifecycle:
 *   1. Workflow activity inserts a row with status=OPEN
 *   2. Workflow pauses via Workflow.await() waiting for a signal
 *   3. User completes the task via REST API
 *   4. TaskService updates status=COMPLETED and sends Temporal signal
 *   5. Workflow resumes with the outcome
 */
@Entity
@Table(
    name = "sys_tasks",
    indexes = {
        @Index(name = "idx_sys_tasks_workflow", columnList = "workflow_id, run_id"),
        @Index(name = "idx_sys_tasks_assignee", columnList = "assignee_role, status"),
        @Index(name = "idx_sys_tasks_status", columnList = "status, tenant_id"),
        @Index(name = "idx_sys_tasks_tenant", columnList = "tenant_id, created_at")
    }
)
public class SysTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false, length = 500)
    private String workflowId;

    @Column(name = "run_id", nullable = false, length = 500)
    private String runId;

    @Column(name = "signal_name", nullable = false, length = 200)
    private String signalName = "HumanApproval";

    @Column(name = "task_type", nullable = false, length = 100)
    private String taskType = "APPROVAL";

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_role", nullable = false, length = 100)
    private String assigneeRole;

    @Column(name = "assignee_user", length = 200)
    private String assigneeUser;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "outcome", length = 100)
    private String outcome;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "outcome_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> outcomeData = new HashMap<>();

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "completed_by", length = 200)
    private String completedBy;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId = "default";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public SysTask() {}

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getSignalName() { return signalName; }
    public void setSignalName(String signalName) { this.signalName = signalName; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssigneeRole() { return assigneeRole; }
    public void setAssigneeRole(String assigneeRole) { this.assigneeRole = assigneeRole; }

    public String getAssigneeUser() { return assigneeUser; }
    public void setAssigneeUser(String assigneeUser) { this.assigneeUser = assigneeUser; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public Map<String, Object> getOutcomeData() { return outcomeData; }
    public void setOutcomeData(Map<String, Object> outcomeData) { this.outcomeData = outcomeData; }

    public OffsetDateTime getDueAt() { return dueAt; }
    public void setDueAt(OffsetDateTime dueAt) { this.dueAt = dueAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "SysTask{" +
               "id=" + id +
               ", workflowId='" + workflowId + '\'' +
               ", taskType='" + taskType + '\'' +
               ", assigneeRole='" + assigneeRole + '\'' +
               ", status='" + status + '\'' +
               ", outcome='" + outcome + '\'' +
               '}';
    }
}
