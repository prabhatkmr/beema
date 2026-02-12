package com.beema.kernel.domain.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MessageHook entities
 */
@Repository
public interface MessageHookRepository extends JpaRepository<MessageHook, Long> {

    /**
     * Find all hooks for a specific message type and source system
     */
    @Query("SELECT h FROM MessageHook h WHERE h.messageType = :messageType " +
           "AND h.sourceSystem = :sourceSystem AND h.enabled = true " +
           "ORDER BY h.preprocessingOrder, h.transformationOrder, h.postprocessingOrder")
    List<MessageHook> findByMessageTypeAndSourceSystemAndEnabledTrue(
            @Param("messageType") String messageType,
            @Param("sourceSystem") String sourceSystem);

    /**
     * Find hook by name
     */
    Optional<MessageHook> findByHookName(String hookName);

    /**
     * Find all enabled hooks for a message type
     */
    List<MessageHook> findByMessageTypeAndEnabledTrueOrderByPreprocessingOrder(String messageType);

    /**
     * Find all hooks by source system
     */
    List<MessageHook> findBySourceSystemAndEnabledTrue(String sourceSystem);

    /**
     * Check if hook name exists
     */
    boolean existsByHookName(String hookName);
}
