package com.beema.kernel.service.temporal;

import com.beema.kernel.domain.schedule.TenantSchedule;
import com.beema.kernel.repository.schedule.TenantScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Startup synchronization service that ensures all active database schedules
 * have corresponding Temporal schedules.
 *
 * On application startup (ApplicationReadyEvent), this service:
 * 1. Reads all active schedules from the database via TenantScheduleRepository
 * 2. Checks if each schedule exists in Temporal
 * 3. Recreates missing schedules in Temporal
 *
 * This provides a self-healing mechanism: if Temporal server is restarted
 * or schedules are lost, they are automatically recreated from the database
 * as the source of truth.
 */
@Service
@ConditionalOnBean(TemporalScheduleServiceImpl.class)
public class TemporalScheduleSyncService {

    private static final Logger log = LoggerFactory.getLogger(TemporalScheduleSyncService.class);

    private final TemporalScheduleServiceImpl temporalScheduleService;
    private final TenantScheduleRepository scheduleRepository;

    public TemporalScheduleSyncService(TemporalScheduleServiceImpl temporalScheduleService,
                                        TenantScheduleRepository scheduleRepository) {
        this.temporalScheduleService = temporalScheduleService;
        this.scheduleRepository = scheduleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncSchedulesOnStartup() {
        log.info("Starting Temporal schedule sync on application startup");

        try {
            // Query all tenants' active schedules - no tenant filter needed for sync
            List<TenantSchedule> activeSchedules = scheduleRepository.findAll().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .toList();

            log.info("Found {} active schedules in database", activeSchedules.size());

            int synced = 0;
            int skipped = 0;
            int failed = 0;

            for (TenantSchedule schedule : activeSchedules) {
                String scheduleId = temporalScheduleService.buildTemporalScheduleId(schedule);
                try {
                    if (temporalScheduleService.scheduleExists(scheduleId)) {
                        log.debug("Schedule already exists in Temporal: {}", scheduleId);
                        skipped++;
                    } else {
                        log.info("Recreating missing Temporal schedule: {}", scheduleId);
                        String temporalId = temporalScheduleService.createSchedule(schedule);
                        // Update the entity with the Temporal schedule ID if missing
                        if (schedule.getTemporalScheduleId() == null) {
                            schedule.setTemporalScheduleId(temporalId);
                            scheduleRepository.save(schedule);
                        }
                        synced++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync schedule: {}", scheduleId, e);
                    failed++;
                }
            }

            log.info("Temporal schedule sync complete: synced={}, skipped={}, failed={}",
                    synced, skipped, failed);

        } catch (Exception e) {
            log.error("Failed to sync schedules on startup. Schedules may be out of sync.", e);
        }
    }

    /**
     * Manual re-sync trigger (can be called from an admin endpoint).
     */
    public void resync() {
        log.info("Manual Temporal schedule re-sync triggered");
        syncSchedulesOnStartup();
    }
}
