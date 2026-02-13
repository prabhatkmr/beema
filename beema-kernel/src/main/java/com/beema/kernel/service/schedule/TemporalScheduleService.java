package com.beema.kernel.service.schedule;

import com.beema.kernel.domain.schedule.TenantSchedule;

import java.util.Map;

/**
 * Interface for Temporal.io schedule integration.
 *
 * Task #8 will provide the real implementation. This placeholder
 * allows the schedule metadata layer and REST API to compile and
 * work independently of the Temporal runtime.
 */
public interface TemporalScheduleService {

    /**
     * Create a Temporal schedule for a tenant schedule entity.
     *
     * @param schedule the tenant schedule metadata
     * @return the Temporal schedule handle ID
     */
    String createSchedule(TenantSchedule schedule);

    /**
     * Pause a running Temporal schedule.
     *
     * @param temporalScheduleId the Temporal schedule handle ID
     */
    void pauseSchedule(String temporalScheduleId);

    /**
     * Unpause a paused Temporal schedule.
     *
     * @param temporalScheduleId the Temporal schedule handle ID
     */
    void unpauseSchedule(String temporalScheduleId);

    /**
     * Delete a Temporal schedule.
     *
     * @param temporalScheduleId the Temporal schedule handle ID
     */
    void deleteSchedule(String temporalScheduleId);

    /**
     * Trigger an immediate execution of a schedule.
     *
     * @param temporalScheduleId the Temporal schedule handle ID
     * @param overrideParams optional parameter overrides for this execution
     */
    void triggerSchedule(String temporalScheduleId, Map<String, Object> overrideParams);
}
