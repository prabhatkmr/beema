package com.beema.kernel.service.schedule;

import com.beema.kernel.domain.schedule.TenantSchedule;
import com.beema.kernel.repository.schedule.TenantScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing tenant batch schedules.
 *
 * Provides CRUD operations, cron validation, and
 * delegates to TemporalScheduleService for runtime orchestration.
 */
@Service
@Transactional
public class TenantScheduleService {

    private static final Logger log = LoggerFactory.getLogger(TenantScheduleService.class);

    private final TenantScheduleRepository repository;
    private final TemporalScheduleService temporalScheduleService;

    public TenantScheduleService(
            TenantScheduleRepository repository,
            TemporalScheduleService temporalScheduleService) {
        this.repository = repository;
        this.temporalScheduleService = temporalScheduleService;
    }

    /**
     * Create a new tenant schedule.
     */
    public TenantSchedule create(TenantSchedule schedule) {
        log.info("Creating schedule '{}' for tenant '{}'", schedule.getScheduleId(), schedule.getTenantId());

        validateCronExpression(schedule.getCronExpression());

        if (repository.existsByTenantIdAndScheduleId(schedule.getTenantId(), schedule.getScheduleId())) {
            throw new IllegalArgumentException(
                    "Schedule '" + schedule.getScheduleId() + "' already exists for tenant '" + schedule.getTenantId() + "'");
        }

        // Register with Temporal
        String temporalId = temporalScheduleService.createSchedule(schedule);
        schedule.setTemporalScheduleId(temporalId);

        TenantSchedule saved = repository.save(schedule);
        log.info("Created schedule with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Get schedule by ID.
     */
    @Transactional(readOnly = true)
    public TenantSchedule getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
    }

    /**
     * List all schedules for a tenant.
     */
    @Transactional(readOnly = true)
    public List<TenantSchedule> listByTenant(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    /**
     * List active schedules for a tenant.
     */
    @Transactional(readOnly = true)
    public List<TenantSchedule> listActiveByTenant(String tenantId) {
        return repository.findByTenantIdAndIsActive(tenantId, true);
    }

    /**
     * Update an existing schedule.
     */
    public TenantSchedule update(UUID id, TenantSchedule updates) {
        TenantSchedule existing = getById(id);
        log.info("Updating schedule: {}", id);

        if (updates.getCronExpression() != null) {
            validateCronExpression(updates.getCronExpression());
            existing.setCronExpression(updates.getCronExpression());
        }
        if (updates.getJobType() != null) {
            existing.setJobType(updates.getJobType());
        }
        if (updates.getJobParams() != null) {
            existing.setJobParams(updates.getJobParams());
        }
        if (updates.getUpdatedBy() != null) {
            existing.setUpdatedBy(updates.getUpdatedBy());
        }

        return repository.save(existing);
    }

    /**
     * Delete a schedule.
     */
    public void delete(UUID id) {
        TenantSchedule schedule = getById(id);
        log.info("Deleting schedule: {} ({})", id, schedule.getScheduleId());

        if (schedule.getTemporalScheduleId() != null) {
            temporalScheduleService.deleteSchedule(schedule.getTemporalScheduleId());
        }

        repository.delete(schedule);
    }

    /**
     * Trigger an immediate execution of a schedule.
     */
    public void trigger(UUID id, Map<String, Object> overrideParams) {
        TenantSchedule schedule = getById(id);
        log.info("Triggering schedule: {} ({})", id, schedule.getScheduleId());

        if (schedule.getTemporalScheduleId() == null) {
            throw new IllegalStateException("Schedule has no Temporal handle: " + id);
        }

        temporalScheduleService.triggerSchedule(schedule.getTemporalScheduleId(), overrideParams);
    }

    /**
     * Pause a schedule.
     */
    public TenantSchedule pause(UUID id) {
        TenantSchedule schedule = getById(id);
        log.info("Pausing schedule: {} ({})", id, schedule.getScheduleId());

        if (schedule.getTemporalScheduleId() != null) {
            temporalScheduleService.pauseSchedule(schedule.getTemporalScheduleId());
        }

        schedule.setIsActive(false);
        return repository.save(schedule);
    }

    /**
     * Unpause a schedule.
     */
    public TenantSchedule unpause(UUID id) {
        TenantSchedule schedule = getById(id);
        log.info("Unpausing schedule: {} ({})", id, schedule.getScheduleId());

        if (schedule.getTemporalScheduleId() != null) {
            temporalScheduleService.unpauseSchedule(schedule.getTemporalScheduleId());
        }

        schedule.setIsActive(true);
        return repository.save(schedule);
    }

    /**
     * Validate a cron expression using Spring's CronExpression parser.
     */
    private void validateCronExpression(String cron) {
        try {
            CronExpression.parse(cron);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression '" + cron + "': " + e.getMessage());
        }
    }
}
