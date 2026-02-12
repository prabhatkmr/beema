package com.beema.kernel.repository.user;

import com.beema.kernel.domain.user.AvailabilityStatus;
import com.beema.kernel.domain.user.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, UUID> {

    Optional<SysUser> findByEmail(String email);

    List<SysUser> findByTenantId(String tenantId);

    /**
     * Find users with specific availability status.
     */
    List<SysUser> findByAvailabilityStatus(AvailabilityStatus status);

    /**
     * Find available users (AVAILABLE status and current_tasks < max_tasks).
     */
    @Query("SELECT u FROM SysUser u WHERE u.availabilityStatus = 'AVAILABLE' " +
           "AND u.currentTasks < u.maxTasks AND u.isActive = true")
    List<SysUser> findAvailableUsers();

    /**
     * Find users by skill using JSONB containment.
     */
    @Query(value = "SELECT * FROM sys_users WHERE skill_tags @> CAST(:skill AS jsonb)", nativeQuery = true)
    List<SysUser> findBySkill(@Param("skill") String skill);

    /**
     * Find users with capacity (not at WIP limit).
     */
    @Query("SELECT u FROM SysUser u WHERE u.currentTasks < u.maxTasks AND u.isActive = true")
    List<SysUser> findUsersWithCapacity();
}
