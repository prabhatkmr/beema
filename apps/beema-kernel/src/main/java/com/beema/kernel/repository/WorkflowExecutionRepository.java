package com.beema.kernel.repository;

import com.beema.kernel.domain.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowExecution entities
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    /**
     * Find execution by workflow ID and run ID
     */
    Optional<WorkflowExecution> findByWorkflowIdAndRunId(String workflowId, String runId);

    /**
     * Find all executions for a specific agreement
     */
    List<WorkflowExecution> findByAgreementIdOrderByStartedAtDesc(Long agreementId);

    /**
     * Find executions by event type
     */
    List<WorkflowExecution> findByEventTypeOrderByStartedAtDesc(String eventType);

    /**
     * Find executions by status
     */
    List<WorkflowExecution> findByStatusOrderByStartedAtDesc(String status);

    /**
     * Find recent executions
     */
    @Query("SELECT we FROM WorkflowExecution we " +
           "ORDER BY we.startedAt DESC")
    List<WorkflowExecution> findRecentExecutions();
}
