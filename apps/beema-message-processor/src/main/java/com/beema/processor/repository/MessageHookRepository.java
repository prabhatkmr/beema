package com.beema.processor.repository;

import com.beema.processor.model.MessageHook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing sys_message_hooks table.
 * Serializable for Flink distribution.
 */
public class MessageHookRepository implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MessageHookRepository.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final ObjectMapper objectMapper;

    public MessageHookRepository(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Finds the best matching hook for a message type and source system.
     * Returns the hook with the highest priority (lowest priority value).
     *
     * @param messageType Message type (e.g., "policy_created")
     * @param sourceSystem Source system (e.g., "legacy_system")
     * @return Optional MessageHook if found
     */
    public Optional<MessageHook> findHookForMessage(String messageType, String sourceSystem) {
        String sql = "SELECT hook_id, hook_name, message_type, source_system, jexl_transform, " +
                "field_mapping, enabled, priority, description, created_at, updated_at, " +
                "created_by, updated_by " +
                "FROM sys_message_hooks " +
                "WHERE message_type = ? AND source_system = ? AND enabled = true " +
                "ORDER BY priority ASC " +
                "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageType);
            stmt.setString(2, sourceSystem);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                MessageHook hook = mapResultSetToHook(rs);
                log.debug("Found hook '{}' for messageType='{}', sourceSystem='{}'",
                        hook.getHookName(), messageType, sourceSystem);
                return Optional.of(hook);
            }

            log.debug("No hook found for messageType='{}', sourceSystem='{}'", messageType, sourceSystem);
            return Optional.empty();

        } catch (SQLException e) {
            log.error("Database error finding hook for messageType='{}', sourceSystem='{}': {}",
                    messageType, sourceSystem, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Finds all enabled hooks for a message type and source system.
     *
     * @param messageType Message type
     * @param sourceSystem Source system
     * @return List of matching hooks
     */
    public List<MessageHook> findAllHooksForMessage(String messageType, String sourceSystem) {
        String sql = "SELECT hook_id, hook_name, message_type, source_system, jexl_transform, " +
                "field_mapping, enabled, priority, description, created_at, updated_at, " +
                "created_by, updated_by " +
                "FROM sys_message_hooks " +
                "WHERE message_type = ? AND source_system = ? AND enabled = true " +
                "ORDER BY priority ASC";

        List<MessageHook> hooks = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageType);
            stmt.setString(2, sourceSystem);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                hooks.add(mapResultSetToHook(rs));
            }

            log.debug("Found {} hooks for messageType='{}', sourceSystem='{}'",
                    hooks.size(), messageType, sourceSystem);
            return hooks;

        } catch (SQLException e) {
            log.error("Database error finding hooks: {}", e.getMessage());
            return hooks;
        }
    }

    /**
     * Maps ResultSet row to MessageHook object.
     */
    private MessageHook mapResultSetToHook(ResultSet rs) throws SQLException {
        MessageHook hook = new MessageHook();
        hook.setHookId(rs.getLong("hook_id"));
        hook.setHookName(rs.getString("hook_name"));
        hook.setMessageType(rs.getString("message_type"));
        hook.setSourceSystem(rs.getString("source_system"));
        hook.setJexlTransform(rs.getString("jexl_transform"));

        // Parse JSONB field_mapping
        String fieldMappingJson = rs.getString("field_mapping");
        try {
            JsonNode fieldMapping = objectMapper.readTree(fieldMappingJson);
            hook.setFieldMapping(fieldMapping);
        } catch (Exception e) {
            log.error("Failed to parse field_mapping JSON for hook {}: {}",
                    rs.getLong("hook_id"), e.getMessage());
        }

        hook.setEnabled(rs.getBoolean("enabled"));
        hook.setPriority(rs.getInt("priority"));
        hook.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            hook.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            hook.setUpdatedAt(updatedAt.toInstant());
        }

        hook.setCreatedBy(rs.getString("created_by"));
        hook.setUpdatedBy(rs.getString("updated_by"));

        return hook;
    }

    /**
     * Gets a database connection.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
