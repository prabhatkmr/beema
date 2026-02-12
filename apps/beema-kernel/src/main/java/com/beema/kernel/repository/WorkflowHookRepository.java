package com.beema.kernel.repository;

import com.beema.kernel.domain.WorkflowHook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for WorkflowHook entities
 */
@Repository
public interface WorkflowHookRepository extends JpaRepository<WorkflowHook, Long> {

    /**
     * Find all enabled hooks for a specific event type, ordered by execution order
     */
    @Query("SELECT wh FROM WorkflowHook wh " +
           "WHERE wh.eventType = :eventType " +
           "AND wh.enabled = true " +
           "ORDER BY wh.executionOrder ASC")
    List<WorkflowHook> findEnabledHooksByEventType(@Param("eventType") String eventType);

    /**
     * Find hook by name
     */
    WorkflowHook findByHookName(String hookName);

    /**
     * Find all enabled hooks ordered by execution order
     */
    @Query("SELECT wh FROM WorkflowHook wh " +
           "WHERE wh.enabled = true " +
           "ORDER BY wh.executionOrder ASC")
    List<WorkflowHook> findAllEnabledHooks();
}
