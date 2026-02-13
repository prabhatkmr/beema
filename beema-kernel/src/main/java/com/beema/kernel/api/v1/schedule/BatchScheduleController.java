package com.beema.kernel.api.v1.schedule;

import com.beema.kernel.api.v1.schedule.dto.ScheduleRequest;
import com.beema.kernel.api.v1.schedule.dto.ScheduleResponse;
import com.beema.kernel.api.v1.schedule.dto.TriggerRequest;
import com.beema.kernel.domain.schedule.TenantSchedule;
import com.beema.kernel.service.schedule.TenantScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for tenant batch schedule management.
 *
 * Endpoints:
 * - POST   /api/v1/schedules           - Create schedule
 * - GET    /api/v1/schedules            - List schedules for tenant
 * - GET    /api/v1/schedules/{id}       - Get schedule by ID
 * - PUT    /api/v1/schedules/{id}       - Update schedule
 * - DELETE /api/v1/schedules/{id}       - Delete schedule
 * - POST   /api/v1/schedules/{id}/trigger  - Trigger immediate run
 * - POST   /api/v1/schedules/{id}/pause    - Pause schedule
 * - POST   /api/v1/schedules/{id}/unpause  - Unpause schedule
 */
@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Batch Schedules", description = "Per-tenant batch job schedule management")
public class BatchScheduleController {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduleController.class);

    private final TenantScheduleService scheduleService;

    public BatchScheduleController(TenantScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @Operation(summary = "Create schedule", description = "Create a new batch schedule for a tenant")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleRequest request
    ) {
        log.info("Creating schedule '{}' for tenant '{}'", request.scheduleId(), request.tenantId());

        TenantSchedule schedule = new TenantSchedule();
        schedule.setTenantId(request.tenantId());
        schedule.setScheduleId(request.scheduleId());
        schedule.setJobType(request.jobType());
        schedule.setCronExpression(request.cronExpression());
        schedule.setJobParams(request.jobParams());
        schedule.setCreatedBy(request.createdBy());
        schedule.setUpdatedBy(request.createdBy());

        TenantSchedule created = scheduleService.create(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScheduleResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "List schedules", description = "List all batch schedules for a tenant")
    public ResponseEntity<List<ScheduleResponse>> listSchedules(
            @RequestParam String tenantId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        log.debug("Listing schedules for tenant: {}, activeOnly: {}", tenantId, activeOnly);

        List<TenantSchedule> schedules;
        if (Boolean.TRUE.equals(activeOnly)) {
            schedules = scheduleService.listActiveByTenant(tenantId);
        } else {
            schedules = scheduleService.listByTenant(tenantId);
        }

        List<ScheduleResponse> response = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule", description = "Get a batch schedule by ID")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable UUID id) {
        log.debug("Fetching schedule: {}", id);
        TenantSchedule schedule = scheduleService.getById(id);
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update schedule", description = "Update an existing batch schedule")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleRequest request
    ) {
        log.info("Updating schedule: {}", id);

        TenantSchedule updates = new TenantSchedule();
        updates.setJobType(request.jobType());
        updates.setCronExpression(request.cronExpression());
        updates.setJobParams(request.jobParams());
        updates.setUpdatedBy(request.createdBy());

        TenantSchedule updated = scheduleService.update(id, updates);
        return ResponseEntity.ok(ScheduleResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete schedule", description = "Delete a batch schedule and its Temporal handle")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        log.info("Deleting schedule: {}", id);
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Trigger schedule", description = "Trigger an immediate ad-hoc execution")
    public ResponseEntity<Map<String, String>> triggerSchedule(
            @PathVariable UUID id,
            @RequestBody(required = false) TriggerRequest request
    ) {
        log.info("Triggering schedule: {}", id);
        Map<String, Object> overrideParams = request != null ? request.overrideParams() : Map.of();
        scheduleService.trigger(id, overrideParams);
        return ResponseEntity.ok(Map.of("status", "triggered", "scheduleId", id.toString()));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause schedule", description = "Pause a running schedule")
    public ResponseEntity<ScheduleResponse> pauseSchedule(@PathVariable UUID id) {
        log.info("Pausing schedule: {}", id);
        TenantSchedule paused = scheduleService.pause(id);
        return ResponseEntity.ok(ScheduleResponse.from(paused));
    }

    @PostMapping("/{id}/unpause")
    @Operation(summary = "Unpause schedule", description = "Unpause a paused schedule")
    public ResponseEntity<ScheduleResponse> unpauseSchedule(@PathVariable UUID id) {
        log.info("Unpausing schedule: {}", id);
        TenantSchedule unpaused = scheduleService.unpause(id);
        return ResponseEntity.ok(ScheduleResponse.from(unpaused));
    }
}
