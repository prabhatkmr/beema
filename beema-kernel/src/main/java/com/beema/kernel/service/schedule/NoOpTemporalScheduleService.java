package com.beema.kernel.service.schedule;

import com.beema.kernel.domain.schedule.TenantSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * No-op implementation of TemporalScheduleService.
 *
 * Used when no real Temporal integration is configured (e.g., dev/test).
 * Task #8 will provide the real implementation which will supersede this
 * via @ConditionalOnMissingBean.
 */
@Service
@ConditionalOnMissingBean(name = "temporalScheduleServiceImpl")
public class NoOpTemporalScheduleService implements TemporalScheduleService {

    private static final Logger log = LoggerFactory.getLogger(NoOpTemporalScheduleService.class);

    @Override
    public String createSchedule(TenantSchedule schedule) {
        String fakeId = "noop-" + UUID.randomUUID();
        log.warn("NoOp: createSchedule called for {} - returning fake ID: {}", schedule.getScheduleId(), fakeId);
        return fakeId;
    }

    @Override
    public void pauseSchedule(String temporalScheduleId) {
        log.warn("NoOp: pauseSchedule called for {}", temporalScheduleId);
    }

    @Override
    public void unpauseSchedule(String temporalScheduleId) {
        log.warn("NoOp: unpauseSchedule called for {}", temporalScheduleId);
    }

    @Override
    public void deleteSchedule(String temporalScheduleId) {
        log.warn("NoOp: deleteSchedule called for {}", temporalScheduleId);
    }

    @Override
    public void triggerSchedule(String temporalScheduleId, Map<String, Object> overrideParams) {
        log.warn("NoOp: triggerSchedule called for {} with params: {}", temporalScheduleId, overrideParams);
    }
}
