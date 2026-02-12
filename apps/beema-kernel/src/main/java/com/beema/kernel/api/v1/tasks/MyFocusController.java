package com.beema.kernel.api.v1.tasks;

import com.beema.kernel.domain.user.SysUser;
import com.beema.kernel.repository.user.SysUserRepository;
import com.beema.kernel.service.router.TaskPriority;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * My Focus Dashboard API
 *
 * Shows tasks routed to the current user, sorted by priority.
 * Replaces generic task lists with personalized, smart-routed work queue.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class MyFocusController {

    private final SysUserRepository userRepository;
    private final MyFocusService myFocusService;

    public MyFocusController(SysUserRepository userRepository, MyFocusService myFocusService) {
        this.userRepository = userRepository;
        this.myFocusService = myFocusService;
    }

    /**
     * Get all tasks assigned to the current user via smart routing.
     *
     * @param authentication Current user authentication
     * @return List of tasks sorted by priority
     */
    @GetMapping("/my-focus")
    public ResponseEntity<List<MyFocusTaskResponse>> getMyFocusTasks(Authentication authentication) {
        String userEmail = authentication.getName();

        SysUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        List<MyFocusTask> tasks = myFocusService.getTasksForUser(user.getId());

        // Sort by priority (URGENT first, then HIGH, NORMAL, LOW)
        List<MyFocusTaskResponse> sortedTasks = tasks.stream()
                .sorted(Comparator.comparing(MyFocusTask::priority).reversed())
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(sortedTasks);
    }

    /**
     * Get user's current work statistics.
     *
     * @param authentication Current user authentication
     * @return Work statistics
     */
    @GetMapping("/my-focus/stats")
    public ResponseEntity<MyFocusStatsResponse> getMyFocusStats(Authentication authentication) {
        String userEmail = authentication.getName();

        SysUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        List<MyFocusTask> tasks = myFocusService.getTasksForUser(user.getId());

        long urgentCount = tasks.stream().filter(t -> t.priority() == TaskPriority.URGENT).count();
        long highCount = tasks.stream().filter(t -> t.priority() == TaskPriority.HIGH).count();
        long normalCount = tasks.stream().filter(t -> t.priority() == TaskPriority.NORMAL).count();
        long lowCount = tasks.stream().filter(t -> t.priority() == TaskPriority.LOW).count();

        MyFocusStatsResponse stats = new MyFocusStatsResponse(
                user.getCurrentTasks(),
                user.getMaxTasks(),
                user.getCapacityUtilization(),
                urgentCount,
                highCount,
                normalCount,
                lowCount,
                user.getSkillTags()
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Update user availability status.
     *
     * @param request Availability update request
     * @param authentication Current user authentication
     * @return Updated user
     */
    @PutMapping("/my-focus/availability")
    public ResponseEntity<Void> updateAvailability(
            @RequestBody UpdateAvailabilityRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        SysUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        user.setAvailabilityStatus(request.status());
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    private MyFocusTaskResponse toResponse(MyFocusTask task) {
        return new MyFocusTaskResponse(
                task.taskId(),
                task.taskType(),
                task.description(),
                task.priority(),
                task.complexity(),
                task.requiredSkills(),
                task.assignedAt(),
                task.dueDate()
        );
    }
}
