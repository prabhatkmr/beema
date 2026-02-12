package com.beema.metadata.repository;

import com.beema.metadata.model.MessageHook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MessageHook entities.
 */
@Repository
public interface MessageHookRepository extends JpaRepository<MessageHook, UUID> {

    /**
     * Find a message hook by hook name.
     *
     * @param hookName Hook name
     * @return Optional MessageHook
     */
    Optional<MessageHook> findByHookName(String hookName);

    /**
     * Find all hooks for a specific message type.
     *
     * @param messageType Message type
     * @return List of MessageHooks
     */
    List<MessageHook> findByMessageType(String messageType);

    /**
     * Find all enabled hooks.
     *
     * @param enabled Enabled flag
     * @return List of MessageHooks
     */
    List<MessageHook> findByEnabled(Boolean enabled);

    /**
     * Find enabled hooks for a specific message type, ordered by priority.
     *
     * @param messageType Message type
     * @param enabled Enabled flag
     * @return List of MessageHooks ordered by priority
     */
    List<MessageHook> findByMessageTypeAndEnabledOrderByPriorityAsc(String messageType, Boolean enabled);

    /**
     * Check if a hook name already exists.
     *
     * @param hookName Hook name
     * @return true if exists, false otherwise
     */
    boolean existsByHookName(String hookName);
}
