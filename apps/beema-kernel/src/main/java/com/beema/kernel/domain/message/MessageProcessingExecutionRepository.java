package com.beema.kernel.domain.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for MessageProcessingExecution entities
 */
@Repository
public interface MessageProcessingExecutionRepository extends JpaRepository<MessageProcessingExecution, Long> {

    /**
     * Find executions by hook ID
     */
    List<MessageProcessingExecution> findByHookIdOrderByStartedAtDesc(Long hookId);

    /**
     * Find failed executions
     */
    List<MessageProcessingExecution> findByStatusOrderByStartedAtDesc(String status);

    /**
     * Find executions within time range
     */
    @Query("SELECT e FROM MessageProcessingExecution e WHERE e.startedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY e.startedAt DESC")
    List<MessageProcessingExecution> findByTimeRange(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find recent executions for a message type
     */
    @Query("SELECT e FROM MessageProcessingExecution e WHERE e.messageType = :messageType " +
           "ORDER BY e.startedAt DESC LIMIT 100")
    List<MessageProcessingExecution> findRecentByMessageType(@Param("messageType") String messageType);

    /**
     * Get execution statistics
     */
    @Query("SELECT e.status, COUNT(e) FROM MessageProcessingExecution e " +
           "WHERE e.hookId = :hookId GROUP BY e.status")
    List<Object[]> getExecutionStatsByHookId(@Param("hookId") Long hookId);
}
