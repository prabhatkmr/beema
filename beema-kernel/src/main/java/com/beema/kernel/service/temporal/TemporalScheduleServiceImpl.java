package com.beema.kernel.service.temporal;

import com.beema.kernel.config.TemporalProperties;
import com.beema.kernel.domain.schedule.TenantSchedule;
import com.beema.kernel.service.schedule.TemporalScheduleService;
import com.beema.kernel.workflow.UniversalBatchWorkflow;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.SchedulePolicy;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.schedules.ScheduleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Real Temporal SDK implementation of TemporalScheduleService.
 *
 * Manages Temporal schedules that trigger UniversalBatchWorkflow
 * on configured cron expressions per tenant.
 *
 * Active only when temporal.enabled=true.
 * When inactive, the NoOpTemporalScheduleService provides the fallback.
 */
@Service("temporalScheduleServiceImpl")
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true")
public class TemporalScheduleServiceImpl implements TemporalScheduleService {

    private static final Logger log = LoggerFactory.getLogger(TemporalScheduleServiceImpl.class);

    private final ScheduleClient scheduleClient;
    private final TemporalProperties temporalProperties;

    public TemporalScheduleServiceImpl(ScheduleClient scheduleClient,
                                        TemporalProperties temporalProperties) {
        this.scheduleClient = scheduleClient;
        this.temporalProperties = temporalProperties;
    }

    @Override
    public String createSchedule(TenantSchedule schedule) {
        String scheduleId = buildTemporalScheduleId(schedule);
        log.info("Creating Temporal schedule: id={}, tenant={}, cron={}",
                scheduleId, schedule.getTenantId(), schedule.getCronExpression());

        try {
            Schedule temporalSchedule = buildSchedule(schedule);

            ScheduleOptions options = ScheduleOptions.newBuilder()
                    .setTriggerImmediately(false)
                    .build();

            scheduleClient.createSchedule(scheduleId, temporalSchedule, options);
            log.info("Created Temporal schedule: {}", scheduleId);
            return scheduleId;

        } catch (Exception e) {
            log.error("Failed to create Temporal schedule: {}", scheduleId, e);
            throw new RuntimeException("Failed to create Temporal schedule: " + e.getMessage(), e);
        }
    }

    @Override
    public void pauseSchedule(String temporalScheduleId) {
        log.info("Pausing Temporal schedule: {}", temporalScheduleId);

        try {
            ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
            handle.pause("Paused via API");
            log.info("Paused Temporal schedule: {}", temporalScheduleId);

        } catch (Exception e) {
            log.error("Failed to pause Temporal schedule: {}", temporalScheduleId, e);
            throw new RuntimeException("Failed to pause Temporal schedule: " + e.getMessage(), e);
        }
    }

    @Override
    public void unpauseSchedule(String temporalScheduleId) {
        log.info("Unpausing Temporal schedule: {}", temporalScheduleId);

        try {
            ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
            handle.unpause("Resumed via API");
            log.info("Unpaused Temporal schedule: {}", temporalScheduleId);

        } catch (Exception e) {
            log.error("Failed to unpause Temporal schedule: {}", temporalScheduleId, e);
            throw new RuntimeException("Failed to unpause Temporal schedule: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSchedule(String temporalScheduleId) {
        log.info("Deleting Temporal schedule: {}", temporalScheduleId);

        try {
            ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
            handle.delete();
            log.info("Deleted Temporal schedule: {}", temporalScheduleId);

        } catch (Exception e) {
            log.error("Failed to delete Temporal schedule: {}", temporalScheduleId, e);
            throw new RuntimeException("Failed to delete Temporal schedule: " + e.getMessage(), e);
        }
    }

    @Override
    public void triggerSchedule(String temporalScheduleId, Map<String, Object> overrideParams) {
        log.info("Triggering Temporal schedule: {}", temporalScheduleId);

        try {
            ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
            handle.trigger();
            log.info("Triggered Temporal schedule: {}", temporalScheduleId);

        } catch (Exception e) {
            log.error("Failed to trigger Temporal schedule: {}", temporalScheduleId, e);
            throw new RuntimeException("Failed to trigger Temporal schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a Temporal schedule exists.
     */
    public boolean scheduleExists(String temporalScheduleId) {
        try {
            ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
            handle.describe();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build Temporal schedule ID from TenantSchedule entity.
     */
    String buildTemporalScheduleId(TenantSchedule schedule) {
        return String.format("beema-%s-%s", schedule.getTenantId(), schedule.getScheduleId());
    }

    /**
     * Build a Temporal Schedule from TenantSchedule entity.
     */
    private Schedule buildSchedule(TenantSchedule data) {
        ScheduleSpec spec = ScheduleSpec.newBuilder()
                .setCronExpressions(List.of(data.getCronExpression()))
                .build();

        Map<String, Object> jobParams = data.getJobParams() != null
                ? data.getJobParams() : Collections.emptyMap();

        ScheduleActionStartWorkflow action = ScheduleActionStartWorkflow.newBuilder()
                .setWorkflowType(UniversalBatchWorkflow.class)
                .setArguments(data.getTenantId(), data.getJobType(), jobParams)
                .setOptions(WorkflowOptions.newBuilder()
                        .setWorkflowId(buildTemporalScheduleId(data) + "-run")
                        .setTaskQueue(temporalProperties.getTaskQueue())
                        .build())
                .build();

        boolean isActive = data.getIsActive() != null && data.getIsActive();
        ScheduleState state = ScheduleState.newBuilder()
                .setPaused(!isActive)
                .setNote(isActive ? "Active schedule" : "Paused schedule")
                .build();

        SchedulePolicy policy = SchedulePolicy.newBuilder()
                .build();

        return Schedule.newBuilder()
                .setAction(action)
                .setSpec(spec)
                .setState(state)
                .setPolicy(policy)
                .build();
    }
}
