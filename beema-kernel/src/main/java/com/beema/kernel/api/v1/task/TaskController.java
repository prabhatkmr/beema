package com.beema.kernel.api.v1.task;

import com.beema.kernel.api.v1.task.dto.TaskCompleteRequest;
import com.beema.kernel.api.v1.task.dto.TaskResponse;
import com.beema.kernel.domain.task.SysTask;
import com.beema.kernel.service.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the human-in-the-loop task inbox.
 *
 * Provides endpoints for users to view and complete tasks
 * that were created by Temporal workflows awaiting human decisions.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Human-in-the-loop task inbox for workflow approvals")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/{taskId}/complete")
    @Operation(summary = "Complete a task", description = "Complete a human task and signal the waiting Temporal workflow to resume")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskCompleteRequest request) {
        SysTask task = taskService.completeTask(
                taskId,
                request.outcome(),
                request.outcomeData(),
                request.completedBy()
        );
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "Cancel a task", description = "Cancel an open task without signaling the workflow")
    public ResponseEntity<TaskResponse> cancelTask(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "system") String cancelledBy) {
        SysTask task = taskService.cancelTask(taskId, cancelledBy);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        SysTask task = taskService.getById(taskId);
        return ResponseEntity.ok(TaskResponse.from(task));
    }

    @GetMapping
    @Operation(summary = "List open tasks", description = "List all open tasks, optionally filtered by assignee role")
    public ResponseEntity<List<TaskResponse>> listTasks(
            @RequestParam(required = false) String role,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        List<SysTask> tasks;
        if (role != null && !role.isBlank()) {
            tasks = taskService.getOpenTasksByRole(role);
        } else {
            tasks = taskService.getTasksByTenant(tenantId);
        }
        return ResponseEntity.ok(tasks.stream().map(TaskResponse::from).toList());
    }

    @GetMapping("/open")
    @Operation(summary = "List all open tasks across tenants")
    public ResponseEntity<List<TaskResponse>> listOpenTasks() {
        List<SysTask> tasks = taskService.getOpenTasks();
        return ResponseEntity.ok(tasks.stream().map(TaskResponse::from).toList());
    }
}
