package com.beema.kernel.repository.schedule;

import com.beema.kernel.domain.schedule.TenantSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TenantSchedule entity.
 */
@Repository
public interface TenantScheduleRepository extends JpaRepository<TenantSchedule, UUID> {

    List<TenantSchedule> findByTenantId(String tenantId);

    List<TenantSchedule> findByTenantIdAndIsActive(String tenantId, Boolean isActive);

    Optional<TenantSchedule> findByTenantIdAndScheduleId(String tenantId, String scheduleId);

    List<TenantSchedule> findByJobType(String jobType);

    List<TenantSchedule> findByTenantIdAndJobType(String tenantId, String jobType);

    boolean existsByTenantIdAndScheduleId(String tenantId, String scheduleId);
}
